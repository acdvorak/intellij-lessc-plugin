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
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.containers.OrderedSet;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.ui.messages.UIBundle;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LessProfileConfigurableForm extends NamedConfigurable<LessProfile> {

    private static final int SIZEPOLICY_FILL_ALL = GridConstraints.SIZEPOLICY_CAN_GROW | GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW;
    private static final GridConstraints GRIDCONSTRAINTS_FILL_ALL = new GridConstraints(
            /* row = */ 0,
            /* col = */ 0,
            /* rowSpan = */ 1,
            /* colSpan = */ 1,
            /* anchor = */ GridConstraints.ANCHOR_CENTER,
            /* fill = */ GridConstraints.FILL_BOTH,
            /* HSizePolicy = */ SIZEPOLICY_FILL_ALL,
            /* VSizePolicy = */ SIZEPOLICY_FILL_ALL,
            /* minimumSize = */ null,
            /* preferredSize = */ null,
            /* maximumSize = */ null
    );

    private final Project project;
    private final LessManager lessManager;
    private final LessProfile lessProfile;
    private final LessProfilesPanel lessProfilesPanel;

    private final EventListenerList listeners = new EventListenerList();

    private String lessProfileName;
    private boolean modified;
    private JPanel rootPanel;
    private JCheckBox compressCssCheckbox;
    private JPanel cssDirPanel;
    private JPanel lessDirPanelWrap;
    private JPanel lessDirPanel;
    private JButton resetPromptsButton;
    private JTextField includePatternTextField;
    private JTextField excludePatternTextField;
    private JPanel inputPanel;
    private JPanel buttonPanel;
    private JPanel outputPanel;

    private TextFieldWithBrowseButton lessDirTextField;

    private final JBTable profileMappingTable;
    private final ListTableModel<CssDirectory> profileMappingModel;

    private List<CssDirectory> cssDirectories;

    public LessProfileConfigurableForm(final Project project, final LessProfile lessProfile, final LessProfilesPanel lessProfilesPanel, final Runnable updater) {
        super(true, updater);

        this.project = project;
        this.lessManager = LessManager.getInstance(project);
        this.lessProfile = lessProfile;
        this.lessProfilesPanel = lessProfilesPanel;
        this.lessProfileName = lessProfile.getName();

        cssDirectories = new ArrayList<CssDirectory>();

        // Deep clone
        for (CssDirectory cssDirectory : lessProfile.getCssDirectories()) {
            cssDirectories.add(new CssDirectory(cssDirectory));
        }

        final ColumnInfo[] columns = { new CssDirectoryColumn() };
        profileMappingModel = new ListTableModel<CssDirectory>(columns, cssDirectories, 0);
        profileMappingTable = new JBTable(profileMappingModel);
    }

    public void setDisplayName(String displayName) {
        lessProfile.setName(displayName);
    }

    public LessProfile getEditableObject() {
        return lessProfile;
    }

    public String getBannerSlogan() {
        return lessProfile.getName();
    }

    public JComponent createOptionsPanel() {
        lessDirTextField = new TextFieldWithBrowseButtonImpl(project, UIBundle.message("file.chooser.less.title"));

        lessDirPanel.add(lessDirTextField, GRIDCONSTRAINTS_FILL_ALL);

        profileMappingTable.addMouseListener(new MouseListener() {
            @Override public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && !mouseEvent.isConsumed()) {
                    mouseEvent.consume();
                    editRow();
                }
            }
            @Override public void mousePressed(MouseEvent mouseEvent) {}
            @Override public void mouseReleased(MouseEvent mouseEvent) {}
            @Override public void mouseEntered(MouseEvent mouseEvent) {}
            @Override public void mouseExited(MouseEvent mouseEvent) {}
        });

        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(profileMappingTable);
        decorator
                .setAddAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        addRow();
                    }
                })
                .setEditAction(new AnActionButtonRunnable() {
                    @Override
                    public void run(AnActionButton button) {
                        editRow();
                    }
                })
                .disableUpDownActions();

        cssDirPanel.add(decorator.createPanel(), GRIDCONSTRAINTS_FILL_ALL);

        resetPromptsButton.setMnemonic('r');
        resetPromptsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                lessManager.getState().resetPrompts();
                lessProfilesPanel.setPromptButtonsEnabled(false);
            }
        });
        resetPromptsButton.setEnabled(!lessManager.getState().hasDefaultPromptSettings());

        return rootPanel;
    }

    public void setPromptButtonEnabled(final boolean enabled) {
        resetPromptsButton.setEnabled(enabled);
    }

    private void addRow() {
        final String path = promptForFilePath();

        if (StringUtil.isNotEmpty(path)) {
            profileMappingModel.addRow(new CssDirectory(path));
            removeDuplicateRows();
        }
    }

    private void editRow() {
        if (profileMappingTable.getSelectedRowCount() != 1) {
            return;
        }

        final CssDirectory cssDirectory = (CssDirectory) profileMappingModel.getItem(profileMappingTable.getSelectedRow());

        if (cssDirectory == null)
            return;

        final String newPath = promptForFilePath(cssDirectory.getPath());

        if (StringUtil.isNotEmpty(newPath)) {
            cssDirectory.setPath(newPath);
            removeDuplicateRows();
        }
    }

    private void removeDuplicateRows() {
        final Set<CssDirectory> withoutDups = new OrderedSet<CssDirectory>();
        withoutDups.addAll(profileMappingModel.getItems());
        cssDirectories = new ArrayList<CssDirectory>(withoutDups);
        profileMappingModel.setItems(cssDirectories);
    }

    @Nullable
    private String promptForFilePath() {
        return promptForFilePath(null);
    }

    @Nullable
    private String promptForFilePath(final @Nullable String initial) {
        @NotNull
        final String initialNN = StringUtils.defaultString(initial);
        final FileChooserDescriptor d = getFileChooserDescriptor();
        final VirtualFile initialFile = StringUtil.isNotEmpty(initialNN) ? LocalFileSystem.getInstance().findFileByPath(initialNN) : null;
        final VirtualFile file = project != null ? FileChooser.chooseFile(project, d, initialFile) : FileChooser.chooseFile(profileMappingTable, d, initialFile);
        String path = null;
        if (file != null) {
            path = file.getPresentableUrl();
            if (SystemInfo.isWindows && path.length() == 2 && Character.isLetter(path.charAt(0)) && path.charAt(1) == ':') {
                path += "\\"; // make path absolute
            }
        }
        return path;
    }

    private FileChooserDescriptor getFileChooserDescriptor() {
        FileChooserDescriptor d = new FileChooserDescriptor(false, true, false, false, false, false);
        d.setTitle(UIBundle.message("file.chooser.css.title"));
        d.setShowFileSystemRoots(true);
        return d;
    }

    @Nls
    public String getDisplayName() {
        return lessProfile.getName();
    }

    @Nullable
    public Icon getIcon() {
        return null;
    }

    @Nullable
    @NonNls
    public String getHelpTopic() {
        return null;
    }

    public boolean isModified() {
        return modified ||
                !Comparing.strEqual(lessProfileName, lessProfile.getName()) ||
                !Comparing.strEqual(lessDirTextField.getText(), lessProfile.getLessDirPath()) ||
                !Comparing.strEqual(includePatternTextField.getText(), lessProfile.getIncludePattern()) ||
                !Comparing.strEqual(excludePatternTextField.getText(), lessProfile.getExcludePattern()) ||
                !Comparing.equal(compressCssCheckbox.isSelected(), lessProfile.isCompressOutput()) ||
                !Comparing.equal(cssDirectories, lessProfile.getCssDirectories());
    }

    public void apply() throws ConfigurationException {
        lessProfile.setLessDirPath(lessDirTextField.getText());
        lessProfile.setIncludePattern(includePatternTextField.getText());
        lessProfile.setExcludePattern(excludePatternTextField.getText());
        lessProfile.setCompressOutput(compressCssCheckbox.isSelected());
        lessProfile.setCssDirectories(new ArrayList<CssDirectory>(cssDirectories));

        LessManager.getInstance(project).replaceProfile(lessProfileName, lessProfile);

        lessProfileName = lessProfile.getName();
        modified = false;
    }

    public void reset() {
        lessProfileName = lessProfile.getName();
        lessDirTextField.setText(lessProfile.getLessDirPath());
        includePatternTextField.setText(lessProfile.getIncludePattern());
        excludePatternTextField.setText(lessProfile.getExcludePattern());
        compressCssCheckbox.setSelected(lessProfile.isCompressOutput());
    }

    public void disposeUIResources() {
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}
