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

package net.andydvorak.intellij.lessc.fs;

import com.asual.lesscss.LessEngine;
import com.asual.lesscss.LessException;
import com.intellij.openapi.diagnostic.Logger;
import net.andydvorak.intellij.lessc.observer.CompileEvent;
import net.andydvorak.intellij.lessc.observer.CompileObservable;
import net.andydvorak.intellij.lessc.observer.CompileObserver;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LessCompileJob implements CompileObservable {

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
    private final Set<CompileObserver> observers = new LinkedHashSet<CompileObserver>();

    /*
     * Constructors
     */

    public LessCompileJob(final LessFile sourceLessFile, final LessProfile lessProfile) {
        this.sourceLessFile = sourceLessFile;
        this.lessProfile = lessProfile;
    }

    /*
     * Public instance methods
     */

    public LessFile getSourceLessFile() {
        return sourceLessFile;
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

    public void addUpdatedLessFile(final LessFile lessFile) {
        updatedLessFiles.add(lessFile);
    }

    /**
     * @return set of LESS {@code File}s that produced new or updated CSS files after being compiled
     */
    public Set<LessFile> getUpdatedLessFiles() {
        return new LinkedHashSet<LessFile>(updatedLessFiles);
    }

    public int getNumUpdated() {
        return getUpdatedLessFiles().size();
    }

    @Override
    public void addObserver(@NotNull final CompileObserver observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(@NotNull final CompileObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(@NotNull final CompileEvent event) {
        for (final CompileObserver observer : observers) {
            event.notify(observer);
        }
    }

    public void compile() throws IOException, LessException, IllegalStateException {
        preventConcurrency();
        try {
            start();
        } finally {
            finish();
        }
    }

    public void refreshVFS() {
        if (lessProfile != null)
            VirtualFileLocationChange.refresh(lessProfile.getCssDirectories());
    }

    public boolean isRunning() {
        return running.get();
    }

    public boolean isFinished() {
        return compiled.get();
    }

    public boolean needsToWait() {
        return isRunning() || (isFinished() && System.currentTimeMillis() - finished.get() <= WAIT_INTERVAL_MS);
    }

    /*
    * Private instance methods
    */

    /**
     * Throws an {@code IllegalStateException} if this job is run more than once.
     * @throws IllegalStateException
     */
    private void preventConcurrency() throws IllegalStateException {
        if (compiled.get() || running.get()) {
            final String message = "LessCompileJob for \"" + sourceLessFile.getName() + "\" can only be compiled once.";
            LOG.error(message);
            throw new IllegalStateException(message);
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

        notifyObservers(new CompileEvent() {
            @Override
            public void notify(@NotNull final CompileObserver observer) {
                observer.compileStarted(sourceAndDependents);
            }
        });

        for (final LessFile lessFile : sourceAndDependents) {
            compile(lessFile);
        }
    }

    private void compile(@NotNull final LessFile lessFile) throws IOException, LessException {
        final boolean cssChanged;

        curLessFileIndex.incrementAndGet();

        if (lessFile.shouldCompile(lessProfile)) {
            lessFile.compile(LessEngine.getInstance(), lessProfile);
            cssChanged = lessFile.hasCssChanged();
        } else {
            cssChanged = false;
        }

        final CompileEvent event;

        if (cssChanged) {
            addUpdatedLessFile(lessFile);
            event = new CompileEvent() {
                @Override
                public void notify(@NotNull final CompileObserver observer) {
                    observer.outputFileChanged(lessFile);
                }
            };
            refreshVFS();
        } else {
            event = new CompileEvent() {
                @Override
                public void notify(@NotNull final CompileObserver observer) {
                    observer.outputFileUnchanged(lessFile);
                }
            };
        }

        notifyObservers(event);
    }

    private void finish() {
        finished.set(System.currentTimeMillis());
        compiled.set(true);
        running.set(false);

        final int numCompiled = getNumUpdated();

        notifyObservers(new CompileEvent() {
            @Override
            public void notify(@NotNull final CompileObserver observer) {
                observer.compileFinished(numCompiled);
            }
        });
    }
}
