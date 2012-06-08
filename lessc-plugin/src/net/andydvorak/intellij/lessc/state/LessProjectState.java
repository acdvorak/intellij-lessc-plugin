package net.andydvorak.intellij.lessc.state;

import java.util.LinkedHashMap;
import java.util.Map;

public class LessProjectState {
    public Map<String, LessProfile> lessProfiles = new LinkedHashMap<String, LessProfile>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LessProjectState that = (LessProjectState) o;

        if (lessProfiles != null ? !lessProfiles.equals(that.lessProfiles) : that.lessProfiles != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return lessProfiles != null ? lessProfiles.hashCode() : 0;
    }
}
