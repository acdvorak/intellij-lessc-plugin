package net.andydvorak.intellij.lessc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.util.ui.ColumnInfo;
import net.andydvorak.intellij.lessc.state.CssDirectory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.io.File;

public class CssDirectoryColumn extends ColumnInfo<CssDirectory, String> {

    private Project project;

    public CssDirectoryColumn(final Project project) {
        super("CSS Output Directory");
        this.project = project;
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
                        if ( ! (file.exists() && file.isDirectory()) ) setForeground(Color.RED);
                    }
                    setText(path);
                }
                return this;
            }
        };
    }

    public TableCellEditor getEditor(final CssDirectory cssDirectory) {
        return new CssDirectoryCellEditor(project, cssDirectory);
    }

    public String valueOf(final CssDirectory cssDirectory) {
        return cssDirectory.getPath();
    }

    public void setValue(final CssDirectory cssDirectory, final String newPath) {
        cssDirectory.setPath(newPath);
    }

    @Override
    public boolean isCellEditable(final CssDirectory item) {
        return true;
    }

}