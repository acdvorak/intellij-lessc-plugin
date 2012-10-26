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

import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;

/**
 * @author Andrew C. Dvorak
 * @since 10/25/12
 */
public class Notifier {

    public static final NotificationGroup NOTIFICATION_GROUP_ID = NotificationGroup.toolWindowGroup(
            "Less Compiler Messages", ChangesViewContentManager.TOOLWINDOW_ID, true);
    public static final NotificationGroup IMPORTANT_ERROR_NOTIFICATION = new NotificationGroup(
            "Less Compiler Important Messages", NotificationDisplayType.STICKY_BALLOON, true);
    public static final NotificationGroup MINOR_NOTIFICATION = new NotificationGroup(
            "Less Compiler Minor Notifications", NotificationDisplayType.BALLOON, true);

    private final @NotNull Project myProject;

    public static Notifier getInstance(@NotNull Project project) {
//        return ServiceManager.getService(project, Notifier.class);
        return new Notifier(project);
    }

    public Notifier(@NotNull Project project) {
        myProject = project;
    }

    @NotNull
    public static Notification createNotification(@NotNull NotificationGroup notificationGroup,
                                                  @NotNull String title, @NotNull String message, @NotNull NotificationType type,
                                                  @Nullable NotificationListener listener) {
        // title can be empty; description can't be neither null, nor empty
        if (StringUtil.isEmptyOrSpaces(message)) {
            message = title;
            title = "";
        }

        // if both title and description were empty, then it is a problem in the calling code => Notifications engine assertion will notify.
        return notificationGroup.createNotification(title, message, type, listener);
    }

    public void notify(@NotNull Notification notification) {
        notification.notify(myProject);
    }

    public void notify(@NotNull NotificationGroup notificationGroup, @NotNull String title, @NotNull String message,
                       @NotNull NotificationType type, @Nullable NotificationListener listener) {
        createNotification(notificationGroup, title, message, type, listener).notify(myProject);
    }

    public void notify(@NotNull NotificationGroup notificationGroup, @NotNull String title, @NotNull String message,
                       @NotNull NotificationType type) {
        notify(notificationGroup, title, message, type, null);
    }

    public void notifyError(@NotNull String title, @NotNull String message) {
        notifyError(title, message, null);
    }

    public void notifyError(@NotNull LessErrorMessage message) {
        notify(message.getNotification(myProject, IMPORTANT_ERROR_NOTIFICATION, NotificationType.ERROR));
    }

    public void notifyError(@NotNull String title, @NotNull String message, @Nullable NotificationListener listener) {
        notify(IMPORTANT_ERROR_NOTIFICATION, title, message, NotificationType.ERROR, listener);
    }

    public void notifySuccess(@NotNull String title, @NotNull String message) {
        notify(NOTIFICATION_GROUP_ID, title, message, NotificationType.INFORMATION,  null);
    }

    public void notifyWeakWarning(@NotNull String title, @NotNull String message, @Nullable NotificationListener listener) {
        notify(MINOR_NOTIFICATION, title, message, NotificationType.WARNING, listener);
    }

    public void notifyStrongWarning(@NotNull String title, @NotNull String content, @Nullable NotificationListener listener) {
        notify(IMPORTANT_ERROR_NOTIFICATION, title, content, NotificationType.WARNING, listener);
    }

    private static class MyNotificationListener implements NotificationListener {

        @NotNull private final Project myProject;

        private MyNotificationListener(@NotNull Project project) {
            myProject = project;
        }

        @Override
        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
            if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (event.getDescription().equals("configure") && !myProject.isDisposed()) {

                }
                else if (event.getDescription().equals("ignore")) {
                    notification.expire();
                }
            }
        }
    }

}
