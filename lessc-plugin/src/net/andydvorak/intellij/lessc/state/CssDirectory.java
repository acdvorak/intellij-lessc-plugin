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

public class CssDirectory {

    private String cssDirPath;

    // For XML serialization
    @SuppressWarnings("UnusedDeclaration")
    public CssDirectory() {
    }

    public CssDirectory(final String path) {
        cssDirPath = path;
    }

    public CssDirectory(final CssDirectory other) {
        this.copyFrom(other);
    }

    public String getPath() {
        return cssDirPath;
    }

    public void setPath(final String path) {
        this.cssDirPath = path;
    }

    public void copyFrom(final CssDirectory other) {
        this.cssDirPath = other.cssDirPath;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CssDirectory that = (CssDirectory) o;
        return !(cssDirPath != null ? !cssDirPath.equals(that.cssDirPath) : that.cssDirPath != null);
    }

    @Override
    public int hashCode() {
        return cssDirPath != null ? cssDirPath.hashCode() : 0;
    }
}
