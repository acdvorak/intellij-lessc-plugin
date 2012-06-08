package net.andydvorak.intellij.lessc.state;

import java.util.ArrayList;
import java.util.List;

public class LessProfile /* extends ProfileEx */ {
    private String lessDir;
    private List<CssDirectory> cssDirectories = new ArrayList<CssDirectory>();
    private boolean compressOutput = false;
    private String name = "";

    // Read from external .xml file
    public LessProfile() {
    }

    public LessProfile(String profileName) {
        this.name = profileName;
    }

    // Clone
    public LessProfile(LessProfile other) {
        this.copyFrom(other);
    }

    public String getLessDir() {
        return lessDir;
    }

    public void setLessDir(String lessDir) {
        this.lessDir = lessDir;
    }

    public List<CssDirectory> getCssDirectories() {
        return cssDirectories;
    }

    public void setCssDirectories(List<CssDirectory> cssDirectories) {
        this.cssDirectories = cssDirectories;
    }

    public void addCssDirectory(final CssDirectory cssDirectory) {
        cssDirectories.add(cssDirectory);
    }

    public void removeCssDirectory(final CssDirectory cssDirectory) {
        cssDirectories.remove(cssDirectory);
    }

    public boolean isCompressOutput() {
        return compressOutput;
    }

    public void setCompressOutput(boolean compressOutput) {
        this.compressOutput = compressOutput;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void copyFrom(LessProfile lessProfile) {
        this.lessDir = new String(lessProfile.lessDir);
        this.cssDirectories.clear();

        for ( CssDirectory cssDirectory : lessProfile.cssDirectories ) {
            this.cssDirectories.add(new CssDirectory(cssDirectory));
        }

        this.compressOutput = lessProfile.compressOutput;
        this.name = new String(lessProfile.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LessProfile that = (LessProfile) o;

        if (compressOutput != that.compressOutput) return false;
        if (cssDirectories != null ? !cssDirectories.equals(that.cssDirectories) : that.cssDirectories != null)
            return false;
        if (lessDir != null ? !lessDir.equals(that.lessDir) : that.lessDir != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = lessDir != null ? lessDir.hashCode() : 0;
        result = 31 * result + (cssDirectories != null ? cssDirectories.hashCode() : 0);
        result = 31 * result + (compressOutput ? 1 : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }
}
