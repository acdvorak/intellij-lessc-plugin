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

import org.jetbrains.annotations.NotNull;

import java.util.Set;

/**
 * @author Andrew C. Dvorak
 * @since 11/13/12
 */
public interface LessCompileObserver {
    public void compileStarted(@NotNull Set<LessFile> lessFiles);
    public void cssFileChanged(@NotNull LessFile lessFile);
    public void cssFileUnchanged(@NotNull LessFile lessFile);
    public void compileFinished(int numChanged);
}
