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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LessProjectConfigurable extends SearchableConfigurable.Parent.Abstract implements Configurable.NoScroll {

    private static final Icon icon = null; // IconLoader.getIcon("/resources/lessc32x32.png");
    private static final Logger logger = Logger.getInstance(LessProjectConfigurable.class.getName());

    private LessProfilesPanel profilesPanel;

    public LessProjectConfigurable(final Project project) {
        this.profilesPanel = new LessProfilesPanel(project);
    }

    public String getDisplayName() {
        return "LESS Compiler";
    }

    public Icon getIcon() {
        return icon;
    }

    public String getHelpTopic() {
        return null;
    }

    @NotNull
    public String getId() {
        return "lessc";
    }

    public JComponent createComponent() {
        logger.debug("createComponent()");
        return profilesPanel.createComponent();
    }

    public boolean isModified() {
        logger.debug("isModified()");
        boolean res = false;
        if (profilesPanel != null) {
            res = profilesPanel.isModified();
        }

        logger.debug("isModified() = " + res);

        return res;
    }

    public void apply() throws ConfigurationException {
        logger.debug("apply()");
        if (profilesPanel != null) {
            profilesPanel.apply();
        }
    }

    public void reset() {
        logger.debug("reset()");
        if (profilesPanel != null) {
            profilesPanel.reset();
        }
    }

    public void disposeUIResources() {
        profilesPanel = null;
    }

    public boolean hasOwnContent() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    public Runnable enableSearch(String option) {
        return null;
    }

    /**
     * Child configurables
     * @return
     */
    protected Configurable[] buildConfigurables() {
        return new Configurable[] {  };
    }
    
}
