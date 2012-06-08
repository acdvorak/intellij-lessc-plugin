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
        return "reference.projectsettings.compiler.lessc";
    }

    @NotNull
    public String getId() {
        return "lessc";
    }

    public JComponent createComponent() {
        logger.info("createComponent()");
        return profilesPanel.createComponent();
    }

    public boolean isModified() {
        logger.info("isModified()");
        boolean res = false;
        if (profilesPanel != null) {
            res = profilesPanel.isModified();
        }

        logger.info("isModified() = " + res);

        return res;
    }

    public void apply() throws ConfigurationException {
        logger.info("apply()");
        if (profilesPanel != null) {
            profilesPanel.apply();
        }
    }

    public void reset() {
        logger.info("reset()");
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
