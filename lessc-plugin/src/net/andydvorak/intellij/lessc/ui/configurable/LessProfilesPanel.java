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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.*;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.PlatformIcons;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.state.CssDirectory;
import net.andydvorak.intellij.lessc.ui.messages.UIBundle;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LessProfilesPanel extends MasterDetailsComponent implements SearchableConfigurable {

    @NotNull private final Project project;
    @NotNull private final LessManager lessManager;
    @NotNull private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final List<LessProfileConfigurableForm> profileForms = new ArrayList<LessProfileConfigurableForm>();

    public LessProfilesPanel(@NotNull final Project project) {
        this.project = project;
        this.lessManager = LessManager.getInstance(project);
        initTree();
    }

    protected boolean wasObjectStored(final Object o) {
        if (o == null || LessProfile.class != o.getClass()) return false;
        final LessProfile profile = (LessProfile) o;
        return lessManager.getProfileMap().containsKey(profile.getId());
    }

    public void apply() throws ConfigurationException {
        final Set<String> names = new HashSet<String>();

        // Check for duplicate profile names
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            final MyNode node = (MyNode) myRoot.getChildAt(i);
            final LessProfileConfigurableForm form = (LessProfileConfigurableForm) node.getConfigurable();
            final LessProfile profile = form.getCurrentState();
            final String name = profile.getName();
            final String dirPath = profile.getLessDirPath();

            if (StringUtils.isBlank(name)) {
                selectNodeInTree(name);
                throw new ConfigurationException(UIBundle.message("blank.less.profile.name"));
            }

            if (names.contains(name)) {
                selectNodeInTree(name);
                throw new ConfigurationException(UIBundle.message("duplicate.less.profile.name", name));
            }

            if (StringUtils.isBlank(dirPath)) {
                selectNodeInTree(name);
                throw new ConfigurationException(UIBundle.message("blank.less.profile.source.dir"));
            }

            if (!profile.getLessDir().exists()) {
                if (!confirmWarning(UIBundle.message("nonexistent.less.profile.source.dir.title"),
                                    UIBundle.message("nonexistent.less.profile.source.dir.prompt", dirPath))) {
                    selectNodeInTree(name);
                    throw new ConfigurationException(UIBundle.message("nonexistent.less.profile.source.dir.error", dirPath));
                }
            }

            if (profile.getCssDirectories().isEmpty()) {
                selectNodeInTree(name);
                throw new ConfigurationException(UIBundle.message("no.less.profile.css.dirs"));
            }

            for (final CssDirectory cssDirectory : profile.getCssDirectories()) {
                if (!new File(cssDirectory.getPath()).exists()) {
                    if (!confirmWarning(UIBundle.message("nonexistent.less.profile.css.dir.title"),
                                        UIBundle.message("nonexistent.less.profile.css.dir.prompt", dirPath))) {
                        selectNodeInTree(name);
                        throw new ConfigurationException(UIBundle.message("nonexistent.less.profile.css.dir.error", dirPath));
                    }
                }
            }

            names.add(name);
        }

        super.apply();
    }

    protected void processRemovedItems() {
        final Map<Integer, LessProfile> profiles = getProfileMap();
        final List<Integer> deletedIds = new ArrayList<Integer>();

        // Compile a list of all profiles that are no longer present in the UI
        for (final LessProfile profile : lessManager.getProfiles()) {
            if (!profiles.containsKey(profile.getId())) {
                deletedIds.add(profile.getId());
            }
        }

        // Remove the deleted profiles from the manager
        for (final int id : deletedIds) {
            lessManager.removeProfile(id);
        }
    }

    public void reset() {
        reloadTree();
        super.reset();
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        isInitialized.set(false);
    }

    @Nullable
    protected ArrayList<AnAction> createActions(final boolean fromPopup) {
        final ArrayList<AnAction> result = new ArrayList<AnAction>();

        final String addText = UIBundle.message("action.add.less.profile.text");
        final String addDescription = UIBundle.message("action.add.less.profile.description");
        final String addPromptTitle = UIBundle.message("action.add.less.profile.prompt.title");

        result.add(new AnAction(addText, addDescription, PlatformIcons.ADD_ICON) {
            {
                registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
            }

            public void actionPerformed(final AnActionEvent event) {
                final String name = askForProfileName(addPromptTitle, "");
                if (name == null) return;
                final LessProfile lessProfile = new LessProfile(getNextId(), name);
                addProfileNode(lessProfile);
            }
        });

        result.add(new MyDeleteAction(forAll(Conditions.alwaysTrue())));

        final String copyText = UIBundle.message("action.copy.less.profile.text");
        final String copyDescription = UIBundle.message("action.copy.less.profile.description");
        final String copyPromptTitle = UIBundle.message("action.copy.less.profile.description");

        result.add(new AnAction(copyText, copyDescription, PlatformIcons.COPY_ICON) {
            {
                registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_MASK)), myTree);
            }
            public void actionPerformed(final AnActionEvent event) {
                final String profileName = askForProfileName(copyPromptTitle, "");
                if (profileName == null) return;
                final LessProfile clone = new LessProfile(getNextId(), (LessProfile) getSelectedObject());
                clone.setName(profileName);
                addProfileNode(clone);
            }

            public void update(final AnActionEvent event) {
                super.update(event);
                event.getPresentation().setEnabled(getSelectedObject() != null);
            }
        });

        return result;
    }

    private void addProfileNode(final LessProfile lessProfile) {
        final LessProfileConfigurableForm profileForm = new LessProfileConfigurableForm(project, lessProfile, this, TREE_UPDATER);
        profileForm.setModified(true);
        profileForms.add(profileForm);
        final MyNode node = new MyNode(profileForm);
        addNode(node, myRoot);
        selectNodeInTree(node);
    }

    private void reloadTree() {
        myRoot.removeAllChildren();
        profileForms.clear();
        final Collection<LessProfile> profiles = lessManager.getProfiles();
        for (final LessProfile profile : profiles) {
            final LessProfile clone = new LessProfile(profile.getId(), profile);
            final LessProfileConfigurableForm profileForm = new LessProfileConfigurableForm(project, clone, this, TREE_UPDATER);
            profileForms.add(profileForm);
            addNode(new MyNode(profileForm), myRoot);
        }
        isInitialized.set(true);
    }

    private Map<Integer, LessProfile> getProfileMap() {
        if (!isInitialized.get()) {
            return getManagerProfileMap();
        } else {
            return getUIProfileMap();
        }
    }

    private Map<Integer, LessProfile> getManagerProfileMap() {
        return lessManager.getProfileMap();
    }

    private Map<Integer, LessProfile> getUIProfileMap() {
        final Map<Integer, LessProfile> profiles = new com.intellij.util.containers.HashMap<Integer, LessProfile>();
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            final MyNode node = (MyNode) myRoot.getChildAt(i);
            final LessProfile lessProfile = ((LessProfileConfigurableForm) node.getConfigurable()).getEditableObject();
            profiles.put(lessProfile.getId(), lessProfile);
        }
        return profiles;
    }

    private int getNextId() {
        int id = -1;
        for (final LessProfile profile : getManagerProfileMap().values()) {
            if (profile.getId() > id) {
                id = profile.getId();
            }
        }
        for (final LessProfile profile : getUIProfileMap().values()) {
            if (profile.getId() > id) {
                id = profile.getId();
            }
        }
        return id + 1;
    }

    @Nullable
    private String askForProfileName(final String title, final String initialName) {
        final String message = UIBundle.message("action.new.less.profile.prompt.message");
        return Messages.showInputDialog(message, title, Messages.getQuestionIcon(), initialName, new InputValidator() {
            public boolean checkInput(final String newName) {
                if (newName == null || newName.length() == 0) {
                    return false;
                }
                // Don't allow duplicate profile names
                for (final LessProfile profile : getProfileMap().values()) {
                    if (StringUtils.equals(newName, profile.getName())) {
                        return false;
                    }
                }
                return true;
            }

            public boolean canClose(final String s) {
                return checkInput(s);
            }
        });
    }

    @Nullable
    private boolean confirmWarning(final String title, final String message) {
        return Messages.YES == Messages.showYesNoDialog(message, title, Messages.getWarningIcon());
    }

    public void setPromptButtonsEnabled(final boolean enabled) {
        for (final LessProfileConfigurableForm profileForm : profileForms) {
            profileForm.setPromptButtonEnabled(enabled);
        }
    }
    /*
     * Method overrides and interface implementations
     */

    @NotNull
    public String getId() {
        return "lessc.profiles";
    }

    @Nls
    public String getDisplayName() {
        return UIBundle.message("pref.display.name");
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

    public Runnable enableSearch(final String option) {
        return null;
    }

    @Override
    protected String getEmptySelectionString() {
        return UIBundle.message("profile.empty.selection");
    }

    @Override
    protected String getComponentStateKey() {
        return "LessCompiler.UI";
    }

    @Override
    protected MasterDetailsStateService getStateService() {
        return MasterDetailsStateService.getInstance(project);
    }

    public void addItemsChangeListener(final Runnable runnable) {
        addItemsChangeListener(new ItemsChangeListener() {
            public void itemChanged(@Nullable final Object deletedItem) {
                SwingUtilities.invokeLater(runnable);
            }

            public void itemsExternallyChanged() {
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

}
