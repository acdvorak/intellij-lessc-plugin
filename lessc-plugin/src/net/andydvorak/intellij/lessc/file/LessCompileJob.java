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
import com.intellij.openapi.util.io.FileUtil;
import net.andydvorak.intellij.lessc.state.LessProfile;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class LessCompileJob {

    private final LessFile sourceLessFile;
    private final LessProfile lessProfile;
    private final Set<LessFile> updatedLessFiles;
    private final Set<String> updatedLessFilePaths;
    private File cssTempFile;
    private LessEngine lessEngine;

    public LessCompileJob(final LessFile sourceLessFile, final LessProfile lessProfile) {
        this.sourceLessFile = sourceLessFile;
        this.lessProfile = lessProfile;
        this.updatedLessFiles = new LinkedHashSet<LessFile>();
        this.updatedLessFilePaths = new LinkedHashSet<String>();
    }

    public LessCompileJob(final LessCompileJob otherCompileJob, final LessFile sourceLessFile) {
        this.sourceLessFile = sourceLessFile;
        this.lessProfile = otherCompileJob.getLessProfile();
        this.updatedLessFiles = new LinkedHashSet<LessFile>(otherCompileJob.getUpdatedLessFiles());
        this.updatedLessFilePaths = new LinkedHashSet<String>(otherCompileJob.getUpdatedLessFilePaths());
    }

    public LessFile getSourceLessFile() {
        return sourceLessFile;
    }

    public LessProfile getLessProfile() {
        return lessProfile;
    }

    public void addUpdatedLessFile(LessFile lessFile) {
        updatedLessFiles.add(lessFile);
        updatedLessFilePaths.add(lessFile.getCanonicalPathSafe());
    }

    /**
     * @return set of LESS {@code File}s that produced new or updated CSS files after being compiled
     */
    public Set<LessFile> getUpdatedLessFiles() {
        return new LinkedHashSet<LessFile>(updatedLessFiles);
    }

    /**
     * @return set of paths to LESS {@code File}s that produced new or updated CSS files after being compiled
     */
    public Set<String> getUpdatedLessFilePaths() {
        return new LinkedHashSet<String>(updatedLessFilePaths);
    }

    public int getNumUpdated() {
        return getUpdatedLessFiles().size();
    }

    public File getCssTempFile() throws IOException {
        if (cssTempFile == null) {
            cssTempFile = FileUtil.createTempFile("intellij-lessc-plugin.", ".css", true);
        }
        return cssTempFile;
    }

    public void compile() throws IOException, LessException {
        if (lessEngine == null) {
            lessEngine = new LessEngine();
        }
        sourceLessFile.compile(lessEngine, getCssTempFile(), lessProfile.isCompressOutput());
    }

    public void refreshVFS() {
        VFSLocationChange.refresh(lessProfile.getCssDirectories());
    }
}
