package cn.huiwings.tcprest.security;

import cn.huiwings.tcprest.commons.Base64;
import cn.huiwings.tcprest.exception.SecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

/**
 * Security utilities for TcpRest protocol.
 *
 * <p>Provides secure encoding/decoding and integrity verification:
 * <ul>
 *   <li>URL-safe Base64 encoding for all protocol components</li>
 *   <li>CRC32 checksum for detecting accidental corruption</li>
 *   <li>HMAC-SHA256 for cryptographic message authentication</li>
 * </ul>
 *
 * <p>All methods are thread-safe and stateless.
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class ProtocolSecurity {

    /** Checksum prefix separator */
    private static final String CHECKSUM_PREFIX = "CHK:";

    /**
     * Encodes a protocol component using URL-safe Base64.
     *
     * <p>URL-safe variant replaces '+' with '-' and '/' with '_', and omits padding '='.
     * This prevents issues with URL encoding and makes the protocol more robust.
     *
     * @param component the component to encode (e.g., "ClassName/methodName")
     * @return Base64-encoded string, never null
     * @throws IllegalArgumentException if component is null
     */
    public static String encodeComponent(String component) {
        if (component == null) {
            throw new IllegalArgumentException("Component to encode cannot be null");
        }
        return Base64.encode(component.getBytes(StandardCharsets.UTF_8))
                .replace('+', '-')
                .replace('/', '_')
                .replace("=", "");
    }

    /**
     * Decodes a URL-safe Base64 encoded component.
     *
     * @param encoded the Base64-encoded string
     * @return decoded string
     * @throws SecurityException if decoding fails (invalid format)
     */
    public static String decodeComponent(String encoded) {
        if (encoded == null) {
            throw new SecurityException("Encoded component cannot be null");
        }

        // Empty string is valid (represents empty content, e.g., no parameters)
        if (encoded.isEmpty()) {
            return "";
        }

        try {
            // Restore standard Base64 format
            String standard = encoded.replace('-', '+').replace('_', '/');

            // Add padding if needed
            int padding = (4 - standard.length() % 4) % 4;
            for (int i = 0; i < padding; i++) {
                standard += "=";
            }

            byte[] decoded = Base64.decode(standard);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SecurityException("Failed to decode component: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates checksum for a message.
     *
     * @param message message to checksum
     * @param config security configuration
     * @return checksum string in format "CHK:value", or empty string if checksum disabled
     * @throws SecurityException if checksum calculation fails
     */
    public static String calculateChecksum(String message, SecurityConfig config) {
        if (config == null || !config.isChecksumEnabled()) {
            return "";
        }

        switch (config.getChecksumAlgorithm()) {
            case CRC32:
                return CHECKSUM_PREFIX + calculateCRC32(message);
            case HMAC_SHA256:
                return CHECKSUM_PREFIX + calculateHMAC(message, config.getHmacSecret());
            default:
                return "";
        }
    }

    /**
     * Verifies message checksum.
     *
     * @param message message without checksum
     * @param receivedChecksum received checksum in format "CHK:value"
     * @param config security configuration
     * @return true if checksum is valid or disabled
     * @throws SecurityException if checksum verification fails
     */
    public static boolean verifyChecksum(String message, String receivedChecksum, SecurityConfig config) {
        if (config == null || !config.isChecksumEnabled()) {
            return true; // Checksum disabled, always valid
        }

        if (receivedChecksum == null || receivedChecksum.isEmpty()) {
            throw new SecurityException("Checksum enabled but not provided in message");
        }

        if (!receivedChecksum.startsWith(CHECKSUM_PREFIX)) {
            throw new SecurityException("Invalid checksum format, expected CHK:value");
        }

        String expectedChecksum = calculateChecksum(message, config);
        return expectedChecksum.equals(receivedChecksum);
    }

    /**
     * Splits a protocol message into content and checksum.
     *
     * <p>Expected format: "content|CHK:value" or just "content" if no checksum.
     *
     * @param message full message with optional checksum
     * @return array with [content, checksum], where checksum is empty string if not present
     */
    public static String[] splitChecksum(String message) {
        if (message == null) {
            return new String[]{"", ""};
        }

        int lastPipe = message.lastIndexOf('|');
        if (lastPipe == -1) {
            return new String[]{message, ""};
        }

        String possibleChecksum = message.substring(lastPipe + 1);
        if (possibleChecksum.startsWith(CHECKSUM_PREFIX)) {
            return new String[]{message.substring(0, lastPipe), possibleChecksum};
        } else {
            return new String[]{message, ""};
        }
    }

    /**
     * Validates that a class name matches Java naming conventions.
     *
     * <p>Prevents path traversal attacks (../) and special characters.
     *
     * @param className class name to validate
     * @return true if class name is safe
     */
    public static boolean isValidClassName(String className) {
        if (className == null || className.isEmpty()) {
            return false;
        }

        // Check for path traversal
        if (className.contains("..")) {
            return false;
        }

        // Must match Java class name pattern: letter/$ followed by letters/digits/$/_
        // Allows dots for package names
        return className.matches("^[a-zA-Z_$][a-zA-Z0-9_$.]*$");
    }

    /**
     * Validates that a method name matches Java naming conventions.
     *
     * @param methodName method name to validate
     * @return true if method name is safe
     */
    public static boolean isValidMethodName(String methodName) {
        if (methodName == null || methodName.isEmpty()) {
            return false;
        }

        // Must match Java method name pattern
        return methodName.matches("^[a-zA-Z_$][a-zA-Z0-9_$]*$");
    }

    // ========== Private Helper Methods ==========

    /**
     * Calculates CRC32 checksum.
     */
    private static String calculateCRC32(String message) {
        CRC32 crc = new CRC32();
        crc.update(message.getBytes(StandardCharsets.UTF_8));
        return Long.toHexString(crc.getValue());
    }

    /**
     * Calculates HMAC-SHA256.
     */
    private static String calculateHMAC(String message, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

            // Convert to hex string
            StringBuilder sb = new StringBuilder();
            for (byte b : hmacBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new SecurityException("Failed to calculate HMAC: " + e.getMessage(), e);
        }
    }
}
