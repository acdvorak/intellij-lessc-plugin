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
