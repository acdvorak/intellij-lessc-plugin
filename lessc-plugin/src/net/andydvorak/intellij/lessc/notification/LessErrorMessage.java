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

package net.andydvorak.intellij.lessc.notification;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import javax.swing.event.HyperlinkEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LessErrorMessage extends Exception {

    private final Matcher matcher;
    private final String fileName;
    private final String filePath;
    private final String title;
    private final String message;
    private final String message_html;
    private final int line;
    private final int column;

    public LessErrorMessage(final @NotNull String lessFilePath, final @NotNull String lessFileName, final @NotNull Throwable t) {
        super(t);

        filePath = lessFilePath;
        fileName = lessFileName;

        title = "LESS CSS Compiler Error";
        message = t.getLocalizedMessage();
        matcher = Pattern.compile("line ([0-9]+), column ([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(message);

        if (matcher.find()) {
            message_html = matcher.replaceFirst("<a href='line_col'>$0</a>");
            line = Integer.parseInt(matcher.group(1));
            column = Integer.parseInt(matcher.group(2));
        } else {
            message_html = message;
            line = column = -1;
        }
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getText() {
        return getFileName() + ": " + message;
    }

    public String getHtml() {
        return "<a href='line_col'>" + getFileName() + "</a>" + ": " + message_html;
    }

    public Notification getNotification(@NotNull final Project project, @NotNull final NotificationGroup group, @NotNull final NotificationType type) {
        final MyNotificationListener listener = new MyNotificationListener(project, getFilePath(), getLine(), getColumn());
        return Notifier.createNotification(group, title, getHtml(), type, listener);
    }

    private static class MyNotificationListener implements NotificationListener {

        @NotNull private final Project myProject;
        @NotNull private final String filePath;
        private final int line;
        private final int column;

        private MyNotificationListener(@NotNull final Project project, @NotNull final String filePath, final int line, final int column) {
            this.myProject = project;
            this.filePath = filePath;
            this.line = line - 1; // for visual placement only
            this.column = column;
        }

        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (event.getDescription().equals("line_col") && !myProject.isDisposed()) {
                    final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
                    if (file != null) {
                        final FileEditorManager editorManager = FileEditorManager.getInstance(myProject);
                        final Editor editor = editorManager.openTextEditor(new OpenFileDescriptor(myProject, file, line - 1, column), true);

                        assert editor != null;

                        // Set correct caret position
                        // See https://github.com/johnlindquist/open-source-plugins/blob/master/QuickJump/src/com/johnlindquist/quickjump/QuickJumpAction.java
                        editor.getCaretModel().moveToVisualPosition(editor.logicalToVisualPosition(new LogicalPosition(line, column)));

                        notification.expire();
                    }
                }
                else if (event.getDescription().equals("ignore")) {
                    notification.expire();
                }
            }
        }
    }

}