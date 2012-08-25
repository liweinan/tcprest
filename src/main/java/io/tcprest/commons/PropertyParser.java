package io.tcprest.commons;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class PropertyParser {
    public static boolean isRelativeFilePath(String filePath) {
        return filePath != null && filePath.startsWith("classpath:");
    }

    public static String extractFilePath(String filePath) {
        if (!isRelativeFilePath(filePath)) return filePath;

        return filePath.substring("classpath:".length(), filePath.length());
    }
}
