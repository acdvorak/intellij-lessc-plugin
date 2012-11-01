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

package net.andydvorak.intellij.lessc.file;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.ui.FileLocationChangeDialog;
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
public class VFSLocationChange {
    @NotNull private final CssDirectory cssRootDir;
    @NotNull private final VirtualFile oldFile;
    @NotNull private final VirtualFile newParent;

    public VFSLocationChange(@NotNull final CssDirectory cssRootDir,
                             @NotNull final VirtualFile oldFile,
                             @NotNull final VirtualFile newParent) {
        this.cssRootDir = cssRootDir;
        this.oldFile = oldFile;
        this.newParent = newParent;
    }

    public void copy() throws IOException {
        final String name = oldFile.getName();
        oldFile.copy(this, newParent, name);
//        FileUtil.copy(new File(oldFile.getPath()), new File(newParent.getPath() + File.separator + oldFile.getName()));
        refresh();
    }

    public void move() throws IOException {
        oldFile.move(this, newParent);
        refresh();
    }

    public void refresh() {
        refresh(cssRootDir);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VFSLocationChange that = (VFSLocationChange) o;

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
        for (CssDirectory cssRootDir : cssRootDirs)
            refresh(cssRootDir);
    }

    public static void refresh(@Nullable final CssDirectory cssRootDir) {
        if (cssRootDir == null)
            return;

        final VirtualFile virtualCssRootDir = getVirtualFile(cssRootDir, "");

        if (virtualCssRootDir != null) {
            virtualCssRootDir.refresh(true, true);
            VirtualFileManager.getInstance().refresh(true);
        }
    }

    public static int copyCssFiles(@NotNull final VirtualFileCopyEvent virtualFileCopyEvent,
                                   @Nullable final LessProfile lessProfile,
                                   @NotNull final FileLocationChangeDialog fileLocationChangeDialog) throws IOException {
        final Set<VFSLocationChange> changes = getChanges(lessProfile, virtualFileCopyEvent);

        if (changes.isEmpty() || !fileLocationChangeDialog.shouldCopyCssFile(virtualFileCopyEvent))
            return 0;

        for (VFSLocationChange vfsLocationChange : changes) {
            vfsLocationChange.copy();
        }

        return changes.size();
    }

    public static int moveCssFiles(@NotNull final VirtualFileMoveEvent virtualFileMoveEvent,
                                   @Nullable final LessProfile lessProfile,
                                   @NotNull final FileLocationChangeDialog fileLocationChangeDialog) throws IOException {
        final Set<VFSLocationChange> changes = getChanges(lessProfile, virtualFileMoveEvent);

        if (changes.isEmpty() || !fileLocationChangeDialog.shouldMoveCssFile(virtualFileMoveEvent))
            return 0;

        for (VFSLocationChange vfsLocationChange : changes) {
            vfsLocationChange.move();
        }

        return changes.size();
    }

    @NotNull
    public static Set<VFSLocationChange> getChanges(@Nullable final LessProfile lessProfile,
                                                    @NotNull final VirtualFileMoveEvent moveEvent) throws IOException {
        return getChanges(lessProfile, moveEvent.getFile(), moveEvent.getOldParent());
    }

    @NotNull
    public static Set<VFSLocationChange> getChanges(@Nullable final LessProfile lessProfile,
                                                    @NotNull final VirtualFileCopyEvent copyEvent) throws IOException {
        return getChanges(lessProfile, copyEvent.getFile(), copyEvent.getOriginalFile().getParent());
    }

    /**
     * NOTE: This method will create the parent directories for new CSS files if they don't already exist.
     * @param lessProfile
     * @param newVirtualLessFile
     * @param oldVirtualLessParent
     * @return
     * @throws IOException
     */
    @NotNull
    public static Set<VFSLocationChange> getChanges(@Nullable final LessProfile lessProfile,
                                                    @NotNull final VirtualFile newVirtualLessFile,
                                                    @NotNull final VirtualFile oldVirtualLessParent) throws IOException {
        final Set<VFSLocationChange> movableFiles = new HashSet<VFSLocationChange>();

        if (lessProfile == null)
            return movableFiles;

        // TODO: Make sure new (and old?) directories are in the LESS profile dir
        final File lessRootDir = lessProfile.getLessDir();

        final File newLessFile = new File(newVirtualLessFile.getPath());
        final String newRelativeCssPath = toCssPath(FileUtil.getRelativePath(lessRootDir, newLessFile));

        if (newRelativeCssPath == null)
            return movableFiles;

        final File oldLessFile = new File(oldVirtualLessParent.getPath() + File.separator + newLessFile.getName());
        final String oldRelativeCssPath = toCssPath(FileUtil.getRelativePath(lessRootDir, oldLessFile));

        if (oldRelativeCssPath == null)
            return movableFiles;

        for(CssDirectory cssRootDir : lessProfile.getCssDirectories()) {
            final VirtualFile oldVirtualCssFile = getVirtualFile(cssRootDir, oldRelativeCssPath);

            if (oldVirtualCssFile == null)
                continue;

            final File newCssFile = getCssFile(cssRootDir, newRelativeCssPath);

            FileUtil.createParentDirs(newCssFile);

            final VirtualFile newVirtualCssFileParent = getVirtualFile(newCssFile.getParentFile());

            if (newVirtualCssFileParent == null)
                continue;

            movableFiles.add(new VFSLocationChange(cssRootDir, oldVirtualCssFile, newVirtualCssFileParent));
        }

        return movableFiles;
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
