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

package net.andydvorak.intellij.lessc.ui.notifier;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * Listens for {@link HyperlinkEvent}s on a {@link Notification} object.
 * If the user clicks on a link to a file, the file will be opened in the editor with the cursor
 * placed at the appropriate line / column.  Clicking "ignore" or "dismiss" will expire the notification.
 */
public class NotificationListenerImpl implements NotificationListener {

    @NotNull
    private final Project myProject;
    @Nullable
    private final String filePath;
    private final int line;
    private final int column;

    public NotificationListenerImpl(@NotNull final Project project) {
        this(project, null);
    }

    public NotificationListenerImpl(@NotNull final Project project, @Nullable final String filePath) {
        this(project, filePath, -1, -1);
    }

    public NotificationListenerImpl(@NotNull final Project project, @Nullable final String filePath, final int line, final int column) {
        this.myProject = project;
        this.filePath = filePath;
        this.line = line;
        this.column = column;
    }

    private LogicalPosition getLogicalPosition() {
        return line != -1 || column != -1 ? new LogicalPosition(line > -1 ? line - 1 : 0, column > -1 ? column : 0) : null;
    }

    private boolean isSupportedEvent(@NotNull HyperlinkEvent event) {
        return !myProject.isDisposed() && event.getEventType() == HyperlinkEvent.EventType.ACTIVATED;
    }

    private boolean isViewFileEvent(@NotNull HyperlinkEvent event) {
        final String description = StringUtils.defaultString(event.getDescription());
        return "file".equals(description) || description.endsWith(".less") || description.endsWith(".css");
    }

    private boolean isIgnoreEvent(@NotNull HyperlinkEvent event) {
        final String description = event.getDescription();
        return "ignore".equals(description) || "dismiss".equals(description);
    }

    @Override
    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
        if (!isSupportedEvent(event))
            return;

        if (isViewFileEvent(event)) {
            String curFilePath = this.filePath;

            if (curFilePath == null) {
                curFilePath = event.getDescription(); // retrieves the "href" attribute of the hyperlink
            }

            final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(curFilePath);
            if (file != null) {
                final FileEditorManager editorManager = FileEditorManager.getInstance(myProject);
                final Editor editor = editorManager.openTextEditor(new OpenFileDescriptor(myProject, file), true);
                final LogicalPosition logicalPosition = getLogicalPosition();

                if(editor != null && logicalPosition != null) {
                    // Set correct caret position
                    // See https://github.com/johnlindquist/open-source-plugins/blob/master/QuickJump/src/com/johnlindquist/quickjump/QuickJumpAction.java
                    editor.getCaretModel().moveToVisualPosition(editor.logicalToVisualPosition(logicalPosition));
                }

                notification.hideBalloon();
            }
        } else if (isIgnoreEvent(event)) {
            notification.expire();
        }
    }

}