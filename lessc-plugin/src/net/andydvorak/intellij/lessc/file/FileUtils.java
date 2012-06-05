package net.andydvorak.intellij.lessc.file;

import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileMoveEvent;
import org.lesscss.LessCompiler;
import org.lesscss.LessException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileUtils {

    // TODO: Get these values dynamically from the current Module or Project config
    private static final String LESS_DIR = "/Users/tkmax82/kiosk/Applications/Kiosk/Kiosk-Web/src/main/less";
    private static final String CSS_DIR = "/Users/tkmax82/kiosk/Applications/Kiosk/Kiosk-Web/src/main/webapp/media/css/ngKiosk";

    private final Map<String, File> fileMap;
    private final LessCompiler lessCompiler;

    public FileUtils() {
        fileMap = new HashMap<String, File>();
        lessCompiler = new LessCompiler();
    }

    public boolean isFileWatchable(final VirtualFileEvent virtualFileEvent) {
        return  virtualFileEvent.getFileName().endsWith(".less") &&
                virtualFileEvent.getFile().getCanonicalPath().startsWith(LESS_DIR);
    }

    public String getLessPath(final VirtualFileEvent virtualFileEvent) {
        return virtualFileEvent.getFile().getCanonicalPath();
    }

    public String getCssPath(final VirtualFileEvent virtualFileEvent) {
        return CSS_DIR + virtualFileEvent.getFile().getCanonicalPath()
                .replaceFirst(LESS_DIR, "")
                .replaceAll("\\.less$", ".css");
    }

    public String getNewPath(final VirtualFileMoveEvent virtualFileMoveEvent) {
        return virtualFileMoveEvent.getOldParent().getCanonicalPath() + "/" + virtualFileMoveEvent.getFileName();
    }

    public File getLessFile(final VirtualFileEvent virtualFileEvent) {
        return getCachedFile(getLessPath(virtualFileEvent));
    }

    public File getCssFile(final VirtualFileEvent virtualFileEvent) {
        return getCachedFile(getLessPath(virtualFileEvent));
    }

    public File getCachedFile(final String path) {
        if ( fileMap.containsKey(path) ) {
            return fileMap.get(path);
        } else {
            final File file = new File(path);
            fileMap.put(path, file);
            return file;
        }
    }

    public void removeCachedFile(final String path) {
        fileMap.remove(path);
    }

    public void compile(final File lessFile, final File cssFile) throws IOException, LessException {
        // TODO: Use proper logging mechanism
        System.out.println("contentsChanged: " + lessFile.getName());
        System.out.println("\t" + CSS_DIR);
        System.out.println("\t" + "lessPath: " + lessFile.getCanonicalPath());
        System.out.println("\t" + "cssPath: " + cssFile.getCanonicalPath());

        lessCompiler.compile(lessFile, cssFile);
    }

}
