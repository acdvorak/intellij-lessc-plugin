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

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Andrew C. Dvorak
 * @since 10/25/12
 */
public class Notifier {

    public static final NotificationGroup LOG_ONLY = NotificationGroup.logOnlyGroup(
            "LESS Compiler Notices");

    public static final NotificationGroup CHANGES_TOOLWINDOW_BALLOON = NotificationGroup.toolWindowGroup(
            "LESS Compiler Successful Compiles", ChangesViewContentManager.TOOLWINDOW_ID, false);

    public static final NotificationGroup IMPORTANT_ERROR_NOTIFICATION = new NotificationGroup(
            "LESS Compiler Error Messages", NotificationDisplayType.STICKY_BALLOON, true);

    private final Project myProject;

    public static Notifier getInstance(@NotNull final Project project) {
        return new Notifier(project);
    }

    public Notifier(@NotNull final Project project) {
        myProject = project;
    }

    @NotNull
    public static Notification createNotification(@NotNull NotificationGroup notificationGroup,
                                                  @NotNull String title, @NotNull String message, @NotNull NotificationType type,
                                                  @Nullable NotificationListener listener) {
        // title can be empty; description can't be null or empty
        if (StringUtil.isEmptyOrSpaces(message)) {
            message = title;
            title = "";
        }

        // if both title and description were empty, then it is a problem in the calling code => Notifications engine assertion will notify.
        return notificationGroup.createNotification(title, message, type, listener);
    }

    /*
     * Generic
     */

    public void notify(@NotNull final Notification notification) {
        notification.notify(myProject);
    }

    public void notify(@NotNull final NotificationGroup notificationGroup,
                       @NotNull final String title, @NotNull final String message,
                       @NotNull final NotificationType type, @Nullable final NotificationListener listener) {
        notify(createNotification(notificationGroup, title, message, type, listener));
    }

    /*
     * Log Only
     */

    public void log(@NotNull final String title, @NotNull final String message,
                    @Nullable final NotificationListener listener) {
        notify(LOG_ONLY, title, message, NotificationType.INFORMATION, listener);
    }

    /*
     * Error
     */

    public void notifyError(@NotNull final LessErrorMessage m) {
        final NotificationListener listener = new FileNotificationListener(myProject, m.getFilePath(), m.getLine(), m.getColumn());
        notify(createNotification(IMPORTANT_ERROR_NOTIFICATION, m.getTitle(), m.getHtml(), NotificationType.ERROR, listener));
    }

    /*
     * Success
     */

    public void notifySuccessBalloon(@NotNull final String title, @NotNull final String message,
                                     @Nullable final NotificationListener listener) {
        notify(CHANGES_TOOLWINDOW_BALLOON, title, message, NotificationType.INFORMATION, listener);
    }

}
