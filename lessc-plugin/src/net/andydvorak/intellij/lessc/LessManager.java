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
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import net.andydvorak.intellij.lessc.file.*;
import net.andydvorak.intellij.lessc.messages.NotificationsBundle;
import net.andydvorak.intellij.lessc.notification.FileNotificationListener;
import net.andydvorak.intellij.lessc.notification.LessErrorMessage;
import net.andydvorak.intellij.lessc.notification.Notifier;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.state.LessProjectState;
import net.andydvorak.intellij.lessc.ui.FileLocationChangeDialog;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

@State(
        name = "LessManager",
        storages = {
                @Storage(id = "default", file = "$PROJECT_FILE$"),
                @Storage(id = "dir", file = "$PROJECT_CONFIG_DIR$/lessc/lessc.xml", scheme = StorageScheme.DIRECTORY_BASED)
        }
)
public class LessManager extends AbstractProjectComponent implements PersistentStateComponent<LessProjectState>, LessFileWatcherService {

    private static final Logger LOG = Logger.getInstance("#" + LessManager.class.getName());
    private static final String IGNORE_LINK = "(<a href='ignore'>ignore</a>)";
    private static final String DISMISS_LINK = "(<a href='dismiss'>dismiss</a>)";

    @NotNull private final LessProjectState state = new LessProjectState();
    @NotNull private final FileLocationChangeDialog fileLocationChangeDialog;

    @Transient
    private final VirtualFileListenerImpl virtualFileListener;

    private final ConcurrentMap<String, LessCompileJob> lastCompileTimes = new ConcurrentHashMap<String, LessCompileJob>();
    private final ConcurrentLinkedQueue<String> compileQueue = new ConcurrentLinkedQueue<String>();
    private final ConcurrentMap<String, LessCompileJob> compileQueueJobs = new ConcurrentHashMap<String, LessCompileJob>();

    private final Notifier notifier;

    public LessManager(final Project project) {
        super(project);
        this.virtualFileListener = new VirtualFileListenerImpl(this);
        this.fileLocationChangeDialog = new FileLocationChangeDialog(project, state);
        this.notifier = Notifier.getInstance(project);
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
    private static LessFile getLessFile(final VirtualFileEvent virtualFileEvent) {
        return new LessFile(virtualFileEvent.getFile().getPath());
    }

    @Nullable
    public LessProfile getLessProfile(final VirtualFileEvent virtualFileEvent) {
        return getLessFile(virtualFileEvent).getLessProfile(getProfiles());
    }

    private void saveAllDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    public void handleChangeEvent(final VirtualFileEvent virtualFileEvent) {
        if (isSupported(virtualFileEvent)) {
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    saveAllDocuments();
                }
            });
            ApplicationManager.getApplication().invokeLater(new Runnable() {
                @Override
                public void run() {
                    compile(virtualFileEvent);
                }
            });
        }
    }

    private void compile(final VirtualFileEvent virtualFileEvent) {
        compile(new LessCompileJob(getLessFile(virtualFileEvent), getLessProfile(virtualFileEvent)), true);
    }

    private void compile(final LessCompileJob compileJob, final boolean async) {
        // Abort if the job is queued
        if (enqueue(compileJob, async)) return;

        final LessFile lessFile = compileJob.getSourceLessFile();
        final String title = NotificationsBundle.message("compiling.single", lessFile.getName());

        PsiDocumentManager.getInstance(myProject).performWhenAllCommitted(new Runnable() {
            @Override
            public void run() {
                ProgressManager.getInstance().run(new Task.Backgroundable(myProject, title, false) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        compileWithProgress(this, indicator, compileJob);
                    }
                });
            }
        });
    }

    private synchronized void compileWithProgress(@NotNull final Task.Backgroundable task,
                                                  @NotNull final ProgressIndicator indicator,
                                                  @NotNull final LessCompileJob compileJob) {
        indicator.setFraction(0);

        compileJob.addObserver(new LessCompileObserverImpl(compileJob, task, indicator));

        try {
            compileJob.compile();
            handleSuccess(compileJob);
        } catch (Exception e) {
            final LessFile curLessFile = compileJob.getCurLessFile();
            handleException(e, curLessFile.getCanonicalPathSafe(), curLessFile.getName());
        } finally {
            indicator.setFraction(1);
            dequeue();
            refreshVFS();
        }
    }

    /**
     * Enqueues a compile job if it is running asynchronously and needs to wait for other compile jobs to finish.
     * @param compileJob job to enqueue
     * @param async {@code true} to run the job asynchronously at a later time if necessary
     *              (determined by the IntelliJ's Event Dispatch Queue);
     *              {@code false} to run it immediately
     * @return {@code true} if the compile job was enqueued to wait for other jobs to finish; otherwise {@code false}
     */
    private boolean enqueue(LessCompileJob compileJob, boolean async) {
        final LessFile lessFile = compileJob.getSourceLessFile();
        final String lessFilePath = lessFile.getCanonicalPathSafe();

        synchronized (lastCompileTimes) {
            // If less than 250 milliseconds (1/4 second) have elapsed since this file was last compiled, queue it up
            if (async && needsToWait(lessFilePath)) {
                // TODO: Create separate ConcurrentLinkedHashMap class?
                synchronized (compileQueue) {
                    compileQueue.add(lessFilePath);
                    compileQueueJobs.put(lessFilePath, compileJob);
                }
                return true;
            }
            lastCompileTimes.put(lessFilePath, compileJob);
        }

        return false;
    }

    /**
     * Run queued compile jobs in the Event Dispatch Thread after all other events have finished.
     */
    private void dequeue() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                final List<LessCompileJob> jobs = getQueuedCompileJobsConcurrent();
                for (LessCompileJob job : jobs) {
                    compile(job, false);
                }
            }
        });
    }

    private void refreshVFS() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                for (LessProfile lessProfile : getProfiles()) {
                    VFSLocationChange.refresh(lessProfile.getCssDirectories());
                }
            }
        });
    }

    /**
     * Determines if the given LESS file has been compiled too recently and needs to wait before recompiling.
     * @param lessFilePath path to the LESS file to be compiled
     * @return {@code true} if the given LESS file has been compiled too recently and needs to wait before recompiling;
     *         otherwise {@code false}
     */
    private boolean needsToWait(final String lessFilePath) {
        return lastCompileTimes.containsKey(lessFilePath) && !lastCompileTimes.get(lessFilePath).canCreateNewCompileJob();
    }

    /**
     * Returns a list of queued compile jobs in FIFO order (thread-safe).
     * @return queued compile jobs in FIFO order
     */
    private List<LessCompileJob> getQueuedCompileJobsConcurrent() {
        final List<LessCompileJob> jobs = new ArrayList<LessCompileJob>();

        // Extract compile jobs atomically, maintaining FIFO order
        synchronized (compileQueue) {
            final LinkedHashSet<String> paths = new LinkedHashSet<String>(compileQueue);

            for (String lessFilePath : paths) {
                jobs.add(compileQueueJobs.get(lessFilePath));
            }

            compileQueue.clear();
            compileQueueJobs.clear();
        }

        return jobs;
    }

    // TODO: This is a bit quirky and doesn't seem to work if the new CSS directory hasn't been created yet and its parent dir isn't open in the project view
    public void handleMoveEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        if (isSupported(virtualFileMoveEvent)) {
            final LessProfile lessProfile = getLessProfile(virtualFileMoveEvent);
            try {
                VFSLocationChange.moveCssFiles(virtualFileMoveEvent, lessProfile, fileLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleCopyEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        if (isSupported(virtualFileCopyEvent)) {
            final LessProfile lessProfile = getLessProfile(virtualFileCopyEvent);
            try {
                VFSLocationChange.copyCssFiles(virtualFileCopyEvent, lessProfile, fileLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleDeleteEvent(final VirtualFileEvent virtualFileEvent) {
        if (isSupported(virtualFileEvent)) {
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
        notifier.error(message);
        LOG.warn(e);
    }

    private void handleSuccess(final LessCompileJob lessCompileJob) {
        for (LessFile lessFile : lessCompileJob.getSourceAndDependents()) {
            notifier.expire(lessFile.getCanonicalPathSafe());
        }

        final int numModified = lessCompileJob.getNumUpdated();

        if (numModified == 0) {
            notifyNone(lessCompileJob.getSourceLessFile());
        } else if (numModified == 1) {
            notifySingle(lessCompileJob.getUpdatedLessFiles().iterator().next());
        } else {
            notifyMultiple(lessCompileJob.getUpdatedLessFiles());
        }
    }

    private void notifyNone(final LessFile lessFile) {
        final String filename = lessFile.getName();
        final String messageText = NotificationsBundle.message("compiled.unchanged", filename);
        final String messageHtml = NotificationsBundle.message("compiled.unchanged", createLink(lessFile)) + " " + IGNORE_LINK;
        final FileNotificationListener listener = new FileNotificationListener(myProject, lessFile.getCanonicalPathSafe());
        final HashSet<LessFile> modifiedLessFiles = new HashSet<LessFile>(Arrays.asList(lessFile));

        notifier.log(messageHtml, listener, modifiedLessFiles);

        LOG.info(messageText);
    }

    private void notifySingle(final LessFile lessFile) {
        final String filename = lessFile.getName();

        final String messageText = NotificationsBundle.message("compiled.changed.single", filename);
        final String messagePartHtml = NotificationsBundle.message("compiled.changed.single", createLink(lessFile));
        final String logMessageHtml = messagePartHtml + " " + IGNORE_LINK;
        final String successMessageHtml = messagePartHtml + " " + DISMISS_LINK;

        final FileNotificationListener listener = new FileNotificationListener(myProject, lessFile.getCanonicalPathSafe());
        final HashSet<LessFile> modifiedLessFiles = new HashSet<LessFile>(Arrays.asList(lessFile));

        notifier.log(logMessageHtml, listener, modifiedLessFiles);
        notifier.success(successMessageHtml, listener, modifiedLessFiles);

        LOG.info(messageText);
    }

    private void notifyMultiple(final Set<LessFile> modifiedLessFiles) {
        final String messageShortText = NotificationsBundle.message("compiled.changed.multiple", modifiedLessFiles.size());
        final StringBuilder messageFullText = new StringBuilder(messageShortText + ":");
        final StringBuilder messageFilesHtml = new StringBuilder(messageShortText + " " + IGNORE_LINK + ":");
        final Iterator<LessFile> iterator = modifiedLessFiles.iterator();
        messageFilesHtml.append(" [ ");
        while (iterator.hasNext()) {
            final LessFile lessFile = iterator.next();
            messageFullText.append('\n');
            messageFullText.append(String.format("\t%s", lessFile.getCanonicalPathSafe()));
            messageFilesHtml.append(String.format("%s%s", createLink(lessFile), iterator.hasNext() ? ", " : ""));
        }
        messageFilesHtml.append(" ] " + IGNORE_LINK);

        final FileNotificationListener listener = new FileNotificationListener(myProject);

        notifier.log(messageFilesHtml.toString(), listener, new HashSet<LessFile>());
        notifier.success(messageShortText + " " + DISMISS_LINK, listener, modifiedLessFiles);

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
