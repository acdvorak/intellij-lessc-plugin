package net.andydvorak.intellij.lessc;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerAdapter;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.containers.HashMap;
import com.intellij.util.text.UniqueNameGenerator;
import com.intellij.util.xmlb.annotations.Transient;
import net.andydvorak.intellij.lessc.file.LessFileUtils;
import net.andydvorak.intellij.lessc.file.LessFileWatcherService;
import net.andydvorak.intellij.lessc.file.VirtualFileListenerImpl;
import net.andydvorak.intellij.lessc.options.LanguageOptions;
import net.andydvorak.intellij.lessc.options.Options;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lesscss.LessException;

import java.io.File;
import java.io.IOException;
import java.util.*;

@State(
        name = "LessManager",
        storages = {
                @Storage(file = "$PROJECT_FILE$"),
                @Storage(file = "$PROJECT_CONFIG_DIR$/lessc/", scheme = StorageScheme.DIRECTORY_BASED,
                    stateSplitter = LessManager.LessStateSplitter.class)
        }
)
public class LessManager extends AbstractProjectComponent implements JDOMExternalizable, PersistentStateComponent<Element>, LessFileWatcherService {

    private static final Logger LOG = Logger.getInstance("#" + LessManager.class.getName());

    @NonNls
    private static final String PROFILE = "profile";
    @NonNls
    private static final String MODULE2COPYRIGHT = "module2copyright";
    @NonNls
    private static final String ELEMENT = "element";
    @NonNls
    private static final String MODULE = "module";
    @NonNls
    private static final String DEFAULT = "default";
    
    @Nullable
    private LessProfile myDefaultProfile = null;

    @Transient
    private final VirtualFileListenerImpl virtualFileListener;

    @Transient
    private final Project project;

    private final Map<String, LessProfile> myProfiles = new HashMap<String, LessProfile>();

    private final LinkedHashMap<String, String> myModule2Copyrights = new LinkedHashMap<String, String>();

    private final Options myOptions = new Options();

    public LessManager(final Project project) {
        super(project);
        this.project = project;
        this.virtualFileListener = new VirtualFileListenerImpl(this);
    }
    
    public static LessManager getInstance(final Project project) {
        return project.getComponent(LessManager.class);
    }

    public void projectOpened() {
        // andrew.dvorak
//        if (myProject != null) {
//            FileEditorManagerListener listener = new FileEditorManagerAdapter() {
//                public void fileOpened(FileEditorManager fileEditorManager, VirtualFile virtualFile) {
//                    if (virtualFile.isWritable() && NewFileTracker.getInstance().contains(virtualFile)) {
//                        NewFileTracker.getInstance().remove(virtualFile);
//                        if (FileTypeUtil.getInstance().isSupportedFile(virtualFile)) {
//                            final Module module = ProjectRootManager.getInstance(myProject).getFileIndex().getModuleForFile(virtualFile);
//                            if (module != null) {
//                                final PsiFile file = PsiManager.getInstance(myProject).findFile(virtualFile);
//                                if (file != null) {
//                                    ApplicationManager.getApplication().invokeLater(new Runnable() {
//                                        public void run() {
//                                            if (file.isValid() && file.isWritable()) {
//                                                final LessProfile opts = LessManager.getInstance(myProject).getCopyrightOptions(file);
//                                                if (opts != null) {
//                                                    new UpdateCopyrightProcessor(myProject, module, file).run();
//                                                }
//                                            }
//                                        }
//                                    }, ModalityState.NON_MODAL);
//                                }
//                            }
//                        }
//                    }
//                }
//            };
//
//            FileEditorManager.getInstance(myProject).addFileEditorManagerListener(listener, myProject);
//        }
    }

    @NonNls
    @NotNull
    public String getComponentName() {
        return "LessManager";
    }

    public Map<String, String> getProfileMapping() {
        return myModule2Copyrights;
    }

    public void setDefaultProfile(@Nullable LessProfile copyright) {
        myDefaultProfile = copyright;
    }

    @Nullable
    public LessProfile getDefaultProfile() {
        return myDefaultProfile;
    }

    public void addProfile(LessProfile lessProfile) {
        myProfiles.put(lessProfile.getName(), lessProfile);
    }

    public void removeProfile(LessProfile lessProfile) {
        myProfiles.values().remove(lessProfile);
        for (Iterator<String> it = myModule2Copyrights.keySet().iterator(); it.hasNext();) {
            final String profileName = myModule2Copyrights.get(it.next());
            if (profileName.equals(lessProfile.getName())) {
                it.remove();
            }
        }
    }

    public void clearProfiles() {
        myDefaultProfile = null;
        myProfiles.clear();
        myModule2Copyrights.clear();
    }

    public void mapProfile(String scopeName, String copyrightProfileName) {
        myModule2Copyrights.put(scopeName, copyrightProfileName);
    }

    public void unmapProfile(String scopeName) {
        myModule2Copyrights.remove(scopeName);
    }

    public Collection<LessProfile> getProfiles() {
        return myProfiles.values();
    }

    @Nullable
    public LessProfile getCopyrightOptions(@NotNull PsiFile file) {
        final VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null || myOptions.getOptions(virtualFile.getFileType().getName()).getFileTypeOverride() == LanguageOptions.NO_COPYRIGHT) return null;
        final DependencyValidationManager validationManager = DependencyValidationManager.getInstance(myProject);
        for (String scopeName : myModule2Copyrights.keySet()) {
            final NamedScope namedScope = validationManager.getScope(scopeName);
            if (namedScope != null) {
                final PackageSet packageSet = namedScope.getValue();
                if (packageSet != null) {
                    if (packageSet.contains(file, validationManager)) {
                        final LessProfile profile = myProfiles.get(myModule2Copyrights.get(scopeName));
                        if (profile != null) {
                            return profile;
                        }
                    }
                }
            }
        }
        return myDefaultProfile != null ? myDefaultProfile : null;
    }

    public Options getOptions() {
        return myOptions;
    }

    public void replaceProfile(String displayName, LessProfile lessProfile) {
        if (myDefaultProfile != null && Comparing.strEqual(myDefaultProfile.getName(), displayName)) {
            myDefaultProfile = lessProfile;
        }
        myProfiles.remove(displayName);
        addProfile(lessProfile);
    }

    public void readExternal(Element element) throws InvalidDataException {
        clearProfiles();
        final Element module2copyright = element.getChild(MODULE2COPYRIGHT);
        if (module2copyright != null) {
            for (Object o : module2copyright.getChildren(ELEMENT)) {
                final Element el = (Element)o;
                final String moduleName = el.getAttributeValue(MODULE);
                final String copyrightName = el.getAttributeValue(PROFILE);
                myModule2Copyrights.put(moduleName, copyrightName);
            }
        }
        for (Object o : element.getChildren(PROFILE)) {
            final LessProfile copyrightProfile = new LessProfile();
            copyrightProfile.readExternal((Element)o);
            myProfiles.put(copyrightProfile.getName(), copyrightProfile);
        }
        myDefaultProfile = myProfiles.get(element.getAttributeValue(DEFAULT));
        myOptions.readExternal(element);
    }

    public void writeExternal(Element element) throws WriteExternalException {
        for (LessProfile copyright : myProfiles.values()) {
            final Element copyrightElement = new Element(PROFILE);
            copyright.writeExternal(copyrightElement);
            element.addContent(copyrightElement);
        }
        final Element map = new Element(MODULE2COPYRIGHT);
        for (String moduleName : myModule2Copyrights.keySet()) {
            final Element setting = new Element(ELEMENT);
            setting.setAttribute(MODULE, moduleName);
            setting.setAttribute(PROFILE, myModule2Copyrights.get(moduleName));
            map.addContent(setting);
        }
        element.addContent(map);
        element.setAttribute(DEFAULT, myDefaultProfile != null ? myDefaultProfile.getName() : "");
        myOptions.writeExternal(element);
    }


    public Element getState() {
        try {
            final Element e = new Element("settings");
            writeExternal(e);
            return e;
        }
        catch (WriteExternalException e1) {
            LOG.error(e1);
            return null;
        }
    }

    public void loadState(Element state) {
        try {
            readExternal(state);
        }
        catch (InvalidDataException e) {
            LOG.error(e);
        }
    }

    public static class LessStateSplitter implements StateSplitter {
        public List<Pair<Element, String>> splitState(Element e) {
            final UniqueNameGenerator generator = new UniqueNameGenerator();
            final List<Pair<Element, String>> result = new ArrayList<Pair<Element, String>>();

            final Element[] elements = JDOMUtil.getElements(e);
            for (Element element : elements) {
                if (element.getName().equals("lessc")) {
                    element.detach();

                    String profileName = null;
                    final Element[] options = JDOMUtil.getElements(element);
                    for (Element option : options) {
                        if (option.getName().equals("option") && option.getAttributeValue("name").equals("myName")) {
                            profileName = option.getAttributeValue("value");
                        }
                    }

                    assert profileName != null;

                    final String name = generator.generateUniqueName(FileUtil.sanitizeFileName(profileName)) + ".xml";
                    result.add(new Pair<Element, String>(element, name));
                }
            }
            result.add(new Pair<Element, String>(e, generator.generateUniqueName("profiles_settings") + ".xml"));
            return result;
        }

        public void mergeStatesInto(Element target, Element[] elements) {
            for (Element element : elements) {
                if (element.getName().equals("lessc")) {
                    element.detach();
                    target.addContent(element);
                }
                else {
                    final Element[] states = JDOMUtil.getElements(element);
                    for (Element state : states) {
                        state.detach();
                        target.addContent(state);
                    }
                    for (Object attr : element.getAttributes()) {
                        target.setAttribute((Attribute)((Attribute)attr).clone());
                    }
                }
            }
        }
    }

    public void handleFileEvent(final VirtualFileEvent virtualFileEvent) {
        final LessFileUtils lessFileUtils = LessFileUtils.getInstance();

        if ( lessFileUtils.isSupported(virtualFileEvent) ) {
            final File lessFile = lessFileUtils.getLessFile(virtualFileEvent);
            final File cssFile = lessFileUtils.getLessFile(virtualFileEvent);

//            Document document = FileDocumentManager.getInstance().getDocument(virtualFileEvent.getFile());
//            document.

//            final Project project = Project.

//            RunBackgroundable.run(new Task() {

            try {
                lessFileUtils.compile(lessFile, cssFile);
                handleSuccess(lessFile, cssFile);
            } catch (IOException e) {
                e.printStackTrace();  // TODO: Use proper logging mechanism
                handleException(e, virtualFileEvent);
            } catch (LessException e) {
                e.printStackTrace();  // TODO: Use proper logging mechanism
                handleException(e, virtualFileEvent);
            } finally {

            }
//            });
        }
    }

    public void handleFileEvent(final VirtualFileMoveEvent virtualFileMoveEvent) {
        final LessFileUtils lessFileUtils = LessFileUtils.getInstance();

        // TODO: Implement this w/ intelligent cleanup of CSS file

        lessFileUtils.removeCachedFile(lessFileUtils.getNewPath(virtualFileMoveEvent));

//        handleFileEvent((VirtualFileEvent) virtualFileMoveEvent);
    }

    public void handleFileEvent(final VirtualFileCopyEvent virtualFileCopyEvent) {
        // TODO: Implement this

//        handleFileEvent((VirtualFileEvent) virtualFileCopyEvent);
    }

    public void handleDeletedFileEvent(final VirtualFileEvent virtualFileEvent) {
        final LessFileUtils lessFileUtils = LessFileUtils.getInstance();

        // TODO: Implement this w/ intelligent cleanup of CSS file

        lessFileUtils.removeCachedFile(virtualFileEvent.getFile().getCanonicalPath());
    }

    private void handleException(final Exception e, final VirtualFileEvent virtualFileEvent) {
        showBalloon(virtualFileEvent.getFileName() + ": " + e.getLocalizedMessage(), MessageType.ERROR);
    }

    private void handleSuccess(final File lessFile, final File cssFile) {
        showBalloon(lessFile.getName() + " successfully compiled to CSS", MessageType.INFO);

        final VirtualFile virtualCssFile = LocalFileSystem.getInstance().findFileByIoFile(cssFile);

        if ( virtualCssFile != null ) {
            // TODO: performance of synchronous vs. asynchronous?
            virtualCssFile.refresh(false, false);
        }
    }

    private void showBalloon(final String message, final MessageType messageType) {
        // For application-level use when we don't have access to the current Project
//        final DataContext dataContext = DataManager.getInstance().getDataContext();
//        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(DataKeys.PROJECT.getData(dataContext));

        final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);

        if ( statusBar != null ) {
            JBPopupFactory.getInstance()
                    .createHtmlTextBalloonBuilder(message, messageType, null)
                    .setFadeoutTime(7500)
                    .createBalloon()
                    .show(RelativePoint.getCenterOf(statusBar.getComponent()), Balloon.Position.atRight);
        }
    }

}
