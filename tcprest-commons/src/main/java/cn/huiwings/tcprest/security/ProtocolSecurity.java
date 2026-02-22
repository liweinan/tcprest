package cn.huiwings.tcprest.security;

import cn.huiwings.tcprest.exception.SecurityException;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.CRC32;

/**
 * Security utilities for TcpRest protocol.
 *
 * <p>Provides secure encoding/decoding and integrity verification:
 * <ul>
 *   <li>URL-safe Base64 encoding for all protocol components</li>
 *   <li>CHK: CRC32/HMAC for integrity</li>
 *   <li>SIG: RSA-SHA256 for origin authentication (GPG via optional modules)</li>
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

    /** Signature value prefix for RSA (wire format: SIG:RSA:base64) */
    private static final String SIG_RSA_PREFIX = "RSA:";

    /** Registry of custom signature handlers (e.g. "GPG" from tcprest-pgp) */
    private static final Map<String, SignatureHandler> SIGNATURE_HANDLERS = new ConcurrentHashMap<>();

    /**
     * Registers a custom signature handler. Used by optional modules (e.g. tcprest-pgp).
     *
     * @param algorithmName name used in wire format (e.g. "GPG")
     * @param handler handler implementation
     */
    public static void registerSignatureHandler(String algorithmName, SignatureHandler handler) {
        if (algorithmName == null || algorithmName.isEmpty() || handler == null) {
            throw new IllegalArgumentException("Algorithm name and handler cannot be null or empty");
        }
        SIGNATURE_HANDLERS.put(algorithmName, handler);
    }

    /**
     * Unregisters a custom signature handler.
     *
     * @param algorithmName name used when registered
     */
    public static void unregisterSignatureHandler(String algorithmName) {
        SIGNATURE_HANDLERS.remove(algorithmName);
    }

    static SignatureHandler getSignatureHandler(String algorithmName) {
        return SIGNATURE_HANDLERS.get(algorithmName);
    }

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
        return Base64.getEncoder().encodeToString(component.getBytes(StandardCharsets.UTF_8))
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

            byte[] decoded = Base64.getDecoder().decode(standard);
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
     * Result of parsing optional trailing CHK and SIG segments.
     * Order on wire is content|CHK:value|SIG:value; parsing strips from right.
     */
    public static final class TrailingSegments {
        private final String content;
        private final String chkSegment;
        private final String sigSegment;

        public TrailingSegments(String content, String chkSegment, String sigSegment) {
            this.content = content != null ? content : "";
            this.chkSegment = chkSegment != null ? chkSegment : "";
            this.sigSegment = sigSegment != null ? sigSegment : "";
        }

        public String getContent() {
            return content;
        }

        public String getChkSegment() {
            return chkSegment;
        }

        public String getSigSegment() {
            return sigSegment;
        }

        /** Payload that was signed when SIG is present: content, or content|CHK:value */
        public String getSignedPayload() {
            if (chkSegment.isEmpty()) {
                return content;
            }
            return content + "|" + chkSegment;
        }
    }

    /**
     * Parses a protocol message into content and optional CHK/SIG segments.
     * Strips from the right: last segment SIG: then CHK:, remainder is content.
     *
     * @param message full message (may end with |CHK:value and/or |SIG:value)
     * @return TrailingSegments with content, chkSegment, sigSegment (empty if absent)
     */
    public static TrailingSegments parseTrailingSegments(String message) {
        if (message == null) {
            return new TrailingSegments("", "", "");
        }
        String rest = message;
        String sigSegment = "";
        String chkSegment = "";
        int lastPipe = rest.lastIndexOf('|');
        while (lastPipe >= 0) {
            String segment = rest.substring(lastPipe + 1);
            if (segment.startsWith(TcpRestProtocol.SIGNATURE_PREFIX)) {
                sigSegment = segment;
                rest = rest.substring(0, lastPipe);
                lastPipe = rest.lastIndexOf('|');
                continue;
            }
            if (segment.startsWith(CHECKSUM_PREFIX)) {
                chkSegment = segment;
                rest = rest.substring(0, lastPipe);
                lastPipe = rest.lastIndexOf('|');
                continue;
            }
            break;
        }
        return new TrailingSegments(rest, chkSegment, sigSegment);
    }

    /**
     * Signs message with RSA-SHA256 (JDK only).
     *
     * @param message payload to sign (UTF-8)
     * @param privateKey signer's private key
     * @return Base64-encoded signature, never null
     */
    public static String sign(String message, PrivateKey privateKey) {
        if (message == null || privateKey == null) {
            throw new IllegalArgumentException("Message and privateKey cannot be null");
        }
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privateKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = sig.sign();
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new SecurityException("Failed to sign message: " + e.getMessage(), e);
        }
    }

    /**
     * Verifies RSA-SHA256 signature.
     *
     * @param message original payload (UTF-8)
     * @param signatureBase64 Base64-encoded signature
     * @param publicKey signer's public key
     * @return true if signature is valid
     */
    public static boolean verifySignature(String message, String signatureBase64, PublicKey publicKey) {
        if (message == null || signatureBase64 == null || publicKey == null) {
            return false;
        }
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return sig.verify(signatureBytes);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Builds the SIG segment for the given message when signature is enabled.
     * Format: SIG:RSA:base64(signature).
     *
     * @param message payload to sign (typically content or content|CHK:value)
     * @param config security configuration
     * @return "SIG:RSA:base64" or "" if signature disabled
     */
    public static String calculateSignature(String message, SecurityConfig config) {
        if (config == null || !config.isSignatureEnabled()) {
            return "";
        }
        String customName = config.getCustomSignatureAlgorithmName();
        if (customName != null) {
            SignatureHandler handler = getSignatureHandler(customName);
            if (handler == null) {
                return "";
            }
            Object keyConfig = config.getSigningKeyConfig();
            if (keyConfig == null) {
                return "";
            }
            String signatureBase64 = handler.sign(message, keyConfig);
            return TcpRestProtocol.SIGNATURE_PREFIX + customName + ":" + signatureBase64;
        }
        if (config.getSignatureAlgorithm() != SecurityConfig.SignatureAlgorithm.RSA_SHA256) {
            return "";
        }
        PrivateKey key = config.getSigningPrivateKey();
        if (key == null) {
            return "";
        }
        String signatureBase64 = sign(message, key);
        return TcpRestProtocol.SIGNATURE_PREFIX + SIG_RSA_PREFIX + signatureBase64;
    }

    /**
     * Verifies the SIG segment against the signed payload.
     * Supports SIG:RSA:base64 (JDK). SIG:GPG:... throws SecurityException (use optional module).
     *
     * @param signedPayload the payload that was signed (content or content|CHK:value)
     * @param sigSegment full segment e.g. "SIG:RSA:base64..."
     * @param config security configuration
     * @throws SecurityException if signature required but invalid or unsupported algorithm
     */
    public static void verifySignatureSegment(String signedPayload, String sigSegment, SecurityConfig config) {
        if (config == null || !config.isSignatureEnabled()) {
            return;
        }
        if (sigSegment == null || sigSegment.isEmpty()) {
            throw new SecurityException("Signature enabled but not provided in message");
        }
        if (!sigSegment.startsWith(TcpRestProtocol.SIGNATURE_PREFIX)) {
            throw new SecurityException("Invalid signature format, expected SIG:value");
        }
        String value = sigSegment.substring(TcpRestProtocol.SIGNATURE_PREFIX.length());
        if (value.startsWith(SIG_RSA_PREFIX)) {
            String signatureBase64 = value.substring(SIG_RSA_PREFIX.length());
            PublicKey key = config.getVerificationPublicKey();
            if (key == null) {
                throw new SecurityException("Signature verification key not configured");
            }
            if (!verifySignature(signedPayload, signatureBase64, key)) {
                throw new SecurityException("Signature verification failed - message may have been tampered or wrong key");
            }
            return;
        }
        int colon = value.indexOf(':');
        if (colon > 0) {
            String algo = value.substring(0, colon);
            SignatureHandler handler = getSignatureHandler(algo);
            if (handler != null) {
                String signatureBase64 = value.substring(colon + 1);
                Object keyConfig = config.getVerificationKeyConfig();
                if (keyConfig == null) {
                    throw new SecurityException("Signature verification key not configured for " + algo);
                }
                if (!handler.verify(signedPayload, signatureBase64, keyConfig)) {
                    throw new SecurityException("Signature verification failed - message may have been tampered or wrong key");
                }
                return;
            }
        }
        if (value.startsWith("GPG:")) {
            throw new SecurityException("GPG signature not supported in commons; use optional tcprest-pgp module");
        }
        throw new SecurityException("Unsupported signature algorithm: " + (value.contains(":") ? value.substring(0, value.indexOf(':')) : value));
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
