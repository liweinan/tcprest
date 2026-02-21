package cn.huiwings.tcprest.classloader;

import java.util.logging.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Load class from file system, it will load the interfaces and superclasses of target class automatically.
 * Please ensure all the necessary resources are located in dir/sub-dir of the target class.
 *
 * @author Weinan Li
 */
public class FilePathClassLoader extends ClassLoader {
    Logger logger = Logger.getLogger(FilePathClassLoader.class.getName());

    String[] dirs;

    public FilePathClassLoader(String path) {
        dirs = path.split(System.getProperty("path.separator"));
        String[] _dirs = dirs.clone();
        for (String dir : _dirs) {
            extendClasspath(dir);
        }
    }

    public void extendClasspath(String path) {
        String[] segments = path.split("/");
        String[] exDirs = new String[segments.length];
        for (int i = 0; i < (segments.length); i++) {
            exDirs[i] = popd(segments, i);
        }

        String[] newDirs = new String[dirs.length + exDirs.length];
        System.arraycopy(dirs, 0, newDirs, 0, dirs.length);
        System.arraycopy(exDirs, 0, newDirs, dirs.length, exDirs.length);
        dirs = newDirs;
    }

    private String popd(String[] pathSegments, int level) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < level; i++) {
            path.append(pathSegments[i]).append("/");
        }
        return path.toString();
    }

    @Override
    public synchronized Class findClass(String canonicalName)
            throws ClassNotFoundException {

        for (String dir : dirs) {
            byte[] buf = getClassData(dir, canonicalName);
            if (buf != null) {
                logger.fine("Loading '" + canonicalName + "' from: " + dir);
                return defineClass(canonicalName, buf, 0, buf.length);
            }
        }
        throw new ClassNotFoundException();
    }

    protected byte[] getClassData(String directory, String canonicalName) {
        String[] tokens = canonicalName.split("\\.");
        String className = tokens[tokens.length - 1];
        List<String> tokenList = Arrays.asList(tokens);
        tokenList.subList(0, tokens.length - 1).toArray(tokens);

        byte[] buf = null;

        for (int i = 0; i < tokens.length; i++) {
            StringBuilder pathSeg = new StringBuilder();
            for (int j = 0; j < i; j++) {
                pathSeg.append(tokens[j]).append("/");
            }
            String classFile = directory + "/" + pathSeg.toString() + className + ".class";
            logger.fine("searching for class: " + className);
            File f = (new File(classFile));
            int classSize = Math.toIntExact(f.length());
            buf = new byte[classSize];
            try {
                FileInputStream filein = new FileInputStream(classFile);
                classSize = filein.read(buf);
                filein.close();
                logger.fine("found class: " + className);
                break;
            } catch (FileNotFoundException e) {
                continue;
            } catch (IOException e) {
                continue;
            }

        }

        return buf;
    }
}