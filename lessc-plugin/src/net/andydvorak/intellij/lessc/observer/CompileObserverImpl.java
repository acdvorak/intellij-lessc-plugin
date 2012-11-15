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

package net.andydvorak.intellij.lessc.observer;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import net.andydvorak.intellij.lessc.fs.LessCompileJob;
import net.andydvorak.intellij.lessc.fs.LessFile;
import net.andydvorak.intellij.lessc.ui.messages.NotificationsBundle;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Andrew C. Dvorak
 * @since 11/13/12
 */
public class CompileObserverImpl implements CompileObserver {

    private final LessCompileJob compileJob;
    private final Task.Backgroundable task;
    private final ProgressIndicator indicator;

    private double numToCompile;
    private double numCompleted;
    private double numChanged;

    public CompileObserverImpl(@NotNull final LessCompileJob compileJob,
                               @NotNull final Task.Backgroundable task,
                               @NotNull final ProgressIndicator indicator) {
        this.compileJob = compileJob;
        this.task = task;
        this.indicator = indicator;
    }

    @Override
    public void compileStarted(@NotNull final Set<LessFile> lessFiles) {
        numToCompile = lessFiles.size();
        if (numToCompile > 1) {
            updateProgress();
        }
    }

    @Override
    public void outputFileChanged(@NotNull final LessFile lessFile) {
        numCompleted++;
        numChanged++;
        updateProgress();
    }

    @Override
    public void outputFileUnchanged(@NotNull final LessFile lessFile) {
        numCompleted++;
        updateProgress();
    }

    @Override
    public void compileFinished(int numChanged) {
        assert(this.numChanged == numChanged);
        updateProgress();
    }

    private void updateProgress() {
        if (numToCompile > 0) {
            final LessFile curLessFile = compileJob.getCurLessFile();
            final String title = NotificationsBundle.message("compiling.multiple",
                    (int) numCompleted + 1,
                    (int) numToCompile,
                    curLessFile != null ? curLessFile.getName() : null);
            task.setTitle(title);
            indicator.setFraction(numCompleted / numToCompile);
        }
    }
}
