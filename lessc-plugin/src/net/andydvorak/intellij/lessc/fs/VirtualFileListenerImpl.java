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

import com.intellij.openapi.vfs.*;

public class VirtualFileListenerImpl implements VirtualFileListener {

    private final VirtualFileWatcher fileWatcher;

    public VirtualFileListenerImpl(final VirtualFileWatcher fileWatcher) {
        this.fileWatcher = fileWatcher;
    }

    public void contentsChanged(final VirtualFileEvent virtualFileEvent) {
        fileWatcher.handleChangeEvent(virtualFileEvent);
    }

    public void fileCreated(final VirtualFileEvent virtualFileEvent) {
        fileWatcher.handleChangeEvent(virtualFileEvent);
    }

    public void fileDeleted(final VirtualFileEvent virtualFileEvent) {
        fileWatcher.handleDeleteEvent(virtualFileEvent);
    }

    public void fileMoved(final VirtualFileMoveEvent virtualFileMoveEvent) {
        fileWatcher.handleMoveEvent(virtualFileMoveEvent);
    }

    public void fileCopied(final VirtualFileCopyEvent virtualFileCopyEvent) {
        fileWatcher.handleCopyEvent(virtualFileCopyEvent);
    }

    public void propertyChanged(final VirtualFilePropertyEvent virtualFilePropertyEvent) {}

    public void beforePropertyChange(final VirtualFilePropertyEvent virtualFilePropertyEvent) {}

    public void beforeContentsChange(final VirtualFileEvent virtualFileEvent) {}

    public void beforeFileDeletion(final VirtualFileEvent virtualFileEvent) {}

    public void beforeFileMovement(final VirtualFileMoveEvent virtualFileMoveEvent) {}

}
