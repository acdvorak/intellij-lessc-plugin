package net.andydvorak.intellij.lessc.ui;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class LessProjectConfigurable extends SearchableConfigurable.Parent.Abstract implements Configurable.NoScroll {

    private final Project project;
    private ProjectSettingsPanel optionsPanel = null;

    private static final Icon icon = null; // IconLoader.getIcon("/resources/copyright32x32.png");

    private static final Logger logger = Logger.getInstance(LessProjectConfigurable.class.getName());
    private final LessProfilesPanel myProfilesPanel;


    public LessProjectConfigurable(Project project) {
        this.project = project;
        myProfilesPanel = new LessProfilesPanel(project);
    }

    public String getDisplayName() {
        return "LESS Compiler";
    }

    public Icon getIcon() {
        return icon;
    }

    public String getHelpTopic() {
        return getId();
    }

    public JComponent createComponent() {
        logger.info("createComponent()");
        optionsPanel = new ProjectSettingsPanel(project, myProfilesPanel);
        return optionsPanel.getMainComponent();
    }

    public boolean isModified() {
        logger.info("isModified()");
        boolean res = false;
        if (optionsPanel != null) {
            res = optionsPanel.isModified();
        }

        logger.info("isModified() = " + res);

        return res;
    }

    public void apply() throws ConfigurationException {
        logger.info("apply()");
        if (optionsPanel != null) {
            optionsPanel.apply();
        }
    }

    public void reset() {
        logger.info("reset()");
        if (optionsPanel != null) {
            optionsPanel.reset();
        }
    }

    public void disposeUIResources() {
        optionsPanel = null;
    }

    public boolean hasOwnContent() {
        return true;
    }

    public boolean isVisible() {
        return true;
    }

    @NotNull
    public String getId() {
        return "lessc";
    }

    public Runnable enableSearch(String option) {
        return null;
    }

    protected Configurable[] buildConfigurables() {
        return new Configurable[] { myProfilesPanel /*, new CopyrightFormattingConfigurable(project) */ };
    }
    
}
