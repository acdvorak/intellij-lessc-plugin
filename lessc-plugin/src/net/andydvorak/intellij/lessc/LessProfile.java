package net.andydvorak.intellij.lessc;

import com.intellij.profile.ProfileEx;
import net.andydvorak.intellij.lessc.file.EntityUtil;

public class LessProfile extends ProfileEx {

    public static final String DEFAULT_COPYRIGHT_NOTICE =
            EntityUtil.encode("Copyright (c) $today.year. Lorem ipsum dolor sit amet, consectetur adipiscing elit. \n" +
                    "Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan. \n" +
                    "Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna. \n" +
                    "Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus. \n" +
                    "Vestibulum commodo. Ut rhoncus gravida arcu. ");

    public String notice = DEFAULT_COPYRIGHT_NOTICE;
    public String keyword = EntityUtil.encode("Copyright");
    public String allowReplaceKeyword = "";

    //read external
    public LessProfile() {
        super("");
    }

    public LessProfile(String profileName) {
        super(profileName);
    }

    public String getNotice() {
        return notice;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setNotice(String text) {
        notice = text;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getAllowReplaceKeyword() {
        return allowReplaceKeyword;
    }

    public void setAllowReplaceKeyword(String allowReplaceKeyword) {
        this.allowReplaceKeyword = allowReplaceKeyword;
    }

}
