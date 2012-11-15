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

package net.andydvorak.intellij.lessc.ui.configurable;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileEvent;
import net.andydvorak.intellij.lessc.ui.messages.UIBundle;
import net.andydvorak.intellij.lessc.state.LessProjectState;

/**
 * @author Andrew C. Dvorak
 * @since 11/1/12
 */
public class VfsLocationChangeDialog {
    private int result = -1;
    private long lastPrompt = -1;
    private long promptIntervalMillis = 1000;
    private final Project myProject;
    private final LessProjectState myLessProjectState;

    public VfsLocationChangeDialog(final Project project, final LessProjectState state) {
        myProject = project;
        myLessProjectState = state;
    }

    public synchronized boolean shouldMoveCssFile(final VirtualFileEvent virtualFileEvent) {
        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return myLessProjectState.isPromptOnMove();
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                myLessProjectState.setPromptOnMove(value);
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return true;
            }

            @Override
            public String getDoNotShowMessage() {
                return UIBundle.message("do.not.ask.me.again");
            }
        };

        if (option.isToBeShown()) {
            if (System.currentTimeMillis() - lastPrompt > promptIntervalMillis) {
                result = Messages.showYesNoDialog(
                        UIBundle.message("vfs.move.message", virtualFileEvent.getFileName()),
                        UIBundle.message("vfs.move.title"), // Title
                        UIBundle.message("vfs.move.yes"), // "Yes" button text
                        UIBundle.message("vfs.move.no"), // "No" button text
                        Messages.getQuestionIcon(),
                        option);
            }
        }
        else {
            result = myLessProjectState.isMoveCssFiles() ? DialogWrapper.OK_EXIT_CODE : DialogWrapper.CANCEL_EXIT_CODE;
        }

        lastPrompt = System.currentTimeMillis();

        final boolean shouldMove = result == DialogWrapper.OK_EXIT_CODE;

        myLessProjectState.setMoveCssFiles(shouldMove);

        return shouldMove;
    }

    public synchronized boolean shouldCopyCssFile(final VirtualFileEvent virtualFileEvent) {
        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return myLessProjectState.isPromptOnCopy();
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                myLessProjectState.setPromptOnCopy(value);
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return true;
            }

            @Override
            public String getDoNotShowMessage() {
                return UIBundle.message("do.not.ask.me.again");
            }
        };

        if (option.isToBeShown()) {
            if (System.currentTimeMillis() - lastPrompt > promptIntervalMillis) {
                result = Messages.showYesNoDialog(
                        UIBundle.message("vfs.copy.message", virtualFileEvent.getFileName()),
                        UIBundle.message("vfs.copy.title"), // Title
                        UIBundle.message("vfs.copy.yes"), // "Yes" button text
                        UIBundle.message("vfs.copy.no"), // "No" button text
                        Messages.getQuestionIcon(),
                        option);
            }
        }
        else {
            result = myLessProjectState.isCopyCssFiles() ? DialogWrapper.OK_EXIT_CODE : DialogWrapper.CANCEL_EXIT_CODE;
        }

        lastPrompt = System.currentTimeMillis();

        final boolean shouldCopy = result == DialogWrapper.OK_EXIT_CODE;

        myLessProjectState.setCopyCssFiles(shouldCopy);

        return shouldCopy;
    }

    public synchronized boolean shouldDeleteCssFile(final VirtualFileEvent virtualFileEvent) {
        DialogWrapper.DoNotAskOption option = new DialogWrapper.DoNotAskOption() {
            @Override
            public boolean isToBeShown() {
                return myLessProjectState.isPromptOnDelete();
            }

            @Override
            public void setToBeShown(boolean value, int exitCode) {
                myLessProjectState.setPromptOnDelete(value);
            }

            @Override
            public boolean canBeHidden() {
                return true;
            }

            @Override
            public boolean shouldSaveOptionsOnCancel() {
                return true;
            }

            @Override
            public String getDoNotShowMessage() {
                return UIBundle.message("do.not.ask.me.again");
            }
        };

        if (option.isToBeShown()) {
            if (System.currentTimeMillis() - lastPrompt > promptIntervalMillis) {
                result = Messages.showYesNoDialog(
                        UIBundle.message("vfs.delete.message", virtualFileEvent.getFileName()),
                        UIBundle.message("vfs.delete.title"), // Title
                        UIBundle.message("vfs.delete.yes"), // "Yes" button text
                        UIBundle.message("vfs.delete.no"), // "No" button text
                        Messages.getQuestionIcon(),
                        option);
            }
        }
        else {
            result = myLessProjectState.isDeleteCssFiles() ? DialogWrapper.OK_EXIT_CODE : DialogWrapper.CANCEL_EXIT_CODE;
        }

        lastPrompt = System.currentTimeMillis();

        final boolean shouldDelete = result == DialogWrapper.OK_EXIT_CODE;

        myLessProjectState.setDeleteCssFiles(shouldDelete);

        return shouldDelete;
    }
}
