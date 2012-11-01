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

package net.andydvorak.intellij.lessc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileEvent;

/**
 * @author Andrew C. Dvorak
 * @since 11/1/12
 */
public class FileLocationChangeDialog {
    private int result = -1;
    private long lastPrompt = -1;
    private long promptIntervalMillis = 1000;
    private final Project myProject;

    public FileLocationChangeDialog(final Project project) {
        myProject = project;
    }

    public synchronized boolean shouldMoveCssFile(final VirtualFileEvent virtualFileEvent) {
        if (System.currentTimeMillis() - lastPrompt > promptIntervalMillis) {
            result = Messages.showYesNoDialog(myProject,
                    virtualFileEvent.getFileName() + " has moved.  Would you like to move the corresponding CSS files as well?",
                    "LESS File Moved", // Title
                    "Move CSS file(s)", // "Yes" button text
                    "Don't move CSS file(s)", // "No" button text
                    Messages.getQuestionIcon());
        }
        lastPrompt = System.currentTimeMillis();
        return result == 0; // 0 = yes, 1 = no
    }

    public synchronized boolean shouldCopyCssFile(final VirtualFileEvent virtualFileEvent) {
        if (System.currentTimeMillis() - lastPrompt > promptIntervalMillis) {
            result = Messages.showYesNoDialog(myProject,
                    virtualFileEvent.getFileName() + " has been copied.  Would you like to copy the corresponding CSS files as well?",
                    "LESS File Copied", // Title
                    "Copy CSS file(s)", // "Yes" button text
                    "Don't copy CSS file(s)", // "No" button text
                    Messages.getQuestionIcon());
        }
        lastPrompt = System.currentTimeMillis();
        return result == 0; // 0 = yes, 1 = no
    }

    public synchronized boolean shouldDeleteCssFile(final VirtualFileEvent virtualFileEvent) {
        if (System.currentTimeMillis() - lastPrompt > promptIntervalMillis) {
            result = Messages.showYesNoDialog(myProject,
                    virtualFileEvent.getFileName() + " was deleted.  Would you like to delete the corresponding CSS files as well?",
                    "LESS File Deleted", // Title
                    "Delete CSS file(s)", // "Yes" button text
                    "Don't delete CSS file(s)", // "No" button text
                    Messages.getQuestionIcon());
        }
        lastPrompt = System.currentTimeMillis();
        return result == 0; // 0 = yes, 1 = no
    }
}
