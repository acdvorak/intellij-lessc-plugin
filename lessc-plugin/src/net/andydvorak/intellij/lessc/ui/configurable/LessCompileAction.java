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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.ui.messages.UIBundle;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Andrew C. Dvorak
 * @since 10/26/12
 */
public class LessCompileAction extends AnAction {

    public void actionPerformed(AnActionEvent _e) {
        final AnActionEventWrapper e = new AnActionEventWrapper(_e);
        final Collection<VirtualFile> files = e.getLessFiles();
        final LessManager lessManager = LessManager.getInstance(e.getProject());

        int numMissing = 0;

        for (VirtualFile file : files) {
            final VirtualFileEvent virtualFileEvent = new VirtualFileEvent(this, file, file.getName(), file.getParent());
            final LessProfile lessProfile = lessManager.getLessProfile(virtualFileEvent);

            if (lessProfile != null && lessProfile.hasCssDirectories()) {
                lessManager.handleManualEvent(virtualFileEvent);
            } else {
                numMissing++;
            }
        }

        if (numMissing > 0) {
            final String title, message;

            if (numMissing == 1) {
                title = UIBundle.message("action.missing.css.dir.single.title");
                message = UIBundle.message("action.missing.css.dir.single.message");
            } else {
                title = UIBundle.message("action.missing.css.dir.multiple.title");
                message = UIBundle.message("action.missing.css.dir.multiple.message", numMissing, files.size());
            }

            Messages.showInfoMessage(e.getProject(), UIBundle.message("action.missing.css.dir.add.message", message), title);
        }
    }

    /**
     * @see <a href="http://devnet.jetbrains.net/message/5126605#5126605">JetBrains forum post</a>
     */
    public void update(AnActionEvent _e) {
        super.update(_e);

        final AnActionEventWrapper e = new AnActionEventWrapper(_e);

        boolean visible = e.hasProject() && e.hasLessFiles();

        // Visibility
        e.getPresentation().setVisible(visible);

        // Enable or disable
        e.getPresentation().setEnabled(visible);
    }

    private static class AnActionEventWrapper extends AnActionEvent {
        public AnActionEventWrapper(final AnActionEvent e) {
            super(e.getInputEvent(), e.getDataContext(), e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
        }

        public VirtualFile getVirtualFile() {
            return getData(PlatformDataKeys.VIRTUAL_FILE);
        }

        public VirtualFile[] getVirtualFiles() {
            return getData(PlatformDataKeys.VIRTUAL_FILE_ARRAY);
        }

        public boolean hasProject() {
            final Project project = getProject();
            return project != null && !project.isDisposed();
        }

        public boolean hasLessFiles() {
            return hasLessFiles(getVirtualFiles());
        }

        public Collection<VirtualFile> getLessFiles() {
            return getLessFiles(getVirtualFiles());
        }

        public static boolean hasLessFiles(final VirtualFile[] files) {
            if (files == null || files.length == 0) return false;

            for (VirtualFile file : files) {
                if (isLessFile(file))
                    return true;
            }

            // Search subdirectories recursively
            for (VirtualFile file : files) {
                if (hasLessFiles(file.getChildren()))
                    return true;
            }

            return false;
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

            for (VirtualFile file : files) {
                if (isLessFile(file))
                    lessFiles.add(file);

                lessFiles.addAll(getLessFiles(file.getChildren()));
            }

            return lessFiles;
        }
    }

}
