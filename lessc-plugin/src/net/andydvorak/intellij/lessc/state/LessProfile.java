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

package net.andydvorak.intellij.lessc.state;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LessProfile {
    private String lessDirPath;
    private List<CssDirectory> cssDirectories = new ArrayList<CssDirectory>();
    private String includePattern = "";
    private String excludePattern = "";
    private boolean compressOutput = false;
    private String name = "";

    // Read from external .xml file
    @SuppressWarnings("UnusedDeclaration")
    public LessProfile() {
    }

    public LessProfile(final String profileName) {
        this.name = profileName;
    }

    // Clone
    public LessProfile(final LessProfile other) {
        this.copyFrom(other);
    }

    public String getLessDirPath() {
        return lessDirPath;
    }

    public void setLessDirPath(final String lessDirPath) {
        this.lessDirPath = lessDirPath;
    }

    public File getLessDir() {
        return new File(lessDirPath);
    }

    public List<CssDirectory> getCssDirectories() {
        return cssDirectories;
    }

    public void setCssDirectories(final List<CssDirectory> cssDirectories) {
        this.cssDirectories = cssDirectories;
    }

    public boolean hasCssDirectories() {
        return ! cssDirectories.isEmpty();
    }

    @NotNull
    public String getIncludePattern() {
        return StringUtils.defaultString(includePattern);
    }

    public void setIncludePattern(String includePattern) {
        this.includePattern = includePattern;
    }

    @NotNull
    public String getExcludePattern() {
        return StringUtils.defaultString(excludePattern);
    }

    public void setExcludePattern(String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public boolean isCompressOutput() {
        return compressOutput;
    }

    public void setCompressOutput(final boolean compressOutput) {
        this.compressOutput = compressOutput;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void copyFrom(final LessProfile lessProfile) {
        this.name = lessProfile.name;
        this.lessDirPath = lessProfile.lessDirPath;
        this.compressOutput = lessProfile.compressOutput;
        this.includePattern = lessProfile.includePattern;
        this.excludePattern = lessProfile.excludePattern;
        this.cssDirectories.clear();

        for (CssDirectory cssDirectory : lessProfile.cssDirectories) {
            this.cssDirectories.add(new CssDirectory(cssDirectory));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LessProfile that = (LessProfile) o;

        if (compressOutput != that.compressOutput) return false;
        if (cssDirectories != null ? !cssDirectories.equals(that.cssDirectories) : that.cssDirectories != null)
            return false;
        if (excludePattern != null ? !excludePattern.equals(that.excludePattern) : that.excludePattern != null)
            return false;
        if (includePattern != null ? !includePattern.equals(that.includePattern) : that.includePattern != null)
            return false;
        if (lessDirPath != null ? !lessDirPath.equals(that.lessDirPath) : that.lessDirPath != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = lessDirPath != null ? lessDirPath.hashCode() : 0;
        result = 31 * result + (cssDirectories != null ? cssDirectories.hashCode() : 0);
        result = 31 * result + (includePattern != null ? includePattern.hashCode() : 0);
        result = 31 * result + (excludePattern != null ? excludePattern.hashCode() : 0);
        result = 31 * result + (compressOutput ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
