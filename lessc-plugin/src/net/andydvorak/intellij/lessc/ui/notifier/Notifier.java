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
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager;
import com.intellij.util.containers.ConcurrentMultiMap;
import net.andydvorak.intellij.lessc.fs.LessFile;
import net.andydvorak.intellij.lessc.ui.messages.NotificationsBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * @author Andrew C. Dvorak
 * @since 10/25/12
 */
public class Notifier {

    /**
     * <p>Adds an entry to the "Event Log" tool window.</p>
     * <p>Does <strong>NOT</strong> display a notification balloon.</p>
     */
    public static final NotificationGroup LOG_GROUP = NotificationGroup.logOnlyGroup(
            "LESS Compiler Notices");

    /**
     * <p>Displays a notification balloon above the "Changes" tool window button.</p>
     * <p>Does <strong>NOT</strong> create an entry in the "Event Log" tool window.</p>
     */
    public static final NotificationGroup SUCCESS_GROUP = NotificationGroup.toolWindowGroup(
            "LESS Compiler Successful Compiles", ChangesViewContentManager.TOOLWINDOW_ID, false);

    /**
     * <p>Displays a sticky notification balloon in the global notification area.</p>
     * <p>Creates an entry in the "Event Log" tool window.</p>
     */
    public static final NotificationGroup ERROR_GROUP = new NotificationGroup(
            "LESS Compiler Error Messages", NotificationDisplayType.STICKY_BALLOON, true);

    private static final String LOG_TITLE = NotificationsBundle.message("log.title");
    private static final String BALLOON_TITLE = NotificationsBundle.message("balloon.title");

    private static final Key<Notifier> userDataKey = new Key<>("LessNotifier");

    public static synchronized Notifier getInstance(@NotNull final Project project) {
        Notifier instance = project.getUserData(userDataKey);
        if (instance == null) {
            instance = new Notifier(project);
            project.putUserData(userDataKey, instance);
        }
        return instance;
    }

    private final Project myProject;
    private final ConcurrentMultiMap<String, Notification> notifications = new ConcurrentMultiMap<>();

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

    /*
     * Generic
     */

    public void notify(@NotNull final Notification notification) {
        notification.notify(myProject);
    }

    public void add(@NotNull final String lessFilePath, @NotNull final Notification notification) {
        synchronized (notifications) {
            notifications.putValue(lessFilePath, notification);
        }
    }

    public void expire(@NotNull final String lessFilePath) {
        synchronized (notifications) {
            if (notifications.containsKey(lessFilePath)) {
                for (Notification notification : notifications.get(lessFilePath)) {
                    notification.expire();
                }
                notifications.remove(lessFilePath);
            }
        }
    }

    /*
     * Log Only
     */

    public void log(@NotNull final String message, @Nullable final NotificationListener listener,
                    @NotNull final Set<LessFile> modifiedLessFiles) {
        final Notification notification = createNotification(LOG_GROUP, LOG_TITLE, message, NotificationType.INFORMATION, listener);

        for (LessFile lessFile : modifiedLessFiles) {
            add(lessFile.getCanonicalPathSafe(), notification);
        }

        notify(notification);
    }

    /*
     * Success
     */

    public void success(@NotNull final String message, @Nullable final NotificationListener listener,
                        @NotNull final Set<LessFile> modifiedLessFiles) {
        final Notification notification = createNotification(SUCCESS_GROUP, BALLOON_TITLE, message, NotificationType.INFORMATION, listener);

        for (LessFile lessFile : modifiedLessFiles) {
            add(lessFile.getCanonicalPathSafe(), notification);
        }

        notify(notification);
    }

    /*
     * Error
     */

    public void error(@NotNull final LessErrorMessage m) {
        final NotificationListener listener = new NotificationListenerImpl(myProject, m.getFilePath(), m.getLine(), m.getColumn());
        final Notification notification = createNotification(ERROR_GROUP, m.getTitle(), m.getHtml(), NotificationType.ERROR, listener);

        expire(m.getFilePath());
        add(m.getFilePath(), notification);
        notify(notification);
    }

}
