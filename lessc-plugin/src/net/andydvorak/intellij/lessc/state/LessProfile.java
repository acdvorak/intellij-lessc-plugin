package net.andydvorak.intellij.lessc.state;

import com.intellij.profile.ProfileEx;

import java.util.ArrayList;

public class LessProfile extends ProfileEx {
    private String lessDir;
    private ArrayList<CssDirectory> cssDirectories = new ArrayList<CssDirectory>();
    private boolean compressOutput = false;

    //read external
    public LessProfile() {
        super("");
    }

    public LessProfile(String profileName) {
        super(profileName);
    }

    public String getLessDir() {
        return lessDir;
    }

    public void setLessDir(String lessDir) {
        this.lessDir = lessDir;
    }

    public ArrayList<CssDirectory> getCssDirectories() {
        return cssDirectories;
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
}
