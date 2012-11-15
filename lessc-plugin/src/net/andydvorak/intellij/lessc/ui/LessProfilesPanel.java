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

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.MasterDetailsComponent;
import com.intellij.openapi.ui.MasterDetailsStateService;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Conditions;
import com.intellij.util.PlatformIcons;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.messages.UIBundle;
import net.andydvorak.intellij.lessc.state.LessProfile;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class LessProfilesPanel extends MasterDetailsComponent implements SearchableConfigurable {

    @NotNull private final Project project;
    @NotNull private final LessManager lessManager;
    @NotNull private final AtomicBoolean isInitialized = new AtomicBoolean(false);

    private final List<CssConfigurableForm> cssConfigurableForms = new ArrayList<CssConfigurableForm>();

    public LessProfilesPanel(@NotNull final Project project) {
        this.project = project;
        this.lessManager = LessManager.getInstance(project);
        initTree();
    }

    @Override
    protected MasterDetailsStateService getStateService() {
        return MasterDetailsStateService.getInstance(project);
    }

    @Override
    protected String getComponentStateKey() {
        return "LessCompiler.UI";
    }

    protected void processRemovedItems() {
        Map<String, LessProfile> profiles = getAllProfiles();
        final List<LessProfile> deleted = new ArrayList<LessProfile>();
        for (LessProfile profile : lessManager.getProfiles()) {
            if (!profiles.containsValue(profile)) {
                deleted.add(profile);
            }
        }
        for (LessProfile profile : deleted) {
            lessManager.removeProfile(profile);
        }
    }

    protected boolean wasObjectStored(Object o) {
        return lessManager.getProfiles().contains(o);
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

    public void apply() throws ConfigurationException {
        final Set<String> profiles = new HashSet<String>();

        // Check for duplicate profile names
        for (int i = 0; i < myRoot.getChildCount(); i++) {
            MyNode node = (MyNode) myRoot.getChildAt(i);
            final String profileName = ((CssConfigurableForm) node.getConfigurable()).getEditableObject().getName();
            if (profiles.contains(profileName)) {
                selectNodeInTree(profileName);
                throw new ConfigurationException(UIBundle.message("duplicate.less.profile.name", profileName));
            }
            profiles.add(profileName);
        }

        super.apply();
    }

    public Map<String, LessProfile> getAllProfiles() {
        final Map<String, LessProfile> profiles = new com.intellij.util.containers.HashMap<String, LessProfile>();
        if (!isInitialized.get()) {
            for (LessProfile profile : lessManager.getProfiles()) {
                profiles.put(profile.getName(), profile);
            }
        } else {
            for (int i = 0; i < myRoot.getChildCount(); i++) {
                MyNode node = (MyNode) myRoot.getChildAt(i);
                final LessProfile lessProfile = ((CssConfigurableForm) node.getConfigurable()).getEditableObject();
                profiles.put(lessProfile.getName(), lessProfile);
            }
        }
        return profiles;
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        isInitialized.set(false);
    }

    @Nullable
    protected ArrayList<AnAction> createActions(boolean fromPopup) {
        final ArrayList<AnAction> result = new ArrayList<AnAction>();

        final String addText = UIBundle.message("action.add.less.profile.text");
        final String addDescription = UIBundle.message("action.add.less.profile.description");
        final String addPromptTitle = UIBundle.message("action.add.less.profile.prompt.title");

        result.add(new AnAction(addText, addDescription, PlatformIcons.ADD_ICON) {
            {
                registerCustomShortcutSet(CommonShortcuts.INSERT, myTree);
            }
            public void actionPerformed(AnActionEvent event) {
                final String name = askForProfileName(addPromptTitle, "");
                if (name == null) return;
                final LessProfile lessProfile = new LessProfile(name);
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
            public void actionPerformed(AnActionEvent event) {
                final String profileName = askForProfileName(copyPromptTitle, "");
                if (profileName == null) return;
                final LessProfile clone = new LessProfile((LessProfile) getSelectedObject());
                clone.setName(profileName);
                addProfileNode(clone);
            }

            public void update(AnActionEvent event) {
                super.update(event);
                event.getPresentation().setEnabled(getSelectedObject() != null);
            }
        });

        return result;
    }

    @Nullable
    private String askForProfileName(String title, String initialName) {
        final String message = UIBundle.message("action.new.less.profile.prompt.message");
        return Messages.showInputDialog(message, title, Messages.getQuestionIcon(), initialName, new InputValidator() {
            public boolean checkInput(String s) {
                return !getAllProfiles().containsKey(s) && s.length() > 0;
            }

            public boolean canClose(String s) {
                return checkInput(s);
            }
        });
    }

    private void addProfileNode(LessProfile lessProfile) {
        final CssConfigurableForm cssConfigurableForm = new CssConfigurableForm(project, lessProfile, this, TREE_UPDATER);
        cssConfigurableForm.setModified(true);
        cssConfigurableForms.add(cssConfigurableForm);
        final MyNode node = new MyNode(cssConfigurableForm);
        addNode(node, myRoot);
        selectNodeInTree(node);
    }

    private void reloadTree() {
        myRoot.removeAllChildren();
        cssConfigurableForms.clear();
        Collection<LessProfile> collection = lessManager.getProfiles();
        for (LessProfile profile : collection) {
            LessProfile clone = new LessProfile(profile);
            final CssConfigurableForm cssConfigurableForm = new CssConfigurableForm(project, clone, this, TREE_UPDATER);
            cssConfigurableForms.add(cssConfigurableForm);
            addNode(new MyNode(cssConfigurableForm), myRoot);
        }
        isInitialized.set(true);
    }

    public void setPromptButtonsEnabled(final boolean enabled) {
        for (CssConfigurableForm cssConfigurableForm : cssConfigurableForms) {
            cssConfigurableForm.setPromptButtonEnabled(enabled);
        }
    }

    public void reset() {
        reloadTree();
        super.reset();
    }

    @Override
    protected String getEmptySelectionString() {
        return UIBundle.message("profile.empty.selection");
    }

    public void addItemsChangeListener(final Runnable runnable) {
        addItemsChangeListener(new ItemsChangeListener() {
            public void itemChanged(@Nullable Object deletedItem) {
                SwingUtilities.invokeLater(runnable);
            }

            public void itemsExternallyChanged() {
                SwingUtilities.invokeLater(runnable);
            }
        });
    }

    @NotNull
    public String getId() {
        return "lessc.profiles";
    }

    public Runnable enableSearch(String option) {
        return null;
    }
    
}
