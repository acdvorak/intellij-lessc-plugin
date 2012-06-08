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

import com.intellij.openapi.vfs.*;

public class VirtualFileListenerImpl implements VirtualFileListener {

    final LessFileWatcherService lessFileWatcherService;

    public VirtualFileListenerImpl(final LessFileWatcherService lessFileWatcherService) {
        this.lessFileWatcherService = lessFileWatcherService;
    }

    public void contentsChanged(final VirtualFileEvent virtualFileEvent) {
        lessFileWatcherService.handleFileEvent(virtualFileEvent);
    }

    public void fileCreated(final VirtualFileEvent virtualFileEvent) {
        lessFileWatcherService.handleFileEvent(virtualFileEvent);
    }

    public void fileDeleted(final VirtualFileEvent virtualFileEvent) {
        lessFileWatcherService.handleDeletedFileEvent(virtualFileEvent);
    }

    public void fileMoved(final VirtualFileMoveEvent virtualFileMoveEvent) {
        lessFileWatcherService.handleFileEvent(virtualFileMoveEvent);
    }

    public void fileCopied(final VirtualFileCopyEvent virtualFileCopyEvent) {
        lessFileWatcherService.handleFileEvent(virtualFileCopyEvent);
    }

    public void propertyChanged(final VirtualFilePropertyEvent virtualFilePropertyEvent) {}

    public void beforePropertyChange(final VirtualFilePropertyEvent virtualFilePropertyEvent) {}

    public void beforeContentsChange(final VirtualFileEvent virtualFileEvent) {}

    public void beforeFileDeletion(final VirtualFileEvent virtualFileEvent) {}

    public void beforeFileMovement(final VirtualFileMoveEvent virtualFileMoveEvent) {}

}
