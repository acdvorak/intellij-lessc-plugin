package net.andydvorak.intellij.lessc.ui;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.ListCellRendererWrapper;
import com.intellij.ide.util.scopeChooser.PackageSetChooserCombo;
import com.intellij.ide.util.scopeChooser.ScopeChooserConfigurable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.options.newEditor.OptionsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.util.Comparing;
import com.intellij.packageDependencies.DefaultScopesProvider;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.NamedScopesHolder;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.editors.JBComboBoxTableCellEditorComponent;
import com.intellij.ui.table.TableView;
import com.intellij.util.Function;
import com.intellij.util.ui.*;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.LessProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.*;

public class ProjectSettingsPanel {

    private final Project myProject;
    private final LessProfilesPanel myProfilesModel;
    private final LessManager myManager;

    private final TableView<ScopeSetting> myScopeMappingTable;
    private final ListTableModel<ScopeSetting> myScopeMappingModel;
    private final JComboBox myProfilesComboBox = new JComboBox();

    private final HyperlinkLabel myScopesLink = new HyperlinkLabel();

    public ProjectSettingsPanel(Project project, LessProfilesPanel profilesModel) {
        myProject = project;
        myProfilesModel = profilesModel;
        myProfilesModel.addItemsChangeListener(new Runnable() {
            public void run() {
                final Object selectedItem = myProfilesComboBox.getSelectedItem();
                fillLessProfiles();
                myProfilesComboBox.setSelectedItem(selectedItem);
                final ArrayList<ScopeSetting> toRemove = new ArrayList<ScopeSetting>();
                for (ScopeSetting setting : myScopeMappingModel.getItems()) {
                    if (setting.getProfile() == null) {
                        toRemove.add(setting);
                    }
                }
                for (ScopeSetting setting : toRemove) {
                    myScopeMappingModel.removeRow(myScopeMappingModel.indexOf(setting));
                }
            }
        });
        myManager = LessManager.getInstance(project);

        ColumnInfo[] columns = {new ScopeColumn(), new SettingColumn()};
        myScopeMappingModel = new ListTableModel<ScopeSetting>(columns, new ArrayList<ScopeSetting>(), 0);
        myScopeMappingTable = new TableView<ScopeSetting>(myScopeMappingModel);

        fillLessProfiles();
        myProfilesComboBox.setRenderer(new ListCellRendererWrapper<LessProfile>(myProfilesComboBox.getRenderer()) {
            @Override
            public void customize(JList list, LessProfile value, int index, boolean selected, boolean hasFocus) {
                if (value == null) {
                    setText("No profile");
                } else {
                    setText(value.getName());
                }
            }
        });

        myScopesLink.setVisible(!myProject.isDefault());
        myScopesLink.setHyperlinkText("Select Scopes to add new scopes or modify existing ones");
        myScopesLink.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(final HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    final DataContext dataContext = DataManager.getInstance().getDataContextFromFocus().getResult();
                    final OptionsEditor optionsEditor = OptionsEditor.KEY.getData(dataContext);
                    if (optionsEditor != null) {
                        optionsEditor.select(ScopeChooserConfigurable.class);
                    }
                }
            }
        });
    }

    private void fillLessProfiles() {
        final DefaultComboBoxModel boxModel = (DefaultComboBoxModel) myProfilesComboBox.getModel();
        boxModel.removeAllElements();
        boxModel.addElement(null);
        for (LessProfile profile : myProfilesModel.getAllProfiles().values()) {
            boxModel.addElement(profile);
        }
    }

    public JComponent getMainComponent() {
        final JPanel panel = new JPanel(new BorderLayout(0, 10));
        final LabeledComponent<JComboBox> component = new LabeledComponent<JComboBox>();
        component.setText("Default &project profile:");
        component.setLabelLocation(BorderLayout.WEST);
        component.setComponent(myProfilesComboBox);
        panel.add(component, BorderLayout.NORTH);
        ElementProducer<ScopeSetting> producer = new ElementProducer<ScopeSetting>() {
            @Override
            public ScopeSetting createElement() {
                return new ScopeSetting(DefaultScopesProvider.getAllScope(), myProfilesModel.getAllProfiles().values().iterator().next());
            }

            @Override
            public boolean canCreateElement() {
                return !myProfilesModel.getAllProfiles().isEmpty();
            }
        };
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(myScopeMappingTable, producer);
        panel.add(decorator.createPanel(), BorderLayout.CENTER);
        panel.add(myScopesLink, BorderLayout.SOUTH);
        return panel;
    }

    public boolean isModified() {
        final LessProfile defaultProfile = myManager.getDefaultProfile();
        final Object selected = myProfilesComboBox.getSelectedItem();
        if (defaultProfile != selected) {
            if (selected == null) return true;
            if (defaultProfile == null) return true;
            if (!defaultProfile.equals(selected)) return true;
        }
        final Map<String, String> map = myManager.getProfileMapping();
        if (map.size() != myScopeMappingModel.getItems().size()) return true;
        final Iterator<String> iterator = map.keySet().iterator();
        for (ScopeSetting setting : myScopeMappingModel.getItems()) {
            final NamedScope scope = setting.getScope();
            if (!iterator.hasNext()) return true;
            final String scopeName = iterator.next();
            if (!Comparing.strEqual(scopeName, scope.getName())) return true;
            final String profileName = map.get(scope.getName());
            if (profileName == null) return true;
            if (!profileName.equals(setting.getProfileName())) return true;
        }
        return false;
    }

    public void apply() {
        Collection<LessProfile> profiles = new ArrayList<LessProfile>(myManager.getProfiles());
        myManager.clearProfiles();
        for (LessProfile profile : profiles) {
            myManager.addProfile(profile);
        }
        final java.util.List<ScopeSetting> settingList = myScopeMappingModel.getItems();
        for (ScopeSetting scopeSetting : settingList) {
            myManager.mapProfile(scopeSetting.getScope().getName(), scopeSetting.getProfileName());
        }
        myManager.setDefaultProfile((LessProfile) myProfilesComboBox.getSelectedItem());
    }

    public void reset() {
        myProfilesComboBox.setSelectedItem(myManager.getDefaultProfile());
        final java.util.List<ScopeSetting> mappings = new ArrayList<ScopeSetting>();
        final Map<String, String> profileMappings = myManager.getProfileMapping();
        final DependencyValidationManager manager = DependencyValidationManager.getInstance(myProject);
        final Set<String> scopes2Unmap = new HashSet<String>();
        for (final String scopeName : profileMappings.keySet()) {
            final NamedScope scope = manager.getScope(scopeName);
            if (scope != null) {
                mappings.add(new ScopeSetting(scope, profileMappings.get(scopeName)));
            } else {
                scopes2Unmap.add(scopeName);
            }
        }
        for (String scopeName : scopes2Unmap) {
            myManager.unmapProfile(scopeName);
        }
        myScopeMappingModel.setItems(mappings);
    }


    private class ScopeSetting {
        private NamedScope myScope;
        private LessProfile myProfile;
        private String myProfileName;

        private ScopeSetting(NamedScope scope, LessProfile profile) {
            myScope = scope;
            myProfile = profile;
            if (myProfile != null) {
                myProfileName = myProfile.getName();
            }
        }

        public ScopeSetting(NamedScope scope, String profile) {
            myScope = scope;
            myProfileName = profile;
        }

        public LessProfile getProfile() {
            if (myProfileName != null) {
                myProfile = myProfilesModel.getAllProfiles().get(getProfileName());
            }
            return myProfile;
        }

        public void setProfile(@NotNull LessProfile profile) {
            myProfile = profile;
            myProfileName = profile.getName();
        }

        public NamedScope getScope() {
            return myScope;
        }

        public void setScope(NamedScope scope) {
            myScope = scope;
        }

        public String getProfileName() {
            return myProfile != null ? myProfile.getName() : myProfileName;
        }
    }

    private class SettingColumn extends MyColumnInfo<LessProfile> {
        private SettingColumn() {
            super("Setting");
        }

        public TableCellRenderer getRenderer(final ScopeSetting scopeSetting) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        final LessProfile profile = myProfilesModel.getAllProfiles().get(scopeSetting.getProfileName());
                        setForeground(profile == null ? Color.RED : UIUtil.getTableForeground());
                    }
                    setText(scopeSetting.getProfileName());
                    return rendererComponent;
                }
            };
        }

        public TableCellEditor getEditor(final ScopeSetting scopeSetting) {
            return new AbstractTableCellEditor() {
                private final JBComboBoxTableCellEditorComponent myProfilesChooser = new JBComboBoxTableCellEditorComponent();

                public Object getCellEditorValue() {
                    return myProfilesChooser.getEditorValue();
                }

                public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
                    final java.util.List<LessProfile> lessProfiles = new ArrayList<LessProfile>(myProfilesModel.getAllProfiles().values());
                    Collections.sort(lessProfiles, new Comparator<LessProfile>() {
                        @Override
                        public int compare(LessProfile o1, LessProfile o2) {
                            return o1.getName().compareToIgnoreCase(o2.getName());
                        }
                    });
                    myProfilesChooser.setCell(table, row, column);
                    myProfilesChooser.setOptions(lessProfiles.toArray());
                    myProfilesChooser.setDefaultValue(scopeSetting.getProfile());
                    myProfilesChooser.setToString(new Function<Object, String>() {
                        @Override
                        public String fun(Object o) {
                            return ((LessProfile) o).getName();
                        }
                    });
                    return myProfilesChooser;
                }
            };
        }

        public LessProfile valueOf(final ScopeSetting object) {
            return object.getProfile();
        }

        public void setValue(final ScopeSetting scopeSetting, final LessProfile lessProfile) {
            if (lessProfile != null) {
                scopeSetting.setProfile(lessProfile);
            }
        }
    }

    private class ScopeColumn extends MyColumnInfo<NamedScope> {
        private ScopeColumn() {
            super("Scope");
        }

        public TableCellRenderer getRenderer(final ScopeSetting mapping) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (value == null) {
                        setText("");
                    } else {
                        final String scopeName = ((NamedScope) value).getName();
                        if (!isSelected) {
                            final NamedScope scope = NamedScopesHolder.getScope(myProject, scopeName);
                            if (scope == null) setForeground(Color.RED);
                        }
                        setText(scopeName);
                    }
                    return this;
                }
            };
        }

        public TableCellEditor getEditor(final ScopeSetting mapping) {
            return new AbstractTableCellEditor() {
                private PackageSetChooserCombo myScopeChooser;

                @Nullable
                public Object getCellEditorValue() {
                    return myScopeChooser.getSelectedScope();
                }

                public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, int row, int column) {
                    myScopeChooser = new PackageSetChooserCombo(myProject, value == null ? null : ((NamedScope) value).getName(), false, false);
                    ((JBComboBoxTableCellEditorComponent) myScopeChooser.getChildComponent()).setCell(table, row, column);
                    return myScopeChooser;
                }
            };
        }

        public NamedScope valueOf(final ScopeSetting mapping) {
            return mapping.getScope();
        }

        public void setValue(final ScopeSetting mapping, final NamedScope set) {
            mapping.setScope(set);
        }
    }

    private static abstract class MyColumnInfo<T> extends ColumnInfo<ScopeSetting, T> {
        protected MyColumnInfo(final String name) {
            super(name);
        }

        @Override
        public boolean isCellEditable(final ScopeSetting item) {
            return true;
        }
    }

}
