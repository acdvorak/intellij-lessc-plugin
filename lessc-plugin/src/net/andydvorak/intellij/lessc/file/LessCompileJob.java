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

package net.andydvorak.intellij.lessc.file;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;
import com.intellij.openapi.diagnostic.Logger;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LessCompileJob implements LessCompileObservable {

    private static final long WAIT_INTERVAL_MS = 250;
    private static final Logger LOG = Logger.getInstance("#" + LessCompileJob.class.getName());

    private final AtomicBoolean compiled = new AtomicBoolean();
    private final AtomicBoolean running = new AtomicBoolean();

    private final AtomicLong finished = new AtomicLong(0);

    private final LessFile sourceLessFile;
    private final LessProfile lessProfile;

    private final Set<LessFile> sourceAndDependents = new LinkedHashSet<LessFile>();
    private final AtomicInteger curLessFileIndex = new AtomicInteger(-1);

    private final Set<LessFile> updatedLessFiles = new LinkedHashSet<LessFile>();
    private final Set<String> updatedLessFilePaths = new LinkedHashSet<String>();
    private final Set<LessCompileObserver> observers = new LinkedHashSet<LessCompileObserver>();

    private LessEngine lessEngine;

    /*
     * Constructors
     */

    public LessCompileJob(final LessFile sourceLessFile, final LessProfile lessProfile) {
        this.sourceLessFile = sourceLessFile;
        this.lessProfile = lessProfile;
    }

    public LessCompileJob(final LessCompileJob otherCompileJob, final LessFile sourceLessFile) {
        this.sourceLessFile = sourceLessFile;
        this.lessProfile = otherCompileJob.getLessProfile();

        // Clone sets
        this.updatedLessFiles.addAll(otherCompileJob.getUpdatedLessFiles());
        this.updatedLessFilePaths.addAll(otherCompileJob.getUpdatedLessFilePaths());
        this.observers.addAll(otherCompileJob.observers);
    }

    /*
     * Public instance methods
     */

    public LessFile getSourceLessFile() {
        return sourceLessFile;
    }

    public LessProfile getLessProfile() {
        return lessProfile;
    }

    public Set<LessFile> getSourceAndDependents() {
        return sourceAndDependents;
    }

    public LessFile getCurLessFile() {
        final int index = curLessFileIndex.get();
        if (!sourceAndDependents.isEmpty() && index > -1) {
            final LessFile[] lessFiles = new LessFile[sourceAndDependents.size()];
            sourceAndDependents.toArray(lessFiles);
            return lessFiles[index];
        } else {
            return null;
        }
    }

    public void addUpdatedLessFile(LessFile lessFile) {
        updatedLessFiles.add(lessFile);
        updatedLessFilePaths.add(lessFile.getCanonicalPathSafe());
    }

    /**
     * @return set of LESS {@code File}s that produced new or updated CSS files after being compiled
     */
    public Set<LessFile> getUpdatedLessFiles() {
        return new LinkedHashSet<LessFile>(updatedLessFiles);
    }

    /**
     * @return set of paths to LESS {@code File}s that produced new or updated CSS files after being compiled
     */
    public Set<String> getUpdatedLessFilePaths() {
        return new LinkedHashSet<String>(updatedLessFilePaths);
    }

    public int getNumUpdated() {
        return getUpdatedLessFiles().size();
    }

    @Override
    public void addObserver(@NotNull LessCompileObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(@NotNull LessCompileObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(@NotNull LessCompileNotification notification) {
        for (LessCompileObserver observer : observers) {
            notification.notifyObserver(observer);
        }
    }

    public void compile() throws IOException, LessException, IllegalStateException {
        preventConcurrency();
        initLessEngine();
        try {
            start();
        } finally {
            finish();
        }
    }

    public void refreshVFS() {
        if (lessProfile != null)
            VFSLocationChange.refresh(lessProfile.getCssDirectories());
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isFinished() {
        return compiled.get();
    }

    // TODO: Rename this method
    public boolean canCreateNewCompileJob() {
        return !isRunning() && (!isFinished() || System.currentTimeMillis() - finished.get() > WAIT_INTERVAL_MS);
    }

    /*
    * Private instance methods
    */

    private void preventConcurrency() throws IllegalStateException {
        if (compiled.get() || running.get()) {
            final String message = "LessCompileJob for \"" + sourceLessFile.getName() + "\" can only be compiled once.";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
    }

    private void initLessEngine() throws LessException {
        if (lessEngine == null) {
            lessEngine = new LessEngine();
        }
    }

    private void findSourceAndDependents() throws IOException {
        if (sourceAndDependents.isEmpty()) {
            sourceAndDependents.add(sourceLessFile);
            if (lessProfile != null && lessProfile.hasCssDirectories()) {
                final Set<LessFile> dependents = sourceLessFile.getDependentsRecursive(lessProfile);
                sourceAndDependents.addAll(dependents);
            }
        }
    }

    private void start() throws IOException, LessException {
        running.set(true);

        findSourceAndDependents();

        notifyObservers(new LessCompileNotification() {
            @Override
            public void notifyObserver(@NotNull LessCompileObserver observer) {
                observer.compileStarted(sourceAndDependents);
            }
        });

        for (LessFile lessFile : sourceAndDependents) {
            compile(lessFile);
        }
    }

    private void compile(@NotNull final LessFile lessFile) throws IOException, LessException {
        final boolean cssChanged;

        curLessFileIndex.incrementAndGet();

        if (lessFile.shouldCompile(lessProfile)) {
            lessFile.compile(lessEngine, lessProfile);
            cssChanged = lessFile.hasCssChanged();
        } else {
            cssChanged = false;
        }

        final LessCompileNotification notification;

        if (cssChanged) {
            addUpdatedLessFile(lessFile);
            notification = new LessCompileNotification() {
                @Override
                public void notifyObserver(@NotNull LessCompileObserver observer) {
                    observer.cssFileChanged(lessFile);
                }
            };
            refreshVFS();
        } else {
            notification = new LessCompileNotification() {
                @Override
                public void notifyObserver(@NotNull LessCompileObserver observer) {
                    observer.cssFileUnchanged(lessFile);
                }
            };
        }

        notifyObservers(notification);
    }

    private void finish() {
        finished.set(System.currentTimeMillis());
        compiled.set(true);
        running.set(false);

        final int numCompiled = getNumUpdated();

        notifyObservers(new LessCompileNotification() {
            @Override
            public void notifyObserver(@NotNull LessCompileObserver observer) {
                observer.compileFinished(numCompiled);
            }
        });
    }
}
