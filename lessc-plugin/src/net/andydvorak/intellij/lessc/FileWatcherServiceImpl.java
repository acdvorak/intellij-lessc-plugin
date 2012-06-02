package net.andydvorak.intellij.lessc;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileWatcherServiceImpl implements ApplicationComponent, FileWatcherService {

    // TODO: Get these values dynamically from the current Module or Project config
    private static final String LESS_DIR = "/Users/tkmax82/kiosk/Applications/Kiosk/Kiosk-Web/src/main/less";
    private static final String CSS_DIR = "/Users/tkmax82/kiosk/Applications/Kiosk/Kiosk-Web/src/main/webapp/media/css/ngKiosk";

    private final Map<String, File> fileMap;
    private final LessCompiler lessCompiler;
    private final VirtualFileListenerImpl virtualFileListener;

    public FileWatcherServiceImpl() {
        fileMap = new HashMap<String, File>();
        lessCompiler = new LessCompiler();
        virtualFileListener = new VirtualFileListenerImpl(this);
    }

    private boolean isFileWatchable(final VirtualFileEvent virtualFileEvent) {
        return  virtualFileEvent.getFileName().endsWith(".less") &&
                virtualFileEvent.getFile().getCanonicalPath().startsWith(LESS_DIR);
    }

    private String getLessPath(final VirtualFileEvent virtualFileEvent) {
        return virtualFileEvent.getFile().getCanonicalPath();
    }

    private String getCssPath(final VirtualFileEvent virtualFileEvent) {
        return CSS_DIR + virtualFileEvent.getFile().getCanonicalPath()
                .replaceFirst(LESS_DIR, "")
                .replaceAll("\\.less$", ".css");
    }

    private File getCachedFile(final String path) {
        if ( fileMap.containsKey(path) ) {
            return fileMap.get(path);
        } else {
            final File file = new File(path);
            fileMap.put(path, file);
            return file;
        }
    }

    private void removeCachedFile(final String path) {
        fileMap.remove(path);
    }

    public void handleFileEvent(final VirtualFileEvent virtualFileEvent) {
        if ( isFileWatchable(virtualFileEvent) ) {
            final String lessPath = getLessPath(virtualFileEvent);
            final String cssPath = getCssPath(virtualFileEvent);

            final File lessFile = getCachedFile(lessPath);
            final File cssFile = getCachedFile(cssPath);

            final IndicatorState indicatorState = startProgress(lessFile, cssFile);

            try {
                lessCompiler.compile(lessFile, cssFile);
                handleSuccess(lessFile, cssFile);
            } catch (IOException e) {
                e.printStackTrace();  // TODO: Use proper logging mechanism
                handleException(e, virtualFileEvent);
            } catch (LessException e) {
                e.printStackTrace();  // TODO: Use proper logging mechanism
                handleException(e, virtualFileEvent);
            } finally {
                stopProgress(indicatorState);
            }

            // TODO: Use proper logging mechanism
            System.out.println("contentsChanged: " + virtualFileEvent.getFileName());
            System.out.println("\t" + CSS_DIR);
            System.out.println("\t" + "lessPath: " + lessPath);
            System.out.println("\t" + "cssPath: " + cssPath);
        }
    }

    // TODO: this doesn't seem to do anything
    private IndicatorState startProgress(final File lessFile, final File cssFile) {
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

        IndicatorState indicatorState = null;

        if (indicator != null) {
            // Save indicator's state so we can restore it later
            indicatorState = new IndicatorState(indicator.getText(), indicator.getFraction());

            indicator.setText("Compiling " + lessFile.getName() + " to " + cssFile.getName());
            indicator.setFraction(0);
        }

        return indicatorState;
    }

    private void stopProgress(final IndicatorState indicatorState) {
        final ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();

        // Restore previous indicator state
        if (indicator != null && indicatorState != null) {
            indicator.setText(indicatorState.getText());
            indicator.setFraction(indicatorState.getFraction());
        }
    }

    public void handleFileEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        // TODO: Implement this w/ intelligent cleanup of CSS file

        // TODO: Clean this up
        removeCachedFile(virtualFileMoveEvent.getOldParent().getCanonicalPath() + "/" + virtualFileMoveEvent.getFileName());

//        handleFileEvent((VirtualFileEvent) virtualFileMoveEvent);
    }

    public void handleFileEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        // TODO: Implement this

//        handleFileEvent((VirtualFileEvent) virtualFileCopyEvent);
    }

    public void handleDeletedFileEvent(final VirtualFileEvent virtualFileEvent) {
        // TODO: Implement this w/ intelligent cleanup of CSS file

        removeCachedFile(virtualFileEvent.getFile().getCanonicalPath());
    }

    private void handleException(final Exception e, final VirtualFileEvent virtualFileEvent) {
        showBalloon(virtualFileEvent.getFileName() + ": " + e.getLocalizedMessage(), MessageType.ERROR);
    }

    private void handleSuccess(final File lessFile, final File cssFile) {
        showBalloon(lessFile.getName() + " successfully compiled to CSS", MessageType.INFO);

        final VirtualFile virtualCssFile = LocalFileSystem.getInstance().findFileByIoFile(cssFile);

        // TODO: performance of synchronous vs. asynchronous?
        virtualCssFile.refresh(false, false);
    }

    private void showBalloon(final String message, final MessageType messageType) {
        final DataContext dataContext = DataManager.getInstance().getDataContext();
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(dataContext));

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, messageType, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
    }

    public void initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(virtualFileListener);
    }

    public void disposeComponent() {
        VirtualFileManager.getInstance().removeVirtualFileListener(virtualFileListener);
    }

    @NotNull
    public String getComponentName() {
        return "FileWatcherServiceImpl";
    }
}
