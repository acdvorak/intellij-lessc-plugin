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
