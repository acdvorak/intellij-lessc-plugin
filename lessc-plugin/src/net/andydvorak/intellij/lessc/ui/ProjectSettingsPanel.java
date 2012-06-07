package net.andydvorak.intellij.lessc.ui;

public class ProjectSettingsPanel {

    /*

    private final Project project;
    private final LessManager lessManager;
    private final LessProfilesPanel lessProfilesPanel;

    private final TableView<LessProfile> profileMappingTable;
    private final ListTableModel<LessProfile> profileMappingModel;

    public ProjectSettingsPanel(final Project project, final LessProfilesPanel lessProfilesPanel) {
        this.project = project;
        this.lessManager = LessManager.getInstance(project);
        this.lessProfilesPanel = lessProfilesPanel;
        this.lessProfilesPanel.addItemsChangeListener(new Runnable() {
            public void run() {
//                final Object selectedItem = myProfilesComboBox.getSelectedItem();
//                fillLessProfiles();
//                myProfilesComboBox.setSelectedItem(selectedItem);
//                final ArrayList<ScopeSetting> toRemove = new ArrayList<ScopeSetting>();
//                for (ScopeSetting setting : profileMappingModel.getItems()) {
//                    if (setting.getProfile() == null) {
//                        toRemove.add(setting);
//                    }
//                }
//                for (ScopeSetting setting : toRemove) {
//                    profileMappingModel.removeRow(profileMappingModel.indexOf(setting));
//                }
            }
        });

        final ColumnInfo[] columns = {new ScopeColumn(), new SettingColumn()};
        profileMappingModel = new ListTableModel<LessProfile>(columns, new ArrayList<LessProfile>(), 0);
        profileMappingTable = new TableView<LessProfile>(profileMappingModel);
    }

    public JComponent getMainComponent() {
        final JPanel panel = new JPanel(new BorderLayout(0, 10));
        ElementProducer<ScopeSetting> producer = new ElementProducer<ScopeSetting>() {
            @Override
            public ScopeSetting createElement() {
                return new ScopeSetting(DefaultScopesProvider.getAllScope(), lessProfilesPanel.getAllProfiles().values().iterator().next());
            }

            @Override
            public boolean canCreateElement() {
                return !lessProfilesPanel.getAllProfiles().isEmpty();
            }
        };
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(profileMappingTable, producer);
        panel.add(decorator.createPanel(), BorderLayout.CENTER);
        return panel;
    }

    public boolean isModified() {
        final LessProfile defaultProfile = lessManager.getDefaultProfile();
        final Object selected = myProfilesComboBox.getSelectedItem();
        if (defaultProfile != selected) {
            if (selected == null) return true;
            if (defaultProfile == null) return true;
            if (!defaultProfile.equals(selected)) return true;
        }
        final Map<String, String> map = lessManager.getProfileMapping();
        if (map.size() != profileMappingModel.getItems().size()) return true;
        final Iterator<String> iterator = map.keySet().iterator();
        for (ScopeSetting setting : profileMappingModel.getItems()) {
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
        Collection<LessProfile> profiles = new ArrayList<LessProfile>(lessManager.getProfiles());
        lessManager.clearProfiles();
        for (LessProfile profile : profiles) {
            lessManager.addProfile(profile);
        }
        final java.util.List<ScopeSetting> settingList = profileMappingModel.getItems();
        for (ScopeSetting scopeSetting : settingList) {
            lessManager.mapProfile(scopeSetting.getScope().getName(), scopeSetting.getProfileName());
        }
        lessManager.setDefaultProfile((LessProfile) myProfilesComboBox.getSelectedItem());
    }

    public void reset() {
        myProfilesComboBox.setSelectedItem(lessManager.getDefaultProfile());
        final java.util.List<ScopeSetting> mappings = new ArrayList<ScopeSetting>();
        final Map<String, String> profileMappings = lessManager.getProfileMapping();
        final DependencyValidationManager manager = DependencyValidationManager.getInstance(project);
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
            lessManager.unmapProfile(scopeName);
        }
        profileMappingModel.setItems(mappings);
    }

    /*
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
                myProfile = lessProfilesPanel.getAllProfiles().get(getProfileName());
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
    */
    /*

    private class SettingColumn extends MyColumnInfo<LessProfile> {
        private SettingColumn() {
            super("Setting");
        }

        public TableCellRenderer getRenderer(final ScopeSetting scopeSetting) {
            return new DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    final Component rendererComponent = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    if (!isSelected) {
                        final LessProfile profile = lessProfilesPanel.getAllProfiles().get(scopeSetting.getProfileName());
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
                    final java.util.List<LessProfile> lessProfiles = new ArrayList<LessProfile>(lessProfilesPanel.getAllProfiles().values());
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
                            final NamedScope scope = NamedScopesHolder.getScope(project, scopeName);
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
                    myScopeChooser = new PackageSetChooserCombo(project, value == null ? null : ((NamedScope) value).getName(), false, false);
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

    */

}
