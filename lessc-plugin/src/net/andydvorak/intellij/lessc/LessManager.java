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

package net.andydvorak.intellij.lessc;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import net.andydvorak.intellij.lessc.file.LessFileWatcherService;
import net.andydvorak.intellij.lessc.file.VirtualFileListenerImpl;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.state.LessProjectState;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.lesscss.LessCompiler;
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

    private final LessProjectState state = new LessProjectState();

    @Transient
    private final Project project;

    @Transient
    private final VirtualFileListenerImpl virtualFileListener;

    public LessManager(final Project project) {
        super(project);
        this.project = project;
        this.virtualFileListener = new VirtualFileListenerImpl(this);
    }
    
    public static LessManager getInstance(final Project project) {
        return project.getComponent(LessManager.class);
    }

    public void initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(virtualFileListener);
    }

    public void disposeComponent() {
        VirtualFileManager.getInstance().removeVirtualFileListener(virtualFileListener);
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

    /** Used to determine if the state has changed, and thus whether IntelliJ needs to write out to the .xml file. */
    public LessProjectState getState() {
        return state;
    }

    /** Import state from external .xml file */
    public void loadState(final LessProjectState state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }

    private boolean isSupported(final VirtualFileEvent virtualFileEvent) {
        return  isLessFile(virtualFileEvent) &&
                isChild(virtualFileEvent);
    }

    private boolean isLessFile(final VirtualFileEvent virtualFileEvent) {
        return virtualFileEvent.getFileName().endsWith(".less");
    }

    private boolean isChild(final VirtualFileEvent virtualFileEvent) {
        return getLessProfile(virtualFileEvent) != null;
    }

    private LessProfile getLessProfile(final VirtualFileEvent virtualFileEvent) {
        for ( LessProfile lessProfile : getProfiles() ) {
            final File lessFile = getLessFile(virtualFileEvent);
            final File lessProfileDir = new File(lessProfile.getLessDirPath());
            if ( FileUtil.isAncestor(lessProfileDir, lessFile, false) ) {
                return lessProfile;
            }
        }
        return null;
    }

    private void compile(final File lessFile, final File cssFile, final LessProfile lessProfile) throws IOException, LessException {
        LOG.info("contentsChanged: " + lessFile.getName());
        LOG.info("\t" + "lessPath: " + lessFile.getCanonicalPath());
        LOG.info("\t" + "cssPath: " + cssFile.getCanonicalPath());

        final LessCompiler lessCompiler = new LessCompiler();

        lessCompiler.setCompress(lessProfile.isCompressOutput());
        lessCompiler.compile(lessFile, cssFile);
    }

    private File getLessFile(final VirtualFileEvent virtualFileEvent) {
        return new File(virtualFileEvent.getFile().getPath());
    }

    private File getCssTempFile(final VirtualFileEvent virtualFileEvent) throws IOException {
        return FileUtil.createTempFile("intellij-lessc-plugin.", ".css", true);
    }

    private void copyCssFile(final File lessFile, final File cssTempFile, final LessProfile lessProfile) throws IOException {
        final String relativeLessPath = FileUtil.getRelativePath(lessProfile.getLessDir(), lessFile.getCanonicalFile());
        final String relativeCssPath = relativeLessPath.replaceFirst("\\.less$", ".css");

        for ( CssDirectory cssDirectory : lessProfile.getCssDirectories() ) {
            final File cssDestFile = new File(cssDirectory.getPath() + File.separator + relativeCssPath);

            FileUtil.createIfDoesntExist(cssDestFile);
            FileUtil.copy(cssTempFile, cssDestFile);

            final VirtualFile virtualCssFile = LocalFileSystem.getInstance().findFileByIoFile(cssDestFile);

            if ( virtualCssFile != null ) {
                // TODO: performance of synchronous vs. asynchronous?
                virtualCssFile.refresh(false, false);
            }
        }

        FileUtil.delete(cssTempFile);
    }

    public void handleFileEvent(final VirtualFileEvent virtualFileEvent) {
        if ( isSupported(virtualFileEvent) ) {
            PsiDocumentManager.getInstance(project).performWhenAllCommitted(new Runnable() {
                @Override
                public void run() {
                    ProgressManager.getInstance().run(new Task.Backgroundable(project, "Compiling " + getLessFile(virtualFileEvent).getName() + " to CSS", false) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            indicator.setFraction(0);

                            try {
                                final LessProfile lessProfile = getLessProfile(virtualFileEvent);

                                if ( ! lessProfile.getCssDirectories().isEmpty() ) {
                                    final File lessFile = getLessFile(virtualFileEvent);
                                    final File cssTempFile = getCssTempFile(virtualFileEvent);

                                    compile(lessFile, cssTempFile, lessProfile);
                                    copyCssFile(lessFile, cssTempFile, lessProfile);

                                    handleSuccess(lessFile, cssTempFile);
                                }
                            } catch (IOException e) {
                                handleException(e, virtualFileEvent);
                            } catch (LessException e) {
                                handleException(e, virtualFileEvent);
                            }

                            indicator.setFraction(1);
                        }
                    });
                }
            });
        }
    }

    public void handleFileEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        // TODO: Implement this w/ intelligent cleanup of CSS file

//        handleFileEvent((VirtualFileEvent) virtualFileMoveEvent);
    }

    public void handleFileEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        // TODO: Implement this

//        handleFileEvent((VirtualFileEvent) virtualFileCopyEvent);
    }

    public void handleDeletedFileEvent(final VirtualFileEvent virtualFileEvent) {
        // TODO: Implement this w/ intelligent cleanup of CSS file
    }

    private void handleException(final Exception e, final VirtualFileEvent virtualFileEvent) {
        showBalloon(virtualFileEvent.getFileName() + ": " + e.getLocalizedMessage(), MessageType.ERROR);
        LOG.warn(e);
    }

    private void handleSuccess(final File lessFile, final File cssFile) {
        final String message = lessFile.getName() + " successfully compiled to CSS";
        showBalloon(message, MessageType.INFO);
        LOG.info(message);
    }

    private void showBalloon(final String message, final MessageType messageType) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

                if ( statusBar != null ) {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(message, messageType, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
                }
            }
        });
    }
}
