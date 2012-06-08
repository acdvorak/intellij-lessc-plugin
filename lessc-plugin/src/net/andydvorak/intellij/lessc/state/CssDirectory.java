package net.andydvorak.intellij.lessc.state;

import java.io.File;

public class CssDirectory {

    private String cssDirPath;

    // For XML serialization
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

        CssDirectory that = (CssDirectory) o;

        if (cssDirPath != null ? !cssDirPath.equals(that.cssDirPath) : that.cssDirPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return cssDirPath != null ? cssDirPath.hashCode() : 0;
    }
}
