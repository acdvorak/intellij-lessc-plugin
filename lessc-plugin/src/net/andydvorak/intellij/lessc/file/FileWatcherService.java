package net.andydvorak.intellij.lessc.file;

import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;

public interface FileWatcherService {

    void handleFileEvent(final VirtualFileEvent virtualFileEvent);
    void handleFileEvent(final VirtualFileMoveEvent virtualFileMoveEvent);
    void handleFileEvent(final VirtualFileCopyEvent virtualFileCopyEvent);
    void handleDeletedFileEvent(final VirtualFileEvent virtualFileEvent);

}
