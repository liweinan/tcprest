package cn.huiwings.tcprest.compression;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for GZIP compression/decompression.
 * Uses JDK built-in java.util.zip (zero external dependencies).
 *
 * Protocol format:
 * - Uncompressed: "0|" + data
 * - Compressed:   "1|" + gzip(data)
 *
 * The prefix allows backward compatibility and automatic detection.
 *
 * @author Weinan Li
 */
public class CompressionUtil {

    private static final String UNCOMPRESSED_PREFIX = "0|";
    private static final String COMPRESSED_PREFIX = "1|";
    private static final int PREFIX_LENGTH = 2;

    /**
     * Compress string data using GZIP.
     * Adds compression prefix to the result.
     *
     * @param data String to compress
     * @param config Compression configuration
     * @return Compressed string with prefix, or original with uncompressed prefix if not worth compressing
     */
    public static String compress(String data, CompressionConfig config) throws IOException {
        if (data == null) {
            return null;
        }

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        // Check if should compress based on threshold
        if (!config.shouldCompress(dataBytes.length)) {
            return UNCOMPRESSED_PREFIX + data;
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(byteArrayOutputStream) {
            {
                def.setLevel(config.getCompressionLevel());
            }
        }) {
            gzipOutputStream.write(dataBytes);
        }

        byte[] compressed = byteArrayOutputStream.toByteArray();

        // If compression doesn't reduce size significantly, don't use it
        if (compressed.length >= dataBytes.length * 0.9) {
            return UNCOMPRESSED_PREFIX + data;
        }

        // Encode compressed bytes as Base64 for safe string transmission
        String base64 = java.util.Base64.getEncoder().encodeToString(compressed);
        return COMPRESSED_PREFIX + base64;
    }

    /**
     * Decompress string data.
     * Automatically detects compressed vs uncompressed based on prefix.
     *
     * @param data String to decompress (with prefix)
     * @return Decompressed string
     */
    public static String decompress(String data) throws IOException {
        if (data == null || data.length() < PREFIX_LENGTH) {
            return data;
        }

        String prefix = data.substring(0, PREFIX_LENGTH);
        String payload = data.substring(PREFIX_LENGTH);

        if (UNCOMPRESSED_PREFIX.equals(prefix)) {
            // Data is not compressed
            return payload;
        } else if (COMPRESSED_PREFIX.equals(prefix)) {
            // Data is compressed - decompress it
            byte[] compressed = java.util.Base64.getDecoder().decode(payload);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressed);
                 GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream)) {

                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzipInputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, len);
                }
            }

            return byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
        } else {
            // No prefix - assume legacy uncompressed format
            return data;
        }
    }

    /**
     * Check if data is compressed (has compression prefix)
     */
    public static boolean isCompressed(String data) {
        if (data == null || data.length() < PREFIX_LENGTH) {
            return false;
        }
        return data.startsWith(COMPRESSED_PREFIX);
    }

    /**
     * Get compression ratio as percentage (0-100).
     * Higher is better compression.
     *
     * @param original Original data
     * @param compressed Compressed data (with prefix)
     * @return Compression ratio percentage (e.g., 75 means 75% reduction)
     */
    public static double getCompressionRatio(String original, String compressed) {
        if (original == null || compressed == null || !isCompressed(compressed)) {
            return 0.0;
        }

        int originalSize = original.getBytes(StandardCharsets.UTF_8).length;
        // Subtract prefix length and decode base64 to get actual compressed size
        String payload = compressed.substring(PREFIX_LENGTH);
        int compressedSize = java.util.Base64.getDecoder().decode(payload).length;

        if (originalSize == 0) {
            return 0.0;
        }

        return ((double) (originalSize - compressedSize) / originalSize) * 100.0;
    }
}
