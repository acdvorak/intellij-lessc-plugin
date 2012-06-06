package net.andydvorak.intellij.lessc.file;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.lesscss.LessException;

import java.io.File;
import java.io.IOException;

public class LessFileWatcherServiceImpl implements ProjectComponent, LessFileWatcherService {

    private final Project project;
    private final LessFileUtils lessFileUtils;
    private final VirtualFileListenerImpl virtualFileListener;

    public LessFileWatcherServiceImpl(final Project project) {
        this.project = project;
        this.lessFileUtils = new LessFileUtils();
        this.virtualFileListener = new VirtualFileListenerImpl(this);
    }

    public void initComponent() {
//        VirtualFileManager.getInstance().addVirtualFileListener(virtualFileListener);
    }

    public void disposeComponent() {
//        VirtualFileManager.getInstance().removeVirtualFileListener(virtualFileListener);
    }

    @NotNull
    public String getComponentName() {
        return "LessFileWatcherServiceImpl";
    }

    @Override
    public void projectOpened() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void projectClosed() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void handleFileEvent(final VirtualFileEvent virtualFileEvent) {
        if ( lessFileUtils.isFileWatchable(virtualFileEvent) ) {
            final File lessFile = lessFileUtils.getLessFile(virtualFileEvent);
            final File cssFile = lessFileUtils.getLessFile(virtualFileEvent);

//            Document document = FileDocumentManager.getInstance().getDocument(virtualFileEvent.getFile());
//            document.

//            final Project project = Project.

//            RunBackgroundable.run(new Task() {

                    try {
                        lessFileUtils.compile(lessFile, cssFile);
                        handleSuccess(lessFile, cssFile);
                    } catch (IOException e) {
                        e.printStackTrace();  // TODO: Use proper logging mechanism
                        handleException(e, virtualFileEvent);
                    } catch (LessException e) {
                        e.printStackTrace();  // TODO: Use proper logging mechanism
                        handleException(e, virtualFileEvent);
                    } finally {

                    }
//            });
        }
    }

    public void handleFileEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        // TODO: Implement this w/ intelligent cleanup of CSS file

        lessFileUtils.removeCachedFile(lessFileUtils.getNewPath(virtualFileMoveEvent));

//        handleFileEvent((VirtualFileEvent) virtualFileMoveEvent);
    }

    public void handleFileEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        // TODO: Implement this

//        handleFileEvent((VirtualFileEvent) virtualFileCopyEvent);
    }

    public void handleDeletedFileEvent(final VirtualFileEvent virtualFileEvent) {
        // TODO: Implement this w/ intelligent cleanup of CSS file

        lessFileUtils.removeCachedFile(virtualFileEvent.getFile().getCanonicalPath());
    }

    private void handleException(final Exception e, final VirtualFileEvent virtualFileEvent) {
        showBalloon(virtualFileEvent.getFileName() + ": " + e.getLocalizedMessage(), MessageType.ERROR);
    }

    private void handleSuccess(final File lessFile, final File cssFile) {
        showBalloon(lessFile.getName() + " successfully compiled to CSS", MessageType.INFO);

        final VirtualFile virtualCssFile = LocalFileSystem.getInstance().findFileByIoFile(cssFile);

        if ( virtualCssFile != null ) {
            // TODO: performance of synchronous vs. asynchronous?
            virtualCssFile.refresh(false, false);
        }
    }

    private void showBalloon(final String message, final MessageType messageType) {
        final DataContext dataContext = DataManager.getInstance().getDataContext();
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(dataContext));

        if ( statusBar != null ) {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(message, messageType, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
        }
    }
}
