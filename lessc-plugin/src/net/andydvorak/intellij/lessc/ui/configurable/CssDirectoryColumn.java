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

import com.intellij.ui.JBColor;
import com.intellij.util.ui.ColumnInfo;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.ui.messages.UIBundle;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.io.File;

public class CssDirectoryColumn extends ColumnInfo<CssDirectory, String> {

    public CssDirectoryColumn() {
        super(UIBundle.message("table.header.css.output.dir"));
    }

    public TableCellRenderer getRenderer(final CssDirectory cssDirectory) {
        return new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value == null) {
                    setText("");
                }
                else {
                    final String path = (String) value;
                    if (!isSelected) {
                        final File file = new File(path);
                        if ( ! (file.exists() && file.isDirectory()) ) setForeground(JBColor.RED);
                    }
                    setText(path);
                }
                return this;
            }
        };
    }

    public String valueOf(final CssDirectory cssDirectory) {
        return cssDirectory.getPath();
    }

    public void setValue(final CssDirectory cssDirectory, final String newPath) {
        cssDirectory.setPath(newPath);
    }

    @Override
    public boolean isCellEditable(final CssDirectory item) {
        return false;
    }

}