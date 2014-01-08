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

package net.andydvorak.intellij.lessc.ui.notifier;

import net.andydvorak.intellij.lessc.fs.LessFile;
import net.andydvorak.intellij.lessc.ui.messages.NotificationsBundle;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LessErrorMessage extends Exception {

    private final Matcher matcher;
    private final String filePath;
    private final String fileName;
    private final String title;
    private final String message;
    private final String message_html;
    private final int line;
    private final int column;

    public LessErrorMessage(final @NotNull Throwable t, final @NotNull LessFile lessFile) {
        super(t);

        filePath = lessFile.getCanonicalPathSafe();
        fileName = lessFile.getName();

        title = NotificationsBundle.message("error.title");
        message = t.getLocalizedMessage();
        matcher = Pattern.compile("line ([0-9]+), column ([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(message);

        if (matcher.find()) {
            message_html = matcher.replaceFirst("<a href='file'>$0</a>");
            line = Integer.parseInt(matcher.group(1));
            column = Integer.parseInt(matcher.group(2));
        } else {
            message_html = message;
            line = column = -1;
        }
    }

    @NotNull
    public String getFilePath() {
        return filePath;
    }

    @NotNull
    public String getFileName() {
        return fileName;
    }

    @NotNull
    public String getTitle() {
        return title;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    @NotNull
    public String getText() {
        return getFileName() + ": " + message;
    }

    @NotNull
    public String getHtml() {
        return "<a href='file'>" + getFileName() + "</a>" + ": " + message_html;
    }

}
