package net.andydvorak.intellij.lessc.ui;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ElementProducer;
import com.intellij.util.ui.ListTableModel;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.EventListenerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class CssConfigurableForm extends NamedConfigurable<LessProfile> {

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
    private JPanel optionsPanel;
    private JPanel compressCssPanel;
    private JPanel lessDirPanelWrap;
    private JPanel lessDirPanel;

    private TextFieldWithBrowseButton lessDirTextField;

    private final TableView<CssDirectory> profileMappingTable;
    private final ListTableModel<CssDirectory> profileMappingModel;

    private List<CssDirectory> cssDirectories;

    public CssConfigurableForm(final Project project, final LessProfile lessProfile, final LessProfilesPanel lessProfilesPanel, final Runnable updater) {
        super(true, updater);

        this.project = project;
        this.lessManager = LessManager.getInstance(project);
        this.lessProfile = lessProfile;
        this.lessProfilesPanel = lessProfilesPanel;
        this.lessProfileName = lessProfile.getName();

        this.cssDirectories = new ArrayList<CssDirectory>(lessProfile.getCssDirectories());

        final ColumnInfo[] columns = { new CssDirectoryColumn(project) };
        profileMappingModel = new ListTableModel<CssDirectory>(columns, cssDirectories, 0);
        profileMappingTable = new TableView<CssDirectory>(profileMappingModel);
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
        lessDirTextField = new TextFieldWithBrowseButtonListener(project, "Choose a LESS source directory");

        lessDirPanel.add(lessDirTextField, GRIDCONSTRAINTS_FILL_ALL);

        compressCssCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                fireChangeEvent();
                updateBox();
            }
        });

        final ElementProducer<CssDirectory> producer = new ElementProducer<CssDirectory>() {
            @Override
            public CssDirectory createElement() {
                return new CssDirectory("/Users/tkmax82/lessc-test/css");
            }

            @Override
            public boolean canCreateElement() {
                return !lessProfilesPanel.getAllProfiles().isEmpty();
            }
        };

        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(profileMappingTable, producer);

        cssDirPanel.add(decorator.createPanel(), GRIDCONSTRAINTS_FILL_ALL);

        return rootPanel;
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
                !Comparing.equal(compressCssCheckbox.isSelected(), lessProfile.isCompressOutput()) ||
                !Comparing.equal(cssDirectories, lessProfile.getCssDirectories());
    }

    public void apply() throws ConfigurationException {
        lessProfile.setLessDirPath(lessDirTextField.getText());
        lessProfile.setCompressOutput(compressCssCheckbox.isSelected());
        lessProfile.setCssDirectories(new ArrayList<CssDirectory>(cssDirectories));

        LessManager.getInstance(project).replaceProfile(lessProfileName, lessProfile);

        lessProfileName = lessProfile.getName();
        modified = false;
    }

    public void reset() {
        lessProfileName = lessProfile.getName();

        lessDirTextField.setText(lessProfile.getLessDirPath());
        compressCssCheckbox.setSelected(lessProfile.isCompressOutput());
    }

    public void disposeUIResources() {
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }

    public void addOptionChangeListener(OptionsPanelListener listener) {
        listeners.add(OptionsPanelListener.class, listener);
    }

    private void fireChangeEvent() {
        Object[] fires = listeners.getListenerList();
        for (int i = fires.length - 2; i >= 0; i -= 2) {
            if (fires[i] == OptionsPanelListener.class) {
                ((OptionsPanelListener)fires[i + 1]).optionChanged();
            }
        }
    }

    private void updateBox() {
    }

}
