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

import com.asual.lesscss.LessException;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
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
import net.andydvorak.intellij.lessc.file.*;
import net.andydvorak.intellij.lessc.notification.FileNotificationListener;
import net.andydvorak.intellij.lessc.notification.LessErrorMessage;
import net.andydvorak.intellij.lessc.notification.Notifier;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.state.LessProjectState;
import net.andydvorak.intellij.lessc.ui.FileLocationChangeDialog;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

@State(
        name = "LessManager",
        storages = {
                @Storage(id = "default", file = "$PROJECT_FILE$"),
                @Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/lessc/lessc.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public class LessManager extends AbstractProjectComponent implements PersistentStateComponent<LessProjectState>, LessFileWatcherService {

    private static final Logger LOG = Logger.getInstance("#" + LessManager.class.getName());
    private static final String NOTIFICATION_TITLE = "LESS CSS Compiler";
    private static final String IGNORE_LINK = "(<a href='ignore'>ignore</a>)";

    @NotNull private final LessProjectState state = new LessProjectState();
    @NotNull private final FileLocationChangeDialog fileLocationChangeDialog;

    @Transient
    private final VirtualFileListenerImpl virtualFileListener;

    public LessManager(final Project project) {
        super(project);
        this.virtualFileListener = new VirtualFileListenerImpl(this);
        this.fileLocationChangeDialog = new FileLocationChangeDialog(project, state);
    }
    
    public static LessManager getInstance(final Project project) {
        return project.getComponent(LessManager.class);
    }

    public void initComponent() {
        VirtualFileManager.getInstance().addVirtualFileListener(virtualFileListener);

        // TODO: See http://confluence.jetbrains.net/display/IDEADEV/IntelliJ+IDEA+Virtual+File+System
        // "This API gives you all the changes detected during the refresh operation in one list, and lets you process them in batch."
//        BulkFileListener
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
    @NotNull
    public LessProjectState getState() {
        return state;
    }

    /** Import state from external .xml file */
    public void loadState(final LessProjectState state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }

    private boolean isSupported(final VirtualFileEvent virtualFileEvent) {
        return  LessFile.isLessFile(virtualFileEvent.getFileName()) &&
                getLessProfile(virtualFileEvent) != null;
    }

    @NotNull
    private LessFile getLessFile(final VirtualFileEvent virtualFileEvent) {
        return new LessFile(virtualFileEvent.getFile().getPath());
    }

    @Nullable
    private LessProfile getLessProfile(final VirtualFileEvent virtualFileEvent) {
        return getLessFile(virtualFileEvent).getLessProfile(getProfiles());
    }

    // TODO: Refactor/combine compile() methods
    private LessCompileJob compile(final VirtualFileEvent virtualFileEvent) throws IOException, LessException {
        final LessFile lessFile = getLessFile(virtualFileEvent);
        final LessProfile lessProfile = getLessProfile(virtualFileEvent);
        final LessCompileJob lessCompileJob = new LessCompileJob(lessFile, lessProfile);

        if (lessProfile != null && lessProfile.hasCssDirectories()) {
            compile(lessCompileJob);
        }

        return lessCompileJob;
    }

    // TODO: Refactor/combine compile() methods
    private void compile(final LessCompileJob lessCompileJob) throws IOException, LessException {
        if (lessCompileJob.getLessFile().shouldCompile(lessCompileJob.getLessProfile()))
            lessCompileJob.compile();

        if ( updateCssFiles(lessCompileJob) ) {
            lessCompileJob.addModifiedLessFile(lessCompileJob.getLessFile());
            LOG.info("Successfully compiled " + lessCompileJob.getLessFile().getCanonicalPath() + " to CSS");
        }

        compileDependents(lessCompileJob);

        lessCompileJob.refreshVFS();
    }

    /**
     *
     * @param lessCompileJob
     * @return true if the CSS output file has changed and is non-empty; otherwise false
     * @throws IOException
     */
    private boolean updateCssFiles(final LessCompileJob lessCompileJob) throws IOException {
        if ( lessCompileJob.getCssTempFile().length() == 0 ) {
            FileUtil.delete(lessCompileJob.getCssTempFile());
            return false;
        }

        final File lessProfileDir = lessCompileJob.getLessProfile().getLessDir();
        final File lessFile = lessCompileJob.getLessFile().getCanonicalFile();

        final String relativeLessPath = StringUtils.defaultString(FileUtil.getRelativePath(lessProfileDir, lessFile));
        final String relativeCssPath = relativeLessPath.replaceFirst("\\.less$", ".css");

        final String cssTempFileContent = FileUtil.loadFile(lessCompileJob.getCssTempFile());

        int numUpdated = 0;

        for ( CssDirectory cssDirectory : lessCompileJob.getLessProfile().getCssDirectories() ) {
            final File cssDestFile = new File(cssDirectory.getPath(), relativeCssPath);

            // CSS file hasn't changed, so don't bother updating
            if ( cssDestFile.exists() && FileUtil.loadFile(cssDestFile).equals(cssTempFileContent) )
                continue;

            numUpdated++;

            FileUtil.createIfDoesntExist(cssDestFile);
            FileUtil.copy(lessCompileJob.getCssTempFile(), cssDestFile);

            final VirtualFile virtualCssFile = LocalFileSystem.getInstance().findFileByIoFile(cssDestFile);

            if ( virtualCssFile != null ) {
                // TODO: performance of synchronous vs. asynchronous?
                virtualCssFile.getParent().refresh(true, false);
                virtualCssFile.getParent().getParent().refresh(true, false);
            }

            lessCompileJob.refreshVFS();
        }

        FileUtil.delete(lessCompileJob.getCssTempFile());

        return numUpdated > 0;
    }

    private void compileDependents(final LessCompileJob lessCompileJob) throws IOException {
        final Set<String> dependentPaths = LessFile.getDependentPaths(lessCompileJob.getLessFile(), lessCompileJob.getLessProfile());

        for ( String dependentPath : dependentPaths ) {
            final LessFile dependentLessFile = new LessFile(dependentPath);
            final LessCompileJob dependentCompileJob = new LessCompileJob(lessCompileJob, dependentLessFile);

            String lessFilePath = "UNKNOWN INPUT FILE PATH";
            String lessFileName = "UNKNOWN INPUT FILE NAME";

            try {
                lessFilePath = dependentLessFile.getCanonicalPath();
                lessFileName = dependentLessFile.getName();

                compile(dependentCompileJob);

                for(LessFile curModifiedLessFile : dependentCompileJob.getModifiedLessFiles()) {
                    lessCompileJob.addModifiedLessFile(curModifiedLessFile);
                }
            } catch (Exception e) {
                handleException(e, lessFilePath, lessFileName);
            }
        }
    }

    private boolean areUnsavedDocuments() {
        return !ArrayUtils.isEmpty(FileDocumentManager.getInstance().getUnsavedDocuments());
    }

    private void saveAllDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    private void waitForSave() {
        if (areUnsavedDocuments())
            saveAllDocuments();

        // Wait for all files to be saved
        while (areUnsavedDocuments()) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {

            }
        }
    }

    public void handleChangeEvent(final VirtualFileEvent virtualFileEvent) {
        if ( isSupported(virtualFileEvent) ) {
            waitForSave();

            final String title = "Compiling " + getLessFile(virtualFileEvent).getName() + " to CSS";

            PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(new Runnable() {
                @Override
                public void run() {
                    ProgressManager.getInstance().run(new Task.Backgroundable(myProject, title, false) {
                        @Override
                        public void run(@NotNull ProgressIndicator indicator) {
                            // TODO: Update progress title when multiple files are being compiled (e.g., "Compiling file 1 of 3...")
//                            this.setTitle()
                            compileWithProgress(indicator, virtualFileEvent);
                        }
                    });
                }
            });
        }
    }

    private void compileWithProgress(@NotNull final ProgressIndicator indicator, @NotNull final VirtualFileEvent virtualFileEvent) {
        indicator.setFraction(0);

        String lessFilePath = "UNKNOWN INPUT FILE PATH";
        String lessFileName = "UNKNOWN INPUT FILE NAME";

        try {
            final LessFile lessFile = getLessFile(virtualFileEvent);

            lessFilePath = lessFile.getCanonicalPath();
            lessFileName = lessFile.getName();

            final LessCompileJob lessCompileJob = compile(virtualFileEvent);

            handleSuccess(lessCompileJob);
        } catch (Exception e) {
            handleException(e, lessFilePath, lessFileName);
        }

        indicator.setFraction(1);
    }

    public void handleMoveEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        if ( isSupported(virtualFileMoveEvent) ) {
            final LessProfile lessProfile = getLessProfile(virtualFileMoveEvent);
            try {
                VFSLocationChange.moveCssFiles(virtualFileMoveEvent, lessProfile, fileLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleCopyEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        if ( isSupported(virtualFileCopyEvent) ) {
            final LessProfile lessProfile = getLessProfile(virtualFileCopyEvent);
            try {
                VFSLocationChange.copyCssFiles(virtualFileCopyEvent, lessProfile, fileLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleDeleteEvent(final VirtualFileEvent virtualFileEvent) {
        if ( isSupported(virtualFileEvent) ) {
            final LessProfile lessProfile = getLessProfile(virtualFileEvent);
            try {
                VFSLocationChange.deleteCssFiles(virtualFileEvent, lessProfile, fileLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    private void handleException(final @NotNull Exception e, @NotNull final String lessFilePath, @NotNull final String lessFileName) {
        final LessErrorMessage message = new LessErrorMessage(lessFilePath, lessFileName, e);
        Notifier.getInstance(myProject).notifyError(message);
        LOG.warn(e);
    }

    private void handleSuccess(final LessCompileJob lessCompileJob) {
        final int numModified = lessCompileJob.getNumModified();

        if (numModified == 0) {
            notifyNone(lessCompileJob.getLessFile());
        } else if (numModified == 1) {
            notifySingle(lessCompileJob.getModifiedLessFiles().iterator().next());
        } else {
            notifyMultiple(lessCompileJob.getModifiedLessFiles());
        }
    }

    private void notifyNone(final LessFile lessFile) {
        final Notifier notifier = Notifier.getInstance(myProject);
        final String filename = lessFile.getName();
        final String messagePart = "was modified, but didn't change any CSS files";
        final String messageText = filename + " " + messagePart;
        final String messageHtml = createLink(lessFile) + " " + messagePart + " " + IGNORE_LINK;
        final FileNotificationListener listener = new FileNotificationListener(myProject, lessFile.getCanonicalPathSafe());
        notifier.logInfo(NOTIFICATION_TITLE, messageHtml, listener);
        LOG.info(messageText);
    }

    private void notifySingle(final LessFile lessFile) {
        final Notifier notifier = Notifier.getInstance(myProject);
        final String filename = lessFile.getName();
        final String messagePart = "successfully compiled to CSS";
        final String messageText = filename + " " + messagePart;
        final String messageHtml = createLink(lessFile) + " " + messagePart + " " + IGNORE_LINK;
        final FileNotificationListener listener = new FileNotificationListener(myProject, lessFile.getCanonicalPathSafe());
        notifier.notifySuccessBalloon(NOTIFICATION_TITLE, messageHtml, listener);
        LOG.info(messageText);
    }

    private void notifyMultiple(final Set<LessFile> modifiedLessFiles) {
        final Notifier notifier = Notifier.getInstance(myProject);
        final String messageShortText = modifiedLessFiles.size() + " " + "LESS files successfully compiled to CSS";
        final StringBuilder messageFullText = new StringBuilder(messageShortText + ":");
        final StringBuilder messageFilesHtml = new StringBuilder(messageShortText + " " + IGNORE_LINK + ":");
        for (LessFile lessFile : modifiedLessFiles) {
            messageFullText.append('\n');
            messageFullText.append(String.format("\t%s %s", lessFile.getCanonicalPathSafe(), "successfully compiled to CSS"));
            messageFilesHtml.append(String.format("<p>%s %s</p>", createLink(lessFile), "successfully compiled to CSS"));
        }
        final FileNotificationListener listener = new FileNotificationListener(myProject);
        notifier.logInfo(NOTIFICATION_TITLE, messageFilesHtml.toString(), listener);
        notifier.notifySuccessBalloon(NOTIFICATION_TITLE, messageShortText + " " + IGNORE_LINK, listener);
        LOG.info(messageFullText.toString());
    }

    private String createLink(final LessFile lessFile) {
        return String.format("<a href='%s'>%s</a>", lessFile.getCanonicalPathSafeHtmlEscaped(), lessFile.getName());
    }

    private void showBalloon(final String message, final MessageType messageType) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);

                if ( statusBar != null ) {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder(message, messageType, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
                }

//                EventLog.getEventLog(project).
            }
        });
    }
}
