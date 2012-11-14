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
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import com.intellij.util.containers.ConcurrentMultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Andrew C. Dvorak
 * @since 10/25/12
 */
public class Notifier {

    /**
     * <p>Adds an entry to the "Event Log" tool window.</p>
     * <p>Does <strong>NOT</strong> display a notification balloon.</p>
     */
    public static final NotificationGroup LOG_ONLY = NotificationGroup.logOnlyGroup(
            "LESS Compiler Notices");

    /**
     * <p>Displays a notification balloon above the "Changes" tool window button.</p>
     * <p>Does <strong>NOT</strong> create an entry in the "Event Log" tool window.</p>
     */
    public static final NotificationGroup CHANGES_TOOLWINDOW_BALLOON = NotificationGroup.toolWindowGroup(
            "LESS Compiler Successful Compiles", ChangesViewContentManager.TOOLWINDOW_ID, false);

    /**
     * <p>Displays a sticky notification balloon in the global notification area.</p>
     * <p>Creates an entry in the "Event Log" tool window.</p>
     */
    public static final NotificationGroup ERROR_NOTIFICATION = new NotificationGroup(
            "LESS Compiler Error Messages", NotificationDisplayType.STICKY_BALLOON, true);

    private static final String NOTIFICATION_TITLE = "LESS CSS Compiler";

    private static final Key<Notifier> userDataKey = new Key<Notifier>("LessNotifier");

    public static synchronized Notifier getInstance(@NotNull final Project project) {
        Notifier instance = project.getUserData(userDataKey);
        if (instance == null) {
            instance = new Notifier(project);
            project.putUserData(userDataKey, instance);
        }
        return instance;
    }

    private final Project myProject;
    private final ConcurrentLinkedQueue<Notification> successes = new ConcurrentLinkedQueue<Notification>();
    private final ConcurrentMultiMap<String, Notification> errors = new ConcurrentMultiMap<String, Notification>();

    public Notifier(@NotNull final Project project) {
        myProject = project;
    }

    @NotNull
    public static Notification createNotification(@NotNull NotificationGroup notificationGroup,
                                                  @NotNull String title, @NotNull String message,
                                                  @NotNull NotificationType type, @Nullable NotificationListener listener) {
        // title can be empty; description can't be null or empty
        if (StringUtil.isEmptyOrSpaces(message)) {
            message = title;
            title = "";
        }

        // if both title and description were empty, then it is a problem in the calling code => Notifications engine assertion will notify.
        return notificationGroup.createNotification(title, message, type, listener);
    }

    @NotNull
    private static Notification createNotification(@NotNull NotificationGroup notificationGroup, @NotNull String message,
                                                   @NotNull NotificationType type, @Nullable NotificationListener listener) {
        return createNotification(notificationGroup, NOTIFICATION_TITLE, message, type, listener);
    }

    /*
     * Generic
     */

    public void notify(@NotNull final Notification notification) {
        notification.notify(myProject);
    }

    public void notify(@NotNull final NotificationGroup notificationGroup, @NotNull final String message,
                       @NotNull final NotificationType type, @Nullable final NotificationListener listener) {
        notify(createNotification(notificationGroup, message, type, listener));
    }

    /*
     * Log Only
     */

    public void log(@NotNull final String message, @Nullable final NotificationListener listener) {
        // TODO: Expire error messages for all unchanged LESS files
        notify(LOG_ONLY, message, NotificationType.INFORMATION, listener);
    }

    /*
     * Error
     */

    private void addError(@NotNull final String lessFilePath, @NotNull final Notification notification) {
        errors.putValue(lessFilePath, notification);
    }

    private void expireErrors(@NotNull final String lessFilePath) {
        if (errors.containsKey(lessFilePath)) {
            for (Notification notification : errors.get(lessFilePath)) {
                notification.hideBalloon();
            }
            errors.remove(lessFilePath);
        }
    }

    public void notifyError(@NotNull final LessErrorMessage m) {
        final NotificationListener listener = new FileNotificationListener(myProject, m.getFilePath(), m.getLine(), m.getColumn());
        final Notification notification = createNotification(ERROR_NOTIFICATION, m.getTitle(), m.getHtml(), NotificationType.ERROR, listener);

        synchronized (errors) {
            expireErrors(m.getFilePath());
            addError(m.getFilePath(), notification);
            notify(notification);
        }
    }

    /*
     * Success
     */

    private void addSuccess(@NotNull final Notification notification) {
        successes.add(notification);
    }

    private void expireSuccesses() {
        for (Notification notification : successes) {
            notification.hideBalloon();
        }
        successes.clear();
    }

    public void notifySuccessBalloon(@NotNull final String message, @Nullable final NotificationListener listener) {
        final Notification notification = createNotification(CHANGES_TOOLWINDOW_BALLOON, message, NotificationType.INFORMATION, listener);

        // TODO: Expire error messages for all successfully compiled LESS files
        synchronized (successes) {
            expireSuccesses();
            addSuccess(notification);
        }

        notify(notification);
    }

}
