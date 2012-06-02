package net.andydvorak.intellij.lessc;

import com.intellij.openapi.vfs.*;

public class VirtualFileListenerImpl implements VirtualFileListener {

    final FileWatcherService fileWatcherService;

    public VirtualFileListenerImpl(final FileWatcherService fileWatcherService) {
        this.fileWatcherService = fileWatcherService;
    }

    public void contentsChanged(final VirtualFileEvent virtualFileEvent) {
        fileWatcherService.handleFileEvent(virtualFileEvent);
    }

    public void fileCreated(final VirtualFileEvent virtualFileEvent) {
        fileWatcherService.handleFileEvent(virtualFileEvent);
    }

    public void fileDeleted(final VirtualFileEvent virtualFileEvent) {
        fileWatcherService.handleDeletedFileEvent(virtualFileEvent);
    }

    public void fileMoved(final VirtualFileMoveEvent virtualFileMoveEvent) {
        fileWatcherService.handleFileEvent(virtualFileMoveEvent);
    }

    public void fileCopied(final VirtualFileCopyEvent virtualFileCopyEvent) {
        fileWatcherService.handleFileEvent(virtualFileCopyEvent);
    }

    public void propertyChanged(final VirtualFilePropertyEvent virtualFilePropertyEvent) {}

    public void beforePropertyChange(final VirtualFilePropertyEvent virtualFilePropertyEvent) {}

    public void beforeContentsChange(final VirtualFileEvent virtualFileEvent) {}

    public void beforeFileDeletion(final VirtualFileEvent virtualFileEvent) {}

    public void beforeFileMovement(final VirtualFileMoveEvent virtualFileMoveEvent) {}

}
