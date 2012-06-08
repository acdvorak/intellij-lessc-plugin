package net.andydvorak.intellij.lessc.state;

import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;

public class CssDirectory {
    @Transient
    private LessProfile lessProfile;

    private String cssDirPath;

    public CssDirectory(final String path, final LessProfile profile) {
        cssDirPath = path;
        lessProfile = profile;
    }

    public LessProfile getProfile() {
        return lessProfile;
    }

    public void setProfile(@NotNull final LessProfile profile) {
        lessProfile = profile;
    }

    public String getProfileName() {
        return lessProfile.getName();
    }

    public String getPath() {
        return cssDirPath;
    }

    public void setPath(final String path) {
        this.cssDirPath = path;
    }

    @Override
    public boolean equals(Object o) {
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
