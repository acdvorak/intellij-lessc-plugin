package net.andydvorak.intellij.lessc.ui;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.NamedConfigurable;
import com.intellij.openapi.util.Comparing;
import net.andydvorak.intellij.lessc.LessManager;
import net.andydvorak.intellij.lessc.LessProfile;
import net.andydvorak.intellij.lessc.file.EntityUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CssConfigurablePanel extends NamedConfigurable<LessProfile> {
    private final LessProfile myLessProfile;
    private JPanel myWholePanel;

    private final Project myProject;
    private boolean myModified;

    private String myDisplayName;
    private JEditorPane myCopyrightPane;
    private JButton myValidateButton;
    private JTextField myKeywordTf;
    private JTextField myAllowReplaceTextField;

    public CssConfigurablePanel(Project project, LessProfile lessProfile, Runnable updater) {
        super(true, updater);
        myProject = project;
        myLessProfile = lessProfile;
        myDisplayName = myLessProfile.getName();
    }

    public void setDisplayName(String s) {
        myLessProfile.setName(s);
    }

    public LessProfile getEditableObject() {
        return myLessProfile;
    }

    public String getBannerSlogan() {
        return myLessProfile.getName();
    }

    public JComponent createOptionsPanel() {
        myCopyrightPane.setFont(EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN));
        myValidateButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
//                try {
//                    VelocityHelper.verify(myCopyrightPane.getText());
//                    Messages.showInfoMessage(myProject, "Velocity template is valid.", "Validation");
//                }
//                catch (Exception e1) {
//                    Messages.showInfoMessage(myProject, "Velocity template contains error:\n" + e1.getMessage(), "Validation");
//                }
            }
        });
        return myWholePanel;
    }

    @Nls
    public String getDisplayName() {
        return myLessProfile.getName();
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
        return myModified ||
                !Comparing.strEqual(EntityUtil.encode(myCopyrightPane.getText().trim()), myLessProfile.getNotice()) ||
                !Comparing.strEqual(myKeywordTf.getText().trim(), myLessProfile.getKeyword()) ||
                !Comparing.strEqual(myAllowReplaceTextField.getText().trim(), myLessProfile.getAllowReplaceKeyword()) ||
                !Comparing.strEqual(myDisplayName, myLessProfile.getName());
    }

    public void apply() throws ConfigurationException {
        myLessProfile.setNotice(EntityUtil.encode(myCopyrightPane.getText().trim()));
        myLessProfile.setKeyword(myKeywordTf.getText());
        myLessProfile.setAllowReplaceKeyword(myAllowReplaceTextField.getText());
        LessManager.getInstance(myProject).replaceProfile(myDisplayName, myLessProfile);
        myDisplayName = myLessProfile.getName();
        myModified = false;
    }

    public void reset() {
        myDisplayName = myLessProfile.getName();
        myCopyrightPane.setText(EntityUtil.decode(myLessProfile.getNotice()));
        myKeywordTf.setText(myLessProfile.getKeyword());
        myAllowReplaceTextField.setText(myLessProfile.getAllowReplaceKeyword());
    }

    public void disposeUIResources() {
    }

    public void setModified(boolean modified) {
        myModified = modified;
    }

}
