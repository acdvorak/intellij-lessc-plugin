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

package net.andydvorak.intellij.lessc.fs;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.ui.configurable.VfsLocationChangeDialog;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Andrew C. Dvorak
 * @since 11/1/12
 */
@SuppressWarnings("WeakerAccess")
public class VirtualFileLocationChange {
    @NotNull private final CssDirectory cssRootDir;
    @NotNull private final VirtualFile oldFile;
    @NotNull private final VirtualFile newParent;

    private VirtualFileLocationChange(@NotNull final CssDirectory cssRootDir,
                                      @NotNull final VirtualFile oldFile,
                                      @NotNull final VirtualFile newParent) {
        this.cssRootDir = cssRootDir;
        this.oldFile = oldFile;
        this.newParent = newParent;
    }

    public void copy() throws IOException {
        deleteExisting();
        final String name = oldFile.getName();
        oldFile.copy(this, newParent, name);
        refresh();
    }

    public void move() throws IOException {
        deleteExisting();
        oldFile.move(this, newParent);
        refresh();
    }

    public void delete() throws IOException {
        oldFile.delete(this);
        refresh();
    }

    private void deleteExisting() throws IOException {
        final File newFile = new File(newParent.getPath() + File.separator + oldFile.getName());
        if (newFile.exists()) {
            final VirtualFile newVirtualFile = getVirtualFile(newFile);
            if (newVirtualFile != null) {
                newVirtualFile.delete(this);
                refresh();
            }
        }
    }

    public void refresh() {
        refresh(cssRootDir);
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final VirtualFileLocationChange that = (VirtualFileLocationChange) o;

        if (!ObjectUtils.equals(cssRootDir.getPath(), that.cssRootDir.getPath())) return false;
        if (!ObjectUtils.equals(newParent.getPath(), that.newParent.getPath())) return false;
        if (!ObjectUtils.equals(oldFile.getPath(), that.oldFile.getPath())) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = StringUtils.defaultString(cssRootDir.getPath()).hashCode();
        result = 31 * result + StringUtils.defaultString(oldFile.getPath()).hashCode();
        result = 31 * result + StringUtils.defaultString(newParent.getPath()).hashCode();
        return result;
    }

    public static void refresh(@NotNull final Collection<CssDirectory> cssRootDirs) {
        for (final CssDirectory cssRootDir : cssRootDirs)
            refresh(cssRootDir);
    }

    public static void refresh(@Nullable final CssDirectory cssRootDir) {
        if (cssRootDir == null)
            return;

        final VirtualFile virtualCssRootDir = getVirtualFile(cssRootDir, "");

        if (virtualCssRootDir != null) {
            virtualCssRootDir.refresh(true, true);
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(true);
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public static int copyCssFiles(@NotNull final VirtualFileCopyEvent virtualFileCopyEvent,
                                   @Nullable final LessProfile lessProfile,
                                   @NotNull final VfsLocationChangeDialog vfsLocationChangeDialog) throws IOException {
        final Set<VirtualFileLocationChange> changes = getChanges(lessProfile, virtualFileCopyEvent);

        if (changes.isEmpty() || !vfsLocationChangeDialog.shouldCopyCssFile(virtualFileCopyEvent))
            return 0;

        for (final VirtualFileLocationChange locationChange : changes) {
            locationChange.copy();
        }

        return changes.size();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static int moveCssFiles(@NotNull final VirtualFileMoveEvent virtualFileMoveEvent,
                                   @Nullable final LessProfile lessProfile,
                                   @NotNull final VfsLocationChangeDialog vfsLocationChangeDialog) throws IOException {
        final Set<VirtualFileLocationChange> changes = getChanges(lessProfile, virtualFileMoveEvent);

        if (changes.isEmpty() || !vfsLocationChangeDialog.shouldMoveCssFile(virtualFileMoveEvent))
            return 0;

        for (final VirtualFileLocationChange locationChange : changes) {
            locationChange.move();
        }

        return changes.size();
    }

    @SuppressWarnings("UnusedReturnValue")
    public static int deleteCssFiles(@NotNull final VirtualFileEvent virtualFileEvent,
                                     @Nullable final LessProfile lessProfile,
                                     @NotNull final VfsLocationChangeDialog vfsLocationChangeDialog) throws IOException {
        final Set<VirtualFileLocationChange> changes = getChanges(lessProfile, virtualFileEvent.getFile(), virtualFileEvent.getFile().getParent());

        if (changes.isEmpty() || !vfsLocationChangeDialog.shouldDeleteCssFile(virtualFileEvent))
            return 0;

        for (final VirtualFileLocationChange locationChange : changes) {
            locationChange.delete();
        }

        return changes.size();
    }

    @NotNull
    public static Set<VirtualFileLocationChange> getChanges(@Nullable final LessProfile lessProfile,
                                                            @NotNull final VirtualFileMoveEvent moveEvent) {
        return getChanges(lessProfile, moveEvent.getFile(), moveEvent.getOldParent());
    }

    @NotNull
    public static Set<VirtualFileLocationChange> getChanges(@Nullable final LessProfile lessProfile,
                                                            @NotNull final VirtualFileCopyEvent copyEvent) {
        return getChanges(lessProfile, copyEvent.getFile(), copyEvent.getOriginalFile().getParent());
    }

    /**
     * NOTE: This method will create the parent directories for new CSS files if they don't already exist.
     */
    @NotNull
    public static Set<VirtualFileLocationChange> getChanges(@Nullable final LessProfile lessProfile,
                                                            @NotNull final VirtualFile newVirtualLessFile,
                                                            @NotNull final VirtualFile oldVirtualLessParent) {
        final Set<VirtualFileLocationChange> changes = new HashSet<VirtualFileLocationChange>();

        if (lessProfile == null)
            return changes;

        // TODO: Make sure new (and old?) directories are in the LESS profile dir
        final File lessRootDir = lessProfile.getLessDir();

        final File newLessFile = new File(newVirtualLessFile.getPath());
        final String newRelativeCssPath = toCssPath(FileUtil.getRelativePath(lessRootDir, newLessFile));

        if (newRelativeCssPath == null)
            return changes;

        final File oldLessFile = new File(oldVirtualLessParent.getPath() + File.separator + newLessFile.getName());
        final String oldRelativeCssPath = toCssPath(FileUtil.getRelativePath(lessRootDir, oldLessFile));

        if (oldRelativeCssPath == null)
            return changes;

        for(final CssDirectory cssRootDir : lessProfile.getCssDirectories()) {
            final VirtualFile oldVirtualCssFile = getVirtualFile(cssRootDir, oldRelativeCssPath);

            if (oldVirtualCssFile == null)
                continue;

            final File newCssFile = getCssFile(cssRootDir, newRelativeCssPath);

            FileUtil.createParentDirs(newCssFile);

            final VirtualFile newVirtualCssFileParent = getVirtualFile(newCssFile.getParentFile());

            if (newVirtualCssFileParent == null)
                continue;

            changes.add(new VirtualFileLocationChange(cssRootDir, oldVirtualCssFile, newVirtualCssFileParent));
        }

        return changes;
    }

    @Nullable
    private static String toCssPath(final String lessPath) {
        return lessPath == null ? null : lessPath.replaceFirst("\\.less$", ".css");
    }

    @NotNull
    private static String getAbsolutePath(@NotNull final CssDirectory cssRootDir, @NotNull final String relativeCssPath) {
        return cssRootDir.getPath() + File.separator + relativeCssPath;
    }

    @NotNull
    private static File getCssFile(@NotNull final CssDirectory cssRootDir, @NotNull final String relativeCssPath) {
        return new File(getAbsolutePath(cssRootDir, relativeCssPath));
    }

    @Nullable
    private static VirtualFile getVirtualFile(@NotNull final CssDirectory cssRootDir, @NotNull final String relativeCssPath) {
        return getVirtualFile(getCssFile(cssRootDir, relativeCssPath));
    }

    @Nullable
    private static VirtualFile getVirtualFile(@NotNull final File file) {
        return LocalFileSystem.getInstance().findFileByIoFile(file);
    }
}
