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
import com.intellij.openapi.vfs.VirtualFileCopyEvent;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.containers.ConcurrentHashMap;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Transient;
import net.andydvorak.intellij.lessc.fs.*;
import net.andydvorak.intellij.lessc.ui.messages.NotificationsBundle;
import net.andydvorak.intellij.lessc.ui.notifier.NotificationListenerImpl;
import net.andydvorak.intellij.lessc.ui.notifier.LessErrorMessage;
import net.andydvorak.intellij.lessc.ui.notifier.Notifier;
import net.andydvorak.intellij.lessc.observer.CompileObserverImpl;
import net.andydvorak.intellij.lessc.state.LessProfile;
import net.andydvorak.intellij.lessc.state.LessProjectState;
import net.andydvorak.intellij.lessc.ui.configurable.VfsLocationChangeDialog;
import org.apache.commons.lang3.ObjectUtils;
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
public class LessManager extends AbstractProjectComponent implements PersistentStateComponent<LessProjectState>, VirtualFileWatcher {

    private static final Logger LOG = Logger.getInstance("#" + LessManager.class.getName());
    private static final String IGNORE_LINK = "(<a href='ignore'>ignore</a>)";
    private static final String DISMISS_LINK = "(<a href='dismiss'>dismiss</a>)";

    @NotNull private final LessProjectState state = new LessProjectState();
    @NotNull private final VfsLocationChangeDialog vfsLocationChangeDialog;

    @Transient
    private final VirtualFileListenerImpl virtualFileListener;

    private final ConcurrentMap<String, LessCompileJob> lastCompileTimes = new ConcurrentHashMap<String, LessCompileJob>();
    private final ConcurrentLinkedQueue<String> compileQueue = new ConcurrentLinkedQueue<String>();
    private final ConcurrentMap<String, LessCompileJob> compileQueueJobs = new ConcurrentHashMap<String, LessCompileJob>();

    private final Notifier notifier;

    public LessManager(final Project project) {
        super(project);
        this.virtualFileListener = new VirtualFileListenerImpl(this);
        this.vfsLocationChangeDialog = new VfsLocationChangeDialog(project, state);
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
            logChangeEvent(virtualFileEvent);
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
        if (enqueue(compileJob, async)) {
            logQueuedJob(compileJob, async);
            return;
        }

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

        compileJob.addObserver(new CompileObserverImpl(compileJob, task, indicator));

        final long startTime = System.currentTimeMillis();

        try {
            compileJob.compile();
            handleSuccess(compileJob, startTime);
        } catch (Exception e) {
            final LessFile curLessFile = compileJob.getCurLessFile();
            handleException(e, curLessFile.getCanonicalPathSafe(), curLessFile.getName(), startTime);
        } finally {
            indicator.setFraction(1);
            dequeue();
            refreshCssDirs();
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

    private void refreshCssDirs() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                for (LessProfile lessProfile : getProfiles()) {
                    VirtualFileLocationChange.refresh(lessProfile.getCssDirectories());
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
                VirtualFileLocationChange.moveCssFiles(virtualFileMoveEvent, lessProfile, vfsLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleCopyEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        if (isSupported(virtualFileCopyEvent)) {
            final LessProfile lessProfile = getLessProfile(virtualFileCopyEvent);
            try {
                VirtualFileLocationChange.copyCssFiles(virtualFileCopyEvent, lessProfile, vfsLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleDeleteEvent(final VirtualFileEvent virtualFileEvent) {
        if (isSupported(virtualFileEvent)) {
            final LessProfile lessProfile = getLessProfile(virtualFileEvent);
            try {
                VirtualFileLocationChange.deleteCssFiles(virtualFileEvent, lessProfile, vfsLocationChangeDialog);
            } catch (IOException e) {
                LOG.warn(e);
            }
        }
    }

    private void handleException(@NotNull final Exception e, @NotNull final String lessFilePath,
                                 @NotNull final String lessFileName, final long startTime) {
        final double runTime = getRunTime(startTime);
        notifier.error(new LessErrorMessage(lessFilePath, lessFileName, e));
        LOG.warn(String.format("Compile failed with an exception in %3.2f seconds:", runTime), e);
    }

    private void handleSuccess(final LessCompileJob lessCompileJob, final long startTime) {
        final double runTime = getRunTime(startTime);

        LOG.info(String.format("Compile succeeded in %3.2f seconds", runTime));

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
        final NotificationListenerImpl listener = new NotificationListenerImpl(myProject, lessFile.getCanonicalPathSafe());
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

        final NotificationListenerImpl listener = new NotificationListenerImpl(myProject, lessFile.getCanonicalPathSafe());
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

        final NotificationListenerImpl listener = new NotificationListenerImpl(myProject);

        notifier.log(messageFilesHtml.toString(), listener, new HashSet<LessFile>());
        notifier.success(messageShortText + " " + DISMISS_LINK, listener, modifiedLessFiles);

        LOG.info(messageFullText.toString());
    }

    private String createLink(final LessFile lessFile) {
        return String.format("<a href='%s'>%s</a>", lessFile.getCanonicalPathSafeHtmlEscaped(), lessFile.getName());
    }

    private double getRunTime(long startTime) {
        return (double)(System.currentTimeMillis() - startTime) / 1000d;
    }

    private void logChangeEvent(VirtualFileEvent virtualFileEvent) {
        LOG.info("LessManager.handleChangeEvent(virtualFileEvent)" + "\n" +
                 "\t virtualFileEvent.getFile() = " + virtualFileEvent.getFile().getPath() + "\n" +
                 "\t virtualFileEvent.isFromSave() = " + virtualFileEvent.isFromSave() + "\n" +
                 "\t virtualFileEvent.isFromRefresh() = " + virtualFileEvent.isFromRefresh() + "\n" +
                 "\t virtualFileEvent.getRequestor() = " + toStringLoggable(virtualFileEvent.getRequestor()));
    }

    private void logQueuedJob(LessCompileJob compileJob, boolean async) {
        final String asyncStr = async ? "asynchronous" : "synchronous";
        final String lessPath = compileJob.getSourceLessFile().getCanonicalPathSafe();
        LOG.info(String.format("Queued %s compile job for %s", asyncStr, lessPath));
    }

    /**
     * Returns a log-friendly string representation of an object that includes the fully-qualified
     * class name and the value of the object's {@code toString()} if the class overrides it.
     * @param obj object to represent as a string
     * @return log-friendly representation of the object
     */
    private static String toStringLoggable(Object obj) {
        final String identity = ObjectUtils.identityToString(obj);
        final String override = ObjectUtils.toString(obj, null);
        return ObjectUtils.equals(identity, override) ? identity : identity + " - " + override;
    }
}
