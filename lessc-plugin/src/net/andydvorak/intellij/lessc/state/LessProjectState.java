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

    public boolean moveCssFiles = true;
    public boolean promptOnMove = true;

    public boolean copyCssFiles = true;
    public boolean promptOnCopy = true;

    public boolean deleteCssFiles = true;
    public boolean promptOnDelete = true;

    public boolean isMoveCssFiles() {
        return moveCssFiles;
    }

    public void setMoveCssFiles(boolean moveCssFiles) {
        this.moveCssFiles = moveCssFiles;
    }

    public boolean isPromptOnMove() {
        return promptOnMove;
    }

    public void setPromptOnMove(boolean promptOnMove) {
        this.promptOnMove = promptOnMove;
    }

    public boolean isCopyCssFiles() {
        return copyCssFiles;
    }

    public void setCopyCssFiles(boolean copyCssFiles) {
        this.copyCssFiles = copyCssFiles;
    }

    public boolean isPromptOnCopy() {
        return promptOnCopy;
    }

    public void setPromptOnCopy(boolean promptOnCopy) {
        this.promptOnCopy = promptOnCopy;
    }

    public boolean isDeleteCssFiles() {
        return deleteCssFiles;
    }

    public void setDeleteCssFiles(boolean deleteCssFiles) {
        this.deleteCssFiles = deleteCssFiles;
    }

    public boolean isPromptOnDelete() {
        return promptOnDelete;
    }

    public void setPromptOnDelete(boolean promptOnDelete) {
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LessProjectState that = (LessProjectState) o;

        if (copyCssFiles != that.copyCssFiles) return false;
        if (deleteCssFiles != that.deleteCssFiles) return false;
        if (moveCssFiles != that.moveCssFiles) return false;
        if (promptOnCopy != that.promptOnCopy) return false;
        if (promptOnDelete != that.promptOnDelete) return false;
        if (promptOnMove != that.promptOnMove) return false;
        if (lessProfiles != null ? !lessProfiles.equals(that.lessProfiles) : that.lessProfiles != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = lessProfiles != null ? lessProfiles.hashCode() : 0;
        result = 31 * result + (moveCssFiles ? 1 : 0);
        result = 31 * result + (promptOnMove ? 1 : 0);
        result = 31 * result + (copyCssFiles ? 1 : 0);
        result = 31 * result + (promptOnCopy ? 1 : 0);
        result = 31 * result + (deleteCssFiles ? 1 : 0);
        result = 31 * result + (promptOnDelete ? 1 : 0);
        return result;
    }
}
