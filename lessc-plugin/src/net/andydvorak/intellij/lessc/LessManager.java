package net.andydvorak.intellij.lessc;

import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import net.andydvorak.intellij.lessc.file.LessFileUtils;
import net.andydvorak.intellij.lessc.file.LessFileWatcherService;
import net.andydvorak.intellij.lessc.file.VirtualFileListenerImpl;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.state.LessProjectState;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.lesscss.LessException;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@State(
        name = "LessManager",
        storages = {
                @Storage(id = "default", file = "$PROJECT_FILE$"),
                @Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/lessc/lessc.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public class LessManager extends AbstractProjectComponent implements PersistentStateComponent<LessProjectState>, LessFileWatcherService {

    private static final Logger LOG = Logger.getInstance("#" + LessManager.class.getName());

    @Transient
    private final Project project;

    private final LessProjectState state;

    @Transient
    private final VirtualFileListenerImpl virtualFileListener;

    public LessManager(final Project project) {
        super(project);
        this.project = project;
        this.state = new LessProjectState();
        this.virtualFileListener = new VirtualFileListenerImpl(this);
    }
    
    public static LessManager getInstance(final Project project) {
        return project.getComponent(LessManager.class);
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return "LessManager";
    }

    public void addProfile(final LessProfile lessProfile) {
        state.lessProfiles.put(lessProfile.getName(), lessProfile);
    }

    public void removeProfile(final LessProfile lessProfile) {
        state.lessProfiles.remove(lessProfile.getName());
    }

    public void replaceProfile(final String displayName, final LessProfile lessProfile) {
        state.lessProfiles.put(displayName, lessProfile);
    }

    public void clearProfiles() {
        state.lessProfiles.clear();
    }

    public Collection<LessProfile> getProfiles() {
        return state.lessProfiles.values();
    }

    public LessProjectState getState() {
        return state;
    }

    public void loadState(final LessProjectState state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }

    public void handleFileEvent(final VirtualFileEvent virtualFileEvent) {
        final LessFileUtils lessFileUtils = LessFileUtils.getInstance();

        if ( lessFileUtils.isSupported(virtualFileEvent) ) {
            final File lessFile = lessFileUtils.getLessFile(virtualFileEvent);
            final File cssFile = lessFileUtils.getLessFile(virtualFileEvent);

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
        }
    }

    public void handleFileEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        final LessFileUtils lessFileUtils = LessFileUtils.getInstance();

        // TODO: Implement this w/ intelligent cleanup of CSS file

        lessFileUtils.removeCachedFile(lessFileUtils.getNewPath(virtualFileMoveEvent));

//        handleFileEvent((VirtualFileEvent) virtualFileMoveEvent);
    }

    public void handleFileEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        // TODO: Implement this

//        handleFileEvent((VirtualFileEvent) virtualFileCopyEvent);
    }

    public void handleDeletedFileEvent(final VirtualFileEvent virtualFileEvent) {
        final LessFileUtils lessFileUtils = LessFileUtils.getInstance();

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
        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        if ( statusBar != null ) {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(message, messageType, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
        }
    }

}
