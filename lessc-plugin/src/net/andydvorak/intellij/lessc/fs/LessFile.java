/*
 * Copyright 2012 Andrew C. Dvorak.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.andydvorak.intellij.lessc.fs;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LessFile extends File implements Comparable<File> {

    private static final Pattern LESS_IMPORT_PATTERN = Pattern.compile("@import \"([^\"]+)\";");
    private static final Pattern LESS_FILENAME_PATTERN = Pattern.compile(".*\\.less$");
    private static final Logger LOG = Logger.getInstance("#" + LessFile.class.getName());

    private final AtomicBoolean cssChanged = new AtomicBoolean();

    public LessFile(final String path) {
        this(new File(path));
    }

    public LessFile(final String parent, final String child) {
        this(new File(parent, child));
    }

    public LessFile(final File parent, final String child) {
        this(new File(parent, child));
    }

    public LessFile(final URI uri) {
        this(new File(uri));
    }

    public LessFile(final File file) {
        super(getCanonicalPathSafe(file));
    }

    /*
     * Public instance methods
     */

    public boolean hasCssChanged() {
        return cssChanged.get();
    }

    /**
     * Similar to {@link #getCanonicalPath()}, but falls back to returning {@link #getAbsolutePath()} instead of throwing an {@link IOException}.
     * @return the canonical path to the {@code File} if it exists; otherwise the absolute path
     */
    public String getCanonicalPathSafe() {
        return getCanonicalPathSafe(this);
    }

    /**
     * Similar to {@link #getCanonicalPath()}, but falls back to returning {@link #getAbsolutePath()} instead of throwing an {@link IOException}.
     * Any HTML Entities in the resulting path will be escaped (encoded) so that the path is safe to insert into HTML code.
     * @return the canonical, HTML-encoded path to the {@code File} if it exists; otherwise the absolute path
     */
    public String getCanonicalPathSafeHtmlEscaped() {
        return StringEscapeUtils.escapeHtml4(getCanonicalPathSafe());
    }

    @Nullable
    public LessProfile getLessProfile(final Collection<LessProfile> lessProfiles) {
        for (LessProfile lessProfile : lessProfiles) {
            final File lessProfileDir = new File(lessProfile.getLessDirPath());
            if (FileUtil.isAncestor(lessProfileDir, this, false)) {
                return lessProfile;
            }
        }
        return null;
    }

    public boolean shouldCompile(@Nullable final LessProfile lessProfile) {
        if (lessProfile == null || !lessProfile.hasCssDirectories())
            return false;

        final List<String> includePatterns = normalizePatterns(lessProfile.getIncludePattern());
        final List<String> excludePatterns = normalizePatterns(lessProfile.getExcludePattern());

        boolean include = includePatterns.isEmpty();
        boolean exclude = false;

        for (String includePattern : includePatterns) {
            include |= matches(includePattern);
        }

        for (String excludePattern : excludePatterns) {
            exclude |= matches(excludePattern);
        }

        return include && !exclude;
    }

    public boolean matches(final String pattern) {
        return FilenameUtils.wildcardMatchOnSystem(getCanonicalPathSafe(), makePatternAbsolute(pattern));
    }

    public boolean equals(@Nullable final LessFile that) {
        return isSameFile(this, that);
    }

    public boolean notEquals(@Nullable final LessFile that) {
        return !this.equals(that);
    }

    /**
     * Returns all files that directly {@code @import} the current LESS file.
     * @param lessProfile profile to search for dependents
     * @return all files that directly {@code @import} the current LESS file.
     * @throws IOException
     */
    @NotNull
    public Set<LessFile> getFirstLevelDependents(@NotNull final LessProfile lessProfile) throws IOException {
        final LessFile parentLessFile = this;
        return getLessFiles(lessProfile, new Filter() {
            @Override
            public boolean accept(@NotNull LessFile lessFile) throws IOException {
                return  lessFile.notEquals(parentLessFile) &&
//                        lessFile.shouldCompile(lessProfile) && // this prevents us from finding compilable dependents in uncompilable files
                        lessFile.imports(parentLessFile);
            }
        });
    }

    /**
     * Returns all compilable dependents of the current LESS file found in the given profile (recursive).
     * If a <em>non-compilable</em> file directly {@code @import}s the current LESS file, and a separate
     * <em>compilable</em> file {@code @import}s the non-compilable file, the compilable file will be included
     * in the returned set but the non-compilable file will not.
     * @param lessProfile profile to search for dependents
     * @return all compilable dependent LESS files
     * @throws IOException
     */
    @NotNull
    public Set<LessFile> getDependentsRecursive(@NotNull final LessProfile lessProfile) throws IOException {
        final Set<LessFile> dependents = getDependentsRecursive(lessProfile, new LinkedHashSet<LessFile>());
        LOG.debug("Dependency tree for " + getName() + " contains " + dependents.size() + " files");
        final Set<LessFile> compilable = filter(dependents, new Filter() {
            @Override
            public boolean accept(@NotNull LessFile lessFile) throws IOException {
                return lessFile.shouldCompile(lessProfile);
            }
        });
        LOG.debug("Found for " + getName() + " contains " + dependents.size() + " files");
        return compilable;
    }

    /**
     * Determines if the current LESS file contains an {@code @import} that resolves to the given LESS file.
     * @param importedLessFile LESS file to search for in the current file's {@code @import}s
     * @return {@code true} if the current LESS file contains an {@code @import} that resolves to the given LESS file;
     *         otherwise {@code false}
     * @throws IOException
     */
    public boolean imports(@NotNull final LessFile importedLessFile) throws IOException {
        final Set<LessFile> matchingImports = getImports(new Filter() {
            @Override
            public boolean accept(@NotNull LessFile lessFile) {
                return importedLessFile.equals(lessFile);
            }
        });
        return !matchingImports.isEmpty();
    }

    @NotNull
    public Set<LessFile> getImports(@NotNull Filter filter) throws IOException {
        final Set<LessFile> imports = new LinkedHashSet<LessFile>();
        final Matcher importMatcher = LESS_IMPORT_PATTERN.matcher(FileUtil.loadFile(this));
        while (importMatcher.find()) {
            final LessFile lessFile = new LessFile(getParent(), resolveImportFileName(importMatcher.group(1)));
            if (filter.accept(lessFile)) {
                imports.add(lessFile);
            }
        }
        return imports;
    }

    public void compile(@NotNull final LessEngine engine, final LessProfile lessProfile) throws IOException, LessException {
        cssChanged.set(false);

        final File cssTempFile = createCssTempFile();

        LOG.info("Compiling " + getName() + ":\n" +
                 "\t" + "lessPath: " + getCanonicalPath() + "\n" +
                 "\t" + "cssTempPath: " + cssTempFile.getCanonicalPath());

        final String inputLessCode = FileUtil.loadFile(this, "UTF-8");
        final boolean compressOutput = (lessProfile.isCompressOutput() && !inputLessCode.contains("//simpless:!minify")) || inputLessCode.contains("//simpless:minify");
        final String compiled = engine.compile(inputLessCode, this.toURI().toURL().toString(), compressOutput);

        FileUtil.writeToFile(cssTempFile, compiled);
        updateCssFiles(cssTempFile, lessProfile);
    }

    /*
     * Private instance methods
     */

    private Set<LessFile> getDependentsRecursive(@NotNull final LessProfile lessProfile,
                                                 @NotNull final Set<LessFile> curDependents) throws IOException {
        final Set<LessFile> newDependents = new LinkedHashSet<LessFile>();
        final Set<LessFile> sourceFileDependents = getFirstLevelDependents(lessProfile);
        final Set<LessFile> sourceFileDependentsFiltered = filter(sourceFileDependents, new Filter() {
            @Override
            public boolean accept(@NotNull LessFile lessFile) throws IOException {
                return !curDependents.contains(lessFile) && !newDependents.contains(lessFile);
            }
        });

        for (LessFile dependentLessFile : sourceFileDependentsFiltered) {
            newDependents.add(dependentLessFile);

            final Set<LessFile> allDependents = mergeSets(curDependents, newDependents);
            final Set<LessFile> recursiveDependents = dependentLessFile.getDependentsRecursive(lessProfile, allDependents);

            newDependents.addAll(recursiveDependents);
        }

        return newDependents;
    }

    /**
     * Copies the contents of the temp file to its corresponding CSS file(s) in every output directory specified in the profile.
     * @throws IOException
     */
    private void updateCssFiles(@NotNull final File cssTempFile, @NotNull final LessProfile lessProfile) throws IOException {
        if (cssTempFile.length() == 0) {
            FileUtil.delete(cssTempFile);
            return;
        }

        final File lessProfileDir = lessProfile.getLessDir();

        final String relativeLessPath = StringUtils.defaultString(FileUtil.getRelativePath(lessProfileDir, this));
        final String relativeCssPath = relativeLessPath.replaceFirst("\\.less$", ".css");

        final String cssTempFileContent = FileUtil.loadFile(cssTempFile);

        int numUpdated = 0;

        for (CssDirectory cssDirectory : lessProfile.getCssDirectories()) {
            final File cssDestFile = new File(cssDirectory.getPath(), relativeCssPath);

            // CSS file hasn't changed, so don't bother updating
            if (cssDestFile.exists() && FileUtil.loadFile(cssDestFile).equals(cssTempFileContent))
                continue;

            numUpdated++;

            FileUtil.createIfDoesntExist(cssDestFile);
            FileUtil.copy(cssTempFile, cssDestFile);

            refreshCssFile(cssDirectory, cssDestFile);
        }

        FileUtil.delete(cssTempFile);

        cssChanged.set(numUpdated > 0);
    }

    private void refreshCssFile(final CssDirectory cssDirectory, final File cssDestFile) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final LocalFileSystem localFileSystem = LocalFileSystem.getInstance();

                localFileSystem.refresh(false);
                localFileSystem.refreshAndFindFileByPath(cssDestFile.getAbsolutePath());

                final VirtualFile virtualCssRoot = localFileSystem.findFileByIoFile(new File(cssDirectory.getPath()));

                if (virtualCssRoot != null)
                    virtualCssRoot.refresh(false, true);

                final VirtualFile virtualCssFile = localFileSystem.findFileByIoFile(cssDestFile);

                if (virtualCssFile != null)
                    virtualCssFile.refresh(false, false);
            }
        });
    }

    /*
     * Public static methods
     */

    public static boolean isLessFile(@NotNull final String filename) {
        return filename.endsWith(".less");
    }

    public static boolean isSameFile(@Nullable final LessFile file1, @Nullable final LessFile file2) {
        return  file1 != null &&
                file2 != null &&
                file1.getCanonicalPathSafe().equals(file2.getCanonicalPathSafe());
    }

    /*
     * Private static methods
     */

    private static List<String> normalizePatterns(final String patterns) {
        final Set<String> normalized = new LinkedHashSet<String>();
        for (String pattern : StringUtils.defaultString(patterns).split(";")) {
            if (StringUtils.isNotEmpty(pattern))
                normalized.add(makePatternAbsolute(pattern));
        }
        return new ArrayList<String>(normalized);
    }

    private static String makePatternAbsolute(final String pattern) {
        return FileUtil.isAbsolute(pattern) ? pattern : "*" + File.separator + pattern;
    }

    /**
     * Resolves extensionless {@code @import} LESS filenames so that they always end with ".less".
     * <p>Examples:</p>
     * <pre>resolveImportFileName("main") = "main.less"</pre>
     * <pre>resolveImportFileName("main.less") = "main.less"</pre>
     * @param filename filename from a LESS {@code @import} that may or may not end with ".less"
     * @return the filename ending with ".less"
     */
    private static String resolveImportFileName(@Nullable String filename) {
        filename = StringUtils.defaultString(filename);
        if (filename.endsWith(".less"))
            return filename;
        else
            return filename + ".less";
    }

    /**
     * Returns a set of all LESS files in the profile directory or one of its subdirectories (recursive)
     * that match the given filter criteria.
     * @param lessProfile LESS profile
     * @param filter filter to reduce the result set
     * @return all LESS files in the profile directory (recursive)
     * @throws IOException
     */
    public static Set<LessFile> getLessFiles(@NotNull final LessProfile lessProfile,
                                             @NotNull final Filter filter) throws IOException {
        final List<File> files = FileUtil.findFilesByMask(LESS_FILENAME_PATTERN, lessProfile.getLessDir());
        final Set<LessFile> lessFiles = new LinkedHashSet<LessFile>();
        for (File file : files) {
            final LessFile lessFile = new LessFile(file);
            if (filter.accept(lessFile)) {
                lessFiles.add(lessFile);
            }
        }
        return lessFiles;
    }

    private static Set<LessFile> mergeSets(@NotNull final Set<LessFile> paths1, @NotNull final Set<LessFile> paths2) {
        final Set<LessFile> mergedPaths = new LinkedHashSet<LessFile>();
        mergedPaths.addAll(paths1);
        mergedPaths.addAll(paths2);
        return mergedPaths;
    }
    
    private static Set<LessFile> filter(@NotNull final Set<LessFile> lessFiles, @NotNull final Filter filter) throws IOException {
        final Set<LessFile> filtered = new LinkedHashSet<LessFile>();
        for (LessFile lessFile : lessFiles) {
            if (filter.accept(lessFile)) {
                filtered.add(lessFile);
            }
        }
        return filtered;
    }

    /**
     * Similar to {@link #getCanonicalPath()}, but falls back to returning {@link #getAbsolutePath()} instead of throwing an {@link IOException}.
     * @return the canonical path to the {@code File} if it exists; otherwise the absolute path
     */
    private static String getCanonicalPathSafe(final File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ignored) {
            return file.getAbsolutePath();
        }
    }

    private static File createCssTempFile() throws IOException {
        return FileUtil.createTempFile("intellij-lessc-plugin.", ".css", true);
    }

    /*
     * Public inner classes
     */

    public static interface Filter {
        public boolean accept(@NotNull LessFile lessFile) throws IOException;
    }
}
