/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.andydvorak.intellij.lessc.options;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import net.andydvorak.intellij.lessc.LessProfile;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ExternalProfileOptionHelper {


    @Nullable
    public static List<LessProfile> loadOptions(File file) {
        try {
            List<LessProfile> profiles = new ArrayList<LessProfile>();
            Document doc = JDOMUtil.loadDocument(file);
            Element root = doc.getRootElement();
            if (root.getName().equals("component")) {
                final Element copyrightElement = root.getChild("profile");
                if (copyrightElement != null) extractNewNoticeAndKeyword(copyrightElement, profiles);
            } else {
                List list = root.getChildren("component");
                for (Object element : list) {
                    Element component = (Element) element;
                    String name = component.getAttributeValue("name");
                    if (name.equals("LessManager")) {
                        for (Object o : component.getChildren("profile")) {
                            extractNewNoticeAndKeyword((Element) o, profiles);
                        }
                    } else if (name.equals("profile")) {
                        extractNoticeAndKeyword(component, profiles);
                    }
                }
            }
            return profiles;
        } catch (Exception e) {
            logger.info(e);
            Messages.showErrorDialog(e.getMessage(), "Import Failure");
            return null;
        }
    }

    public static void extractNoticeAndKeyword(Element valueElement, List<LessProfile> profiles) {
        LessProfile profile = new LessProfile();
        boolean extract = false;
        for (Object l : valueElement.getChildren("LanguageOptions")) {
            if (((Element) l).getAttributeValue("name").equals("__TEMPLATE__")) {
                for (Object o1 : ((Element) l).getChildren("option")) {
                    extract |= extract(profile, (Element) o1);
                }
                break;
            }
        }
        if (extract) profiles.add(profile);
    }

    public static void extractNewNoticeAndKeyword(Element valueElement, List<LessProfile> profiles) {
        LessProfile profile = new LessProfile();
        boolean extract = false;
        for (Object l : valueElement.getChildren("option")) {
            extract |= extract(profile, (Element) l);
        }
        if (extract) profiles.add(profile);
    }

    private static boolean extract(final LessProfile profile, final Element el) {
        if (el.getAttributeValue("name").equals("notice")) {
            profile.setNotice(el.getAttributeValue("value"));
            return true;
        } else if (el.getAttributeValue("name").equals("keyword")) {
            profile.setKeyword(el.getAttributeValue("value"));
        } else if (el.getAttributeValue("name").equals("myName")) {
            profile.setName(el.getAttributeValue("value"));
        }
        return false;
    }


    private ExternalProfileOptionHelper() {
    }

    private static final Logger logger = Logger.getInstance(ExternalProfileOptionHelper.class.getName());
}
