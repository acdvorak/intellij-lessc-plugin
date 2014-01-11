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
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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

    @SuppressWarnings("WeakerAccess")
    public LessManager(final Project project) {
        super(project);
        this.virtualFileListener = new VirtualFileListenerImpl(this);
        this.vfsLocationChangeDialog = new VfsLocationChangeDialog(state);
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

    /*
     * State management
     */

    public void putProfile(final int id, final LessProfile newLessProfile) {
        state.lessProfileMap.put(id, newLessProfile);
    }

    public void removeProfile(final int id) {
        state.lessProfileMap.remove(id);
    }

    public Map<Integer, LessProfile> getProfileMap() {
        return new LinkedHashMap<Integer, LessProfile>(state.lessProfileMap);
    }

    public Collection<LessProfile> getProfiles() {
        return state.lessProfileMap.values();
    }

    /** Used to determine if the state has changed, and thus whether IntelliJ needs to write out to the .xml file. */
    @NotNull
    public LessProjectState getState() {
        return state;
    }

    /** Import state from external .xml file */
    public void loadState(final LessProjectState state) {
        XmlSerializerUtil.copyBean(state, this.state);
        migrateConfig();
        checkProfiles();
    }

    private void migrateConfig() {
        if (this.state.lessProfiles.isEmpty())
            return;

        LOG.info("Migrating old profile config to new format");

        int nextProfileId = getNextProfileId();

        // Migrate old config
        for (final LessProfile profile : this.state.lessProfiles.values()) {
            profile.setId(nextProfileId++);
            this.state.lessProfileMap.put(profile.getId(), profile);
        }

        // TODO: Re-save settings file

        this.state.lessProfiles.clear();
    }

    private void checkProfiles() {
        for (final LessProfile profile : state.lessProfileMap.values()) {
            if (StringUtils.isBlank(profile.getLessDirPath())) {
                final String title = NotificationsBundle.message("profile.missing.less.dir.title");
                final String text = NotificationsBundle.message("profile.missing.less.dir.text", profile.getName());
                final String html = NotificationsBundle.message("profile.missing.less.dir.html", profile.getName());
                warn(title, text, html);
            }
            if (profile.getCssDirectories().isEmpty()) {
                final String title = NotificationsBundle.message("profile.missing.css.dirs.title");
                final String text = NotificationsBundle.message("profile.missing.css.dirs.text", profile.getName());
                final String html = NotificationsBundle.message("profile.missing.css.dirs.html", profile.getName());
                warn(title, text, html);
            }
        }
    }

    private int getNextProfileId() {
        int id = -1;
        for (final LessProfile profile : state.lessProfileMap.values()) {
            if (profile.getId() > id) {
                id = profile.getId();
            }
        }
        return id + 1;
    }

    /*
     * File system events
     */

    private boolean isSupported(final VirtualFileEvent virtualFileEvent, final boolean isManual) {
        if (!LessFile.isLessFile(virtualFileEvent.getFileName())) {
            return false;
        }

        final List<LessProfile> lessProfiles = getLessProfiles(virtualFileEvent);

        for (final LessProfile profile : lessProfiles) {
            if (profile.isCompileAutomatically() || isManual) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private static LessFile getLessFile(final VirtualFileEvent virtualFileEvent) {
        return new LessFile(virtualFileEvent.getFile().getPath());
    }

    @NotNull
    public List<LessProfile> getLessProfiles(final VirtualFileEvent virtualFileEvent) {
        return getLessFile(virtualFileEvent).getLessProfiles(getProfiles());
    }

    @Nullable
    public LessProfile getFirstLessProfile(final VirtualFileEvent virtualFileEvent) {
        final List<LessProfile> profiles = getLessProfiles(virtualFileEvent);
        if (profiles.isEmpty()) {
            return null;
        } else {
            return profiles.get(0);
        }
    }

    private void saveAllDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    public void handleChangeEvent(final VirtualFileEvent virtualFileEvent) {
        handleEvent(virtualFileEvent, false);
    }

    public void handleManualEvent(final VirtualFileEvent virtualFileEvent) {
        handleEvent(virtualFileEvent, true);
    }

    private void handleEvent(final VirtualFileEvent virtualFileEvent, final boolean isManual) {
        if (isSupported(virtualFileEvent, isManual)) {
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

    // TODO: This is a bit quirky and doesn't seem to work if the new CSS directory hasn't been created yet and its parent dir isn't open in the project view
    public void handleMoveEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        if (isSupported(virtualFileMoveEvent, false)) {
            final LessProfile lessProfile = getFirstLessProfile(virtualFileMoveEvent);
            if (lessProfile == null) {
                return;
            }
            try {
                VirtualFileLocationChange.moveCssFiles(virtualFileMoveEvent, lessProfile, vfsLocationChangeDialog);
            } catch (final IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleCopyEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        if (isSupported(virtualFileCopyEvent, false)) {
            final LessProfile lessProfile = getFirstLessProfile(virtualFileCopyEvent);
            if (lessProfile == null) {
                return;
            }
            try {
                VirtualFileLocationChange.copyCssFiles(virtualFileCopyEvent, lessProfile, vfsLocationChangeDialog);
            } catch (final IOException e) {
                LOG.warn(e);
            }
        }
    }

    public void handleDeleteEvent(final VirtualFileEvent virtualFileEvent) {
        if (isSupported(virtualFileEvent, false)) {
            final LessProfile lessProfile = getFirstLessProfile(virtualFileEvent);
            if (lessProfile == null) {
                return;
            }
            try {
                VirtualFileLocationChange.deleteCssFiles(virtualFileEvent, lessProfile, vfsLocationChangeDialog);
            } catch (final IOException e) {
                LOG.warn(e);
            }
        }
    }

    /*
     * Compiling
     */

    private void compile(final VirtualFileEvent virtualFileEvent) {
        final List<LessProfile> profiles = getLessProfiles(virtualFileEvent);
        for (final LessProfile profile : profiles) {
            compile(new LessCompileJob(getLessFile(virtualFileEvent), profile), true);
        }
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
                    public void run(@NotNull final ProgressIndicator indicator) {
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
        } catch (final LessException e) {
            handleLessException(e, compileJob.getCurLessFile(), startTime);
        } catch (final Exception e) {
            handleGenericException(e, compileJob.getCurLessFile(), startTime);
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
    private boolean enqueue(final LessCompileJob compileJob, final boolean async) {
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
                for (final LessCompileJob job : jobs) {
                    compile(job, false);
                }
            }
        });
    }

    private void refreshCssDirs() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                for (final LessProfile lessProfile : getProfiles()) {
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
        return lastCompileTimes.containsKey(lessFilePath) && lastCompileTimes.get(lessFilePath).needsToWait();
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

            for (final String lessFilePath : paths) {
                jobs.add(compileQueueJobs.get(lessFilePath));
            }

            compileQueue.clear();
            compileQueueJobs.clear();
        }

        return jobs;
    }

    /*
     * Exception handling
     */

    private LessFile getLessFile(final @NotNull String uri) {
        try {
            return new LessFile(new URI(uri));
        } catch (final URISyntaxException ex) {
            return new LessFile(uri);
        }
    }

    private void handleLessException(@NotNull final LessException e, @NotNull final LessFile curLessFile, final long startTime) {
        final LessFile srcLessFile;
        if (StringUtils.isNotBlank(e.getFilename())) {
            srcLessFile = getLessFile(e.getFilename());
        } else {
            srcLessFile = curLessFile;
        }
        handleGenericException(e, srcLessFile, startTime);
    }

    private void handleGenericException(@NotNull final Exception e, @NotNull final LessFile lessFile, final long startTime) {
        final double runTime = getRunTime(startTime);
        notifier.error(new LessErrorMessage(e, lessFile));
        LOG.warn(String.format("Compile failed with an exception in %3.2f seconds:", runTime), e);
    }

    /*
     * Success handling
     */

    private void handleSuccess(final LessCompileJob lessCompileJob, final long startTime) {
        final double runTime = getRunTime(startTime);

        LOG.info(String.format("Compile succeeded in %3.2f seconds", runTime));

        for (final LessFile lessFile : lessCompileJob.getSourceAndDependents()) {
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

    /*
     * Notifications
     */

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

    private void warn(final String title, final String text, final String html) {
        final NotificationListenerImpl listener = new NotificationListenerImpl(myProject);
        notifier.warn(title, html, listener);
        LOG.warn(text);
    }

    private String createLink(final LessFile lessFile) {
        return String.format("<a href='%s'>%s</a>", lessFile.getCanonicalPathSafeHtmlEscaped(), lessFile.getName());
    }

    private double getRunTime(final long startTime) {
        return (double)(System.currentTimeMillis() - startTime) / 1000d;
    }

    /*
     * Logging
     */

    private void logChangeEvent(final VirtualFileEvent virtualFileEvent) {
        LOG.info("LessManager.handleEvent(virtualFileEvent)" + "\n" +
                 "\t virtualFileEvent.getFile() = " + virtualFileEvent.getFile().getPath() + "\n" +
                 "\t virtualFileEvent.isFromSave() = " + virtualFileEvent.isFromSave() + "\n" +
                 "\t virtualFileEvent.isFromRefresh() = " + virtualFileEvent.isFromRefresh() + "\n" +
                 "\t virtualFileEvent.getRequestor() = " + toStringLoggable(virtualFileEvent.getRequestor()));
    }

    private void logQueuedJob(final LessCompileJob compileJob, final boolean async) {
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
    private static String toStringLoggable(final Object obj) {
        final String identity = ObjectUtils.identityToString(obj);
        final String override = ObjectUtils.toString(obj, null);
        return ObjectUtils.equals(identity, override) ? identity : identity + " - " + override;
    }
}
