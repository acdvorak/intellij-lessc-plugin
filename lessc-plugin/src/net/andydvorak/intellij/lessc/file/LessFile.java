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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
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

    public void compile(final LessCompiler lessCompiler, final File cssFile, final boolean compress)  throws IOException, LessException {
        LOG.info("Compiling LESS file: " + getName());
        LOG.info("\t" + "lessPath: " + getCanonicalPath());
        LOG.info("\t" + "cssPath: " + cssFile.getCanonicalPath());

        lessCompiler.setCompress(compress);
        lessCompiler.compile(this, cssFile);
    }

    public LessProfile getLessProfile(Collection<LessProfile> lessProfiles) {
        for ( LessProfile lessProfile : lessProfiles ) {
            final File lessProfileDir = new File(lessProfile.getLessDirPath());
            if ( FileUtil.isAncestor(lessProfileDir, this, false) ) {
                return lessProfile;
            }
        }
        return null;
    }

    public static boolean isLessFile(final String filename) {
        return filename.endsWith(".less");
    }

    public static boolean isSameFile(final File file1, final File file2) throws IOException {
        return file1.getCanonicalPath().equals(file2.getCanonicalPath());
    }

    /**
     * Returns a list of paths to all LESS files in the profile directory that @import the given LESS file.
     * @param lessFile
     * @param lessProfile
     * @return
     * @throws IOException
     */
    public static Set<String> getImporterPaths(final LessFile lessFile, final LessProfile lessProfile) throws IOException {
        final String lessParentPath = lessFile.getParent();
        final Set<String> lessFiles = new LinkedHashSet<String>();
        final Matcher importMatcher = LESS_IMPORT_PATTERN.matcher("");

        for ( File otherLessFile : FileUtil.findFilesByMask(LESS_FILENAME_PATTERN, lessProfile.getLessDir()) ) {
            if ( ! isSameFile(lessFile, otherLessFile) ) {
                importMatcher.reset(FileUtil.loadFile(otherLessFile));

                while ( importMatcher.find() ) {
                    final LessFile importedLessFile = new LessFile(lessParentPath, importMatcher.group(1));

                    if ( isSameFile(lessFile, importedLessFile) ) {
                        lessFiles.add(otherLessFile.getCanonicalPath());
                    }
                }
            }
        }

        return lessFiles;
    }
}
