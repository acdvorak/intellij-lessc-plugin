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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public class LessProjectState {

    /**
     * Old profile map.  Uses profile names as keys.  This is problematic when renaming/deleting profiles.
     */
    public Map<String, LessProfile> lessProfiles = new LinkedHashMap<String, LessProfile>();

    /**
     * New profile map.  Uses unique profile IDs as keys.  {@link #lessProfiles} must be automatically migrated into this map.
     */
    public Map<Integer, LessProfile> lessProfileMap = new LinkedHashMap<Integer, LessProfile>();

    public boolean moveCssFiles = true;
    public boolean promptOnMove = true;

    public boolean copyCssFiles = true;
    public boolean promptOnCopy = true;

    public boolean deleteCssFiles = true;
    public boolean promptOnDelete = true;

    public boolean isMoveCssFiles() {
        return moveCssFiles;
    }

    public void setMoveCssFiles(final boolean moveCssFiles) {
        this.moveCssFiles = moveCssFiles;
    }

    public boolean isPromptOnMove() {
        return promptOnMove;
    }

    public void setPromptOnMove(final boolean promptOnMove) {
        this.promptOnMove = promptOnMove;
    }

    public boolean isCopyCssFiles() {
        return copyCssFiles;
    }

    public void setCopyCssFiles(final boolean copyCssFiles) {
        this.copyCssFiles = copyCssFiles;
    }

    public boolean isPromptOnCopy() {
        return promptOnCopy;
    }

    public void setPromptOnCopy(final boolean promptOnCopy) {
        this.promptOnCopy = promptOnCopy;
    }

    public boolean isDeleteCssFiles() {
        return deleteCssFiles;
    }

    public void setDeleteCssFiles(final boolean deleteCssFiles) {
        this.deleteCssFiles = deleteCssFiles;
    }

    public boolean isPromptOnDelete() {
        return promptOnDelete;
    }

    public void setPromptOnDelete(final boolean promptOnDelete) {
        this.promptOnDelete = promptOnDelete;
    }

    public void resetPrompts() {
        moveCssFiles = true;
        promptOnMove = true;

        copyCssFiles = true;
        promptOnCopy = true;

        deleteCssFiles = true;
        promptOnDelete = true;
    }

    public boolean hasDefaultPromptSettings() {
        return moveCssFiles && promptOnMove && copyCssFiles && promptOnCopy && deleteCssFiles && promptOnDelete;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final LessProjectState that = (LessProjectState) o;

        return new EqualsBuilder()
                .append(copyCssFiles,   that.copyCssFiles)
                .append(deleteCssFiles, that.deleteCssFiles)
                .append(moveCssFiles,   that.moveCssFiles)
                .append(promptOnCopy,   that.promptOnCopy)
                .append(promptOnDelete, that.promptOnDelete)
                .append(promptOnMove,   that.promptOnMove)
                .append(lessProfiles,   that.lessProfiles)
                .append(lessProfileMap, that.lessProfileMap)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(copyCssFiles)
                .append(deleteCssFiles)
                .append(moveCssFiles)
                .append(promptOnCopy)
                .append(promptOnDelete)
                .append(promptOnMove)
                .append(lessProfiles)
                .append(lessProfileMap)
                .hashCode();
    }

}
