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

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.ui.messages.NotificationsBundle;
import net.andydvorak.intellij.lessc.ui.notifier.NotificationListenerImpl;
import net.andydvorak.intellij.lessc.ui.notifier.Notifier;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Andrew C. Dvorak
 * @since 10/26/12
 */
public class LessCompileAction extends AnAction {

    private static final Logger LOG = Logger.getInstance("#" + LessManager.class.getName());

    public void actionPerformed(final AnActionEvent _e) {
        final AnActionEventWrapper e = new AnActionEventWrapper(_e);
        final Project project = e.getProject();

        if (project == null) return;

        final Collection<VirtualFile> lessFiles = e.getLessFiles();
        final LessCompileEvent compileEvent = new LessCompileEvent(project);
        compileEvent.processFiles(lessFiles);
    }

    /**
     * Show/hide and enable/disable the "Compile to CSS" menu item in the context menu when right-clicking
     * on a file in the Project file list or Editor window.
     * @see <a href="http://devnet.jetbrains.net/message/5126605#5126605">JetBrains forum post</a>
     */
    public void update(final AnActionEvent _e) {
        super.update(_e);

        final AnActionEventWrapper e = new AnActionEventWrapper(_e);

        final boolean visible = e.hasProject() && e.hasLessFiles();

        // Visibility
        e.getPresentation().setVisible(visible);

        // Enable or disable
        e.getPresentation().setEnabled(visible);
    }

    private static class LessCompileEvent {
        private final LessManager manager;
        private final Notifier notifier;
        private final NotificationListener listener;
        private final List<VirtualFile> filesWithNoProfile = new ArrayList<VirtualFile>();

        private LessCompileEvent(@NotNull final Project project) {
            this.manager = LessManager.getInstance(project);
            this.notifier = Notifier.getInstance(project);
            this.listener = new NotificationListenerImpl(project);
        }

        public void processFiles(final Collection<VirtualFile> files) {
            for (final VirtualFile file : files) {
                final VirtualFileEvent virtualFileEvent = new VirtualFileEvent(this, file, file.getName(), file.getParent());
                final List<LessProfile> profiles = manager.getLessProfiles(virtualFileEvent);

                if (!profiles.isEmpty()) {
                    // TODO: Gather all files in a list, then generate their import trees and compile unique ones in batch
                    manager.handleManualEvent(virtualFileEvent);
                } else {
                    filesWithNoProfile.add(file);
                }
            }

            checkForMissingProfiles();
        }

        private void checkForMissingProfiles() {
            if (filesWithNoProfile.isEmpty())
                return;

            if (filesWithNoProfile.size() == 1) {
                warnMissingProfile();
            } else {
                warnMissingProfiles();
            }
        }

        private void warnMissingProfile() {
            final VirtualFile file = filesWithNoProfile.get(0);
            final String title = NotificationsBundle.message("action.missing.profile.single.title");
            final String text = NotificationsBundle.message("action.missing.profile.single.text", file.getCanonicalPath());
            final String html = NotificationsBundle.message("action.missing.profile.single.html", String.format("<a href='file:%s'>%s</a>", file.getCanonicalPath(), file.getName()));

            warn(title, text, html);
        }

        private void warnMissingProfiles() {
            final List<String> textLines = new ArrayList<String>();
            final List<String> htmlLines = new ArrayList<String>();

            for (final VirtualFile file : filesWithNoProfile) {
                textLines.add(file.getCanonicalPath());
                htmlLines.add(String.format("<a href='file:%s'>%s</a>", file.getCanonicalPath(), file.getName()));
            }

            final String title = NotificationsBundle.message("action.missing.profile.multiple.title");
            final String text = NotificationsBundle.message("action.missing.profile.multiple.text", StringUtils.join(textLines, "\n"));
            final String html = NotificationsBundle.message("action.missing.profile.multiple.html", StringUtils.join(htmlLines, "\n"));

            warn(title, text, html);
        }

        private void warn(final String title, final String text, final String html) {
            LOG.warn(text);
            notifier.warn(title, html, listener);
        }
    }

    private static class AnActionEventWrapper extends AnActionEvent {
        public AnActionEventWrapper(final AnActionEvent e) {
            super(e.getInputEvent(), e.getDataContext(), e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
        }

        public VirtualFile[] getVirtualFiles() {
            return getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        }

        public boolean hasProject() {
            final Project project = getProject();
            return project != null && !project.isDisposed();
        }

        public boolean hasLessFiles() {
            return hasLessFilesRecursive(getVirtualFiles());
        }

        public Collection<VirtualFile> getLessFiles() {
            return getLessFiles(getVirtualFiles());
        }

        public static boolean hasLessFilesRecursive(final VirtualFile[] files) {
            final AtomicBoolean hasLessFiles = new AtomicBoolean();

            // Search subdirectories recursively
            for (final VirtualFile file : files) {
                VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Object>() {
                    @Override
                    public boolean visitFile(@NotNull final VirtualFile file) {
                        hasLessFiles.compareAndSet(false, isLessFile(file));
                        return true;
                    }
                });
            }

            return hasLessFiles.get();
        }

        private static boolean isLessFile(final VirtualFile file) {
            return "LESS".equals(file.getFileType().getName()) ||
                   "LESS".equals(StringUtils.upperCase(file.getExtension()));
        }

        /**
         * Traverses an array of VirtualFiles recursively and returns all LESS files in the array and subdirectories
         * of folders in the array.
         * @param files array of VirtualFiles (files and/or folders) selected by the user which may or may not contain LESS files
         * @return Collection of LESS files
         */
        public static Collection<VirtualFile> getLessFiles(final VirtualFile[] files) {
            final ArrayList<VirtualFile> lessFiles = new ArrayList<VirtualFile>();

            if (ArrayUtils.isEmpty(files))
                return lessFiles;

            for (final VirtualFile file : files) {
                if (isLessFile(file))
                    lessFiles.add(file);

                lessFiles.addAll(getLessFiles(file.getChildren()));
            }

            return lessFiles;
        }
    }

}
