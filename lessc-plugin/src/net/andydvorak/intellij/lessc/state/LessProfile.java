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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LessProfile {

    private int id = -1;
    private String name = "";
    private String lessDirPath;
    private List<CssDirectory> cssDirectories = new ArrayList<CssDirectory>();
    private String includePattern = "";
    private String excludePattern = "";
    private boolean compileAutomatically = true;
    private boolean compressOutput = false;

    // For XML deserialization
    @SuppressWarnings("UnusedDeclaration")
    public LessProfile() {
    }

    public LessProfile(final int id, final String profileName) {
        this.id = id;
        this.name = profileName;
    }

    // Clone
    public LessProfile(final int id, final LessProfile other) {
        this.copyFrom(other);
        this.id = id;
    }

    public int getId() {
        return id;
    }

    // For XML deserialization
    @SuppressWarnings("UnusedDeclaration")
    public void setId(final int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    public void setIncludePattern(final String includePattern) {
        this.includePattern = includePattern;
    }

    @NotNull
    public String getExcludePattern() {
        return StringUtils.defaultString(excludePattern);
    }

    public void setExcludePattern(final String excludePattern) {
        this.excludePattern = excludePattern;
    }

    public boolean isCompileAutomatically() {
        return compileAutomatically;
    }

    public void setCompileAutomatically(final boolean compileAutomatically) {
        this.compileAutomatically = compileAutomatically;
    }

    public boolean isCompressOutput() {
        return compressOutput;
    }

    public void setCompressOutput(final boolean compressOutput) {
        this.compressOutput = compressOutput;
    }

    public void copyFrom(final LessProfile lessProfile) {
        id = lessProfile.id;
        name = lessProfile.name;
        lessDirPath = lessProfile.lessDirPath;
        includePattern = lessProfile.includePattern;
        excludePattern = lessProfile.excludePattern;
        cssDirectories.clear();
        for (final CssDirectory cssDirectory : lessProfile.cssDirectories) {
            cssDirectories.add(new CssDirectory(cssDirectory));
        }
        compileAutomatically = lessProfile.compileAutomatically;
        compressOutput = lessProfile.compressOutput;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final LessProfile that = (LessProfile) o;
        return new EqualsBuilder()
                .append(id,                   that.id)
                .append(name,                 that.name)
                .append(lessDirPath,          that.lessDirPath)
                .append(includePattern,       that.includePattern)
                .append(excludePattern,       that.excludePattern)
                .append(cssDirectories,       that.cssDirectories)
                .append(compileAutomatically, that.compileAutomatically)
                .append(compressOutput,       that.compressOutput)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(13, 17)
                .append(id)
                .append(name)
                .append(lessDirPath)
                .append(includePattern)
                .append(excludePattern)
                .append(cssDirectories)
                .append(compileAutomatically)
                .append(compressOutput)
                .hashCode();
    }

}
