package io.tcprest.commons;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class PropertyProcessor {
    public static FileInputStream getFileInputStream(String filePath) throws FileNotFoundException {
        if (PropertyParser.isRelativeFilePath(filePath)) {
            return new FileInputStream(getFilePath(filePath));
        }
        return new FileInputStream(filePath);
    }

    public static String getFilePath(String filePath) {
        URL url = PropertyProcessor.class.getClassLoader().getResource(PropertyParser.extractFilePath(filePath));
        return url.getPath();
    }
}
