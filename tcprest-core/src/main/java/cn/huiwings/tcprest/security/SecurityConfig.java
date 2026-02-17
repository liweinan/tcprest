package cn.huiwings.tcprest.security;

import java.util.HashSet;
import java.util.Set;

/**
 * Security configuration for TcpRest protocol.
 *
 * <p>Provides options for:
 * <ul>
 *   <li>Message integrity verification (CRC32/HMAC)</li>
 *   <li>Class name whitelist validation</li>
 *   <li>Secure encoding of all protocol components</li>
 * </ul>
 *
 * <p>All security features are optional and disabled by default for backward compatibility.
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class SecurityConfig {

    /**
     * Checksum algorithm for message integrity verification.
     */
    public enum ChecksumAlgorithm {
        /** No checksum (default) */
        NONE,
        /** CRC32 checksum (fast, detects accidental corruption) */
        CRC32,
        /** HMAC-SHA256 (secure, detects malicious tampering, requires shared secret) */
        HMAC_SHA256
    }

    private ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.NONE;
    private String hmacSecret = null;
    private boolean enableClassWhitelist = false;
    private Set<String> allowedClasses = new HashSet<>();

    /**
     * Creates a default security configuration with all features disabled.
     */
    public SecurityConfig() {
    }

    /**
     * Enables CRC32 checksum for message integrity verification.
     *
     * <p>CRC32 provides fast integrity checking to detect accidental message corruption
     * during transmission. It does NOT provide security against malicious tampering.
     *
     * @return this config for chaining
     */
    public SecurityConfig enableCRC32() {
        this.checksumAlgorithm = ChecksumAlgorithm.CRC32;
        return this;
    }

    /**
     * Enables HMAC-SHA256 checksum for secure message authentication.
     *
     * <p>HMAC provides cryptographic authentication to detect malicious message
     * tampering. Requires a shared secret between client and server.
     *
     * @param secret shared secret for HMAC computation (must be same on client and server)
     * @return this config for chaining
     * @throws IllegalArgumentException if secret is null or empty
     */
    public SecurityConfig enableHMAC(String secret) {
        if (secret == null || secret.isEmpty()) {
            throw new IllegalArgumentException("HMAC secret cannot be null or empty");
        }
        this.checksumAlgorithm = ChecksumAlgorithm.HMAC_SHA256;
        this.hmacSecret = secret;
        return this;
    }

    /**
     * Disables checksum verification (default).
     *
     * @return this config for chaining
     */
    public SecurityConfig disableChecksum() {
        this.checksumAlgorithm = ChecksumAlgorithm.NONE;
        this.hmacSecret = null;
        return this;
    }

    /**
     * Enables class name whitelist validation.
     *
     * <p>When enabled, only class names that have been explicitly added to the
     * whitelist will be allowed. This prevents unauthorized access to internal classes.
     *
     * @return this config for chaining
     */
    public SecurityConfig enableClassWhitelist() {
        this.enableClassWhitelist = true;
        return this;
    }

    /**
     * Disables class name whitelist validation (default).
     *
     * @return this config for chaining
     */
    public SecurityConfig disableClassWhitelist() {
        this.enableClassWhitelist = false;
        return this;
    }

    /**
     * Adds a class name to the whitelist.
     *
     * <p>Only effective when whitelist is enabled via {@link #enableClassWhitelist()}.
     *
     * @param className fully qualified class name to allow
     * @return this config for chaining
     */
    public SecurityConfig allowClass(String className) {
        this.allowedClasses.add(className);
        return this;
    }

    /**
     * Adds multiple class names to the whitelist.
     *
     * @param classNames class names to allow
     * @return this config for chaining
     */
    public SecurityConfig allowClasses(String... classNames) {
        for (String className : classNames) {
            this.allowedClasses.add(className);
        }
        return this;
    }

    /**
     * Checks if a class name is allowed.
     *
     * @param className class name to check
     * @return true if whitelist is disabled OR class is in whitelist
     */
    public boolean isClassAllowed(String className) {
        if (!enableClassWhitelist) {
            return true; // Whitelist disabled, allow all
        }
        return allowedClasses.contains(className);
    }

    // Getters

    public ChecksumAlgorithm getChecksumAlgorithm() {
        return checksumAlgorithm;
    }

    public String getHmacSecret() {
        return hmacSecret;
    }

    public boolean isClassWhitelistEnabled() {
        return enableClassWhitelist;
    }

    public Set<String> getAllowedClasses() {
        return new HashSet<>(allowedClasses);
    }

    public boolean isChecksumEnabled() {
        return checksumAlgorithm != ChecksumAlgorithm.NONE;
    }
}
