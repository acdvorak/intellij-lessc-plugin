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

package net.andydvorak.intellij.lessc.file;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LessFile extends File implements Comparable<File> {

    private static final Pattern LESS_IMPORT_PATTERN = Pattern.compile("@import \"([^\"]+)\";");
    private static final Pattern LESS_FILENAME_PATTERN = Pattern.compile(".*\\.less$");

    private static final Logger LOG = Logger.getInstance("#" + LessFile.class.getName());

    public LessFile(final String path) {
        super(path);
    }

    public LessFile(final String parent, final String child) {
        super(parent, child);
    }

    public LessFile(final File parent, final String child) {
        super(parent, child);
    }

    public LessFile(final URI uri) {
        super(uri);
    }

    /**
     * Similar to {@link #getCanonicalPath()}, but falls back to returning {@link #getAbsolutePath()} instead of throwing an {@link IOException}.
     * @return the canonical path to the {@code File} if it exists; otherwise the absolute path
     */
    public String getCanonicalPathSafe() {
        try {
            return super.getCanonicalPath();
        } catch (IOException ignored) {
            return getAbsolutePath();
        }
    }

    /**
     * Similar to {@link #getCanonicalPath()}, but returns {@code null} instead of throwing an {@link IOException}.
     * Any HTML Entities in the resulting path will be escaped (encoded) so that the path is safe to insert into HTML code.
     * @return the canonical, HTML-encoded path to the {@code File} if it exists; otherwise {@code null}
     */
    public String getCanonicalPathSafeHtmlEscaped() {
        return StringEscapeUtils.escapeHtml4(getCanonicalPathSafe());
    }

    public void compile(final LessEngine engine, final File cssFile, final boolean compress) throws IOException, LessException {
        LOG.info("Compiling LESS file: " + getName());
        LOG.info("\t" + "lessPath: " + getCanonicalPath());
        LOG.info("\t" + "cssPath: " + cssFile.getCanonicalPath());

        final String compiled = engine.compile(toURL(), compress);

        FileUtil.writeToFile(cssFile, compiled);
    }

    @Nullable
    public LessProfile getLessProfile(final Collection<LessProfile> lessProfiles) {
        for ( LessProfile lessProfile : lessProfiles ) {
            final File lessProfileDir = new File(lessProfile.getLessDirPath());
            if ( FileUtil.isAncestor(lessProfileDir, this, false) ) {
                return lessProfile;
            }
        }
        return null;
    }

    public boolean shouldCompile(@Nullable final LessProfile lessProfile) {
        if (lessProfile == null)
            return false;

        final List<String> includePatterns = getNormalizePatterns(lessProfile.getIncludePattern());
        final List<String> excludePatterns = getNormalizePatterns(lessProfile.getExcludePattern());

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

    private List<String> getNormalizePatterns(final String patterns) {
        final Set<String> normalized = new HashSet<String>();
        for (String pattern : StringUtils.defaultString(patterns).split(";")) {
            if (StringUtils.isNotEmpty(pattern))
                normalized.add(makePatternAbsolute(pattern));
        }
        return new ArrayList<String>(normalized);
    }

    private boolean matches(final String pattern) {
        return FilenameUtils.wildcardMatchOnSystem(getCanonicalPathSafe(), makePatternAbsolute(pattern));
    }

    private String makePatternAbsolute(final String pattern) {
        return FileUtil.isAbsoluteFilePath(pattern) ? pattern : "*" + File.separator + pattern;
    }

    public static boolean isLessFile(final String filename) {
        return filename.endsWith(".less");
    }

    public static boolean isSameFile(final File file1, final File file2) throws IOException {
        return file1.getCanonicalPath().equals(file2.getCanonicalPath());
    }

    /**
     * Returns a list of paths to all LESS files in the profile directory that @import the given LESS file.
     * @param sourceLessFile
     * @param lessProfile
     * @return
     * @throws IOException
     */
    public static Set<String> getDependentPaths(final LessFile sourceLessFile, final LessProfile lessProfile) throws IOException {
        final String lessParentPath = sourceLessFile.getParent();
        final Set<String> lessFiles = new LinkedHashSet<String>();
        final Matcher importMatcher = LESS_IMPORT_PATTERN.matcher("");

        final List<File> filesByMask = FileUtil.findFilesByMask(LESS_FILENAME_PATTERN, lessProfile.getLessDir());
        for ( File dependentLessFile : filesByMask) {
            if ( ! isSameFile(sourceLessFile, dependentLessFile) ) {
                importMatcher.reset(FileUtil.loadFile(dependentLessFile));

                while ( importMatcher.find() ) {
                    final LessFile importedLessFile = new LessFile(lessParentPath, importMatcher.group(1));

                    if ( isSameFile(sourceLessFile, importedLessFile) && new LessFile(dependentLessFile.getAbsolutePath()).shouldCompile(lessProfile) ) {
                        lessFiles.add(dependentLessFile.getCanonicalPath());
                    }
                }
            }
        }

        return lessFiles;
    }
}
