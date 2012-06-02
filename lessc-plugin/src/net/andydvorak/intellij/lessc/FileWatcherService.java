package net.andydvorak.intellij.lessc;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
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

public class FileWatcherService implements ApplicationComponent {

    // TODO: Put this in <configuration> in plugin.xml
    private static final String LESS_DIR = "/Users/tkmax82/kiosk/Applications/Kiosk/Kiosk-Web/src/main/less";
    private static final String CSS_DIR = "/Users/tkmax82/kiosk/Applications/Kiosk/Kiosk-Web/src/main/webapp/media/css/ngKiosk";

    public FileWatcherService() {
    }

    private boolean isFileWatchable(final VirtualFileEvent virtualFileEvent) {
        return  virtualFileEvent.getFileName().endsWith(".less") &&
                virtualFileEvent.getFile().getCanonicalPath().startsWith(LESS_DIR);
    }

    @NotNull
    private String getCssPath(final VirtualFileEvent virtualFileEvent) {
        return CSS_DIR + virtualFileEvent.getFile().getCanonicalPath().replaceFirst(LESS_DIR, "").replaceAll("\\.less$", ".css");
    }

    private void handleFileChange(final VirtualFileEvent virtualFileEvent) {
        if ( isFileWatchable(virtualFileEvent) ) {
            // Instantiate the LESS compiler
            LessCompiler lessCompiler = new LessCompiler();

            final String lessPath = virtualFileEvent.getFile().getCanonicalPath();
            final String cssPath = getCssPath(virtualFileEvent);

            try {
                // Compile LESS input string to CSS output string
//                        final String css = lessCompiler.compile("@color: #4D926F; #header { color: @color; }");

                // Compile LESS input file to CSS output file
                lessCompiler.compile(new File(lessPath), new File(cssPath));

                handleSuccess(virtualFileEvent);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                handleException(e, virtualFileEvent);

            } catch (LessException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                handleException(e, virtualFileEvent);
            }

            System.out.println("contentsChanged: " + virtualFileEvent.getFileName());
            System.out.println("\t" + CSS_DIR);
            System.out.println("\t" + "lessPath: " + lessPath);
            System.out.println("\t" + "cssPath: " + cssPath);
        }
    }

    private void handleException(final Exception e, final VirtualFileEvent virtualFileEvent) {
        displayPopup(e.getLocalizedMessage(), MessageType.ERROR);
    }

    private void handleSuccess(final VirtualFileEvent virtualFileEvent) {
        displayPopup(virtualFileEvent.getFileName() + " successfully converted to CSS", MessageType.INFO);
    }

    private void displayPopup(final String message, final MessageType messageType) {
        final DataContext dataContext = DataManager.getInstance().getDataContext();
        final Project project = DataKeys.PROJECT.getData(dataContext);

//        final Project currentProject = DataKeys.PROJECT.getData(actionEvent.getDataContext());
//        final VirtualFile currentFile = DataKeys.VIRTUAL_FILE.getData(actionEvent.getDataContext());
//        final Editor editor = DataKeys.EDITOR.getData(actionEvent.getDataContext());

        final StatusBar statusBar = WindowManager.getInstance()
                .getStatusBar(DataKeys.PROJECT.getData(dataContext));

        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(message, messageType, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getCenterOf(statusBar.getComponent()),
                        Balloon.Position.atRight);
    }

    public void initComponent() {
        final VirtualFileManager virtualFileManager = VirtualFileManager.getInstance();

        virtualFileManager.addVirtualFileListener(new VirtualFileListener() {
            public void propertyChanged(final VirtualFilePropertyEvent virtualFilePropertyEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void contentsChanged(final VirtualFileEvent virtualFileEvent) {
                handleFileChange(virtualFileEvent);
            }

            public void fileCreated(final VirtualFileEvent virtualFileEvent) {
                handleFileChange(virtualFileEvent);
            }

            public void fileDeleted(final VirtualFileEvent virtualFileEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void fileMoved(final VirtualFileMoveEvent virtualFileMoveEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void fileCopied(final VirtualFileCopyEvent virtualFileCopyEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void beforePropertyChange(final VirtualFilePropertyEvent virtualFilePropertyEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void beforeContentsChange(final VirtualFileEvent virtualFileEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void beforeFileDeletion(final VirtualFileEvent virtualFileEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void beforeFileMovement(final VirtualFileMoveEvent virtualFileMoveEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "FileWatcherService";
    }
}
