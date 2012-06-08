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

package net.andydvorak.intellij.lessc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.CellEditorComponentWithBrowseButton;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class CssDirectoryCellEditor extends AbstractTableCellEditor {

    private final CssDirectory cssDirectory;
    private final TextFieldWithBrowseButton textFieldWithBrowseButton;
    private final CellEditorComponentWithBrowseButton<JTextField> cellEditorComponent;

    public CssDirectoryCellEditor(final Project project, final CssDirectory cssDirectory) {
        this.cssDirectory = cssDirectory;
        this.textFieldWithBrowseButton = new TextFieldWithBrowseButtonListener(project, "Choose a CSS output directory");
        this.cellEditorComponent = new CellEditorComponentWithBrowseButton<JTextField>(textFieldWithBrowseButton, this);
        getChildComponent().setEditable(false);
        getChildComponent().setBorder(null);
    }

    @Nullable
    public Object getCellEditorValue() {
        return getChildComponent().getText();
    }

    private JTextField getChildComponent() {
        return cellEditorComponent.getChildComponent();
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        getChildComponent().setText((String) value);
        cssDirectory.setPath((String) value);
        return cellEditorComponent;
    }

}