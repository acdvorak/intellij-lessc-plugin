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

package net.andydvorak.intellij.lessc.ui.configurable;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TextFieldWithBrowseButtonImpl extends TextFieldWithBrowseButton {

    private final Project myProject;
    private String myTitle;

    public TextFieldWithBrowseButtonImpl(final Project project) {
        this(project, null);
    }

    public TextFieldWithBrowseButtonImpl(@Nullable final Project project, @Nullable final String title) {
        super();

        this.myProject = project;
        this.myTitle = title;

        final TextFieldWithBrowseButtonImpl parent = this;
        final Project defaultProject = ProjectManager.getInstance().getDefaultProject();
        this.getButton().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final FileChooserDescriptor d = getFileChooserDescriptor();
                String initial = parent.getText();
                VirtualFile initialFile = StringUtil.isNotEmpty(initial) ? LocalFileSystem.getInstance().findFileByPath(initial) : null;
                VirtualFile file =
                        myProject != null ? FileChooser.chooseFile(d, myProject, initialFile) : FileChooser.chooseFile(d, defaultProject, initialFile);
                if (file != null) {
                    String path = file.getPresentableUrl();
                    if (SystemInfo.isWindows && path.length() == 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                        path += "\\"; // make path absolute
                    }
                    parent.setText(path);
                }
            }
        });
    }

    private FileChooserDescriptor getFileChooserDescriptor() {
        FileChooserDescriptor d = new FileChooserDescriptor(false, true, false, false, false, false);
        if (myTitle != null) {
            d.setTitle(myTitle);
        }
        d.setShowFileSystemRoots(true);
        return d;
    }

    private void setFileChooserTitle(final String title) {
        this.myTitle = title;
    }

}
