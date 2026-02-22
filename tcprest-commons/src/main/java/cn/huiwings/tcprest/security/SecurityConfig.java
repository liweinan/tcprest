package cn.huiwings.tcprest.security;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

/**
 * Security configuration for TcpRest protocol.
 *
 * <p>Provides options for:
 * <ul>
 *   <li>Message integrity verification via CHK (CRC32/HMAC)</li>
 *   <li>Origin signature via SIG (e.g. RSA-SHA256)</li>
 *   <li>Class name whitelist validation</li>
 *   <li>Secure encoding of all protocol components</li>
 * </ul>
 *
 * <p>CHK and SIG are independent: CHK is integrity-only; SIG is origin authentication.
 * Both are optional and disabled by default for backward compatibility.
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class SecurityConfig {

    /**
     * Checksum algorithm for message integrity verification (CHK segment).
     */
    public enum ChecksumAlgorithm {
        /** No checksum (default) */
        NONE,
        /** CRC32 checksum (fast, detects accidental corruption) */
        CRC32,
        /** HMAC-SHA256 (secure, detects malicious tampering, requires shared secret) */
        HMAC_SHA256
    }

    /**
     * Signature algorithm for origin authentication (SIG segment).
     */
    public enum SignatureAlgorithm {
        /** No signature (default) */
        NONE,
        /** RSA with SHA-256 (JDK built-in, zero extra dependency) */
        RSA_SHA256
    }

    private ChecksumAlgorithm checksumAlgorithm = ChecksumAlgorithm.NONE;
    private String hmacSecret = null;
    private SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.NONE;
    private PrivateKey signingPrivateKey = null;
    private PublicKey verificationPublicKey = null;
    /** Custom signature algorithm name (e.g. "GPG") when using SPI handler; null when not used */
    private String customSignatureAlgorithmName = null;
    private Object signingKeyConfig = null;
    private Object verificationKeyConfig = null;
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
     * Enables RSA-SHA256 signature for origin authentication (SIG segment).
     *
     * <p>Each side configures its own signing private key and the peer's public key for verification.
     * Server signs responses with server private key; client verifies with server public key.
     * Client signs requests with client private key; server verifies with client public key.
     *
     * @param signingKey this side's private key for signing outgoing messages
     * @param verificationKey peer's public key for verifying incoming messages
     * @return this config for chaining
     * @throws IllegalArgumentException if either key is null
     */
    public SecurityConfig enableSignature(PrivateKey signingKey, PublicKey verificationKey) {
        if (signingKey == null || verificationKey == null) {
            throw new IllegalArgumentException("Signing key and verification key cannot be null");
        }
        this.signatureAlgorithm = SignatureAlgorithm.RSA_SHA256;
        this.signingPrivateKey = signingKey;
        this.verificationPublicKey = verificationKey;
        return this;
    }

    /**
     * Enables custom signature via SPI (e.g. GPG from tcprest-pgp).
     *
     * @param algorithmName wire prefix, e.g. "GPG" for SIG:GPG:base64
     * @param signingKeyConfig opaque key for signing (e.g. PGP private key)
     * @param verificationKeyConfig opaque key for verification (e.g. PGP public key)
     * @return this config for chaining
     */
    public SecurityConfig enableCustomSignature(String algorithmName, Object signingKeyConfig, Object verificationKeyConfig) {
        if (algorithmName == null || algorithmName.isEmpty() || algorithmName.contains(":")) {
            throw new IllegalArgumentException("Algorithm name must be non-empty and contain no colon");
        }
        if (signingKeyConfig == null || verificationKeyConfig == null) {
            throw new IllegalArgumentException("Signing and verification key config cannot be null");
        }
        this.signatureAlgorithm = SignatureAlgorithm.NONE;
        this.signingPrivateKey = null;
        this.verificationPublicKey = null;
        this.customSignatureAlgorithmName = algorithmName;
        this.signingKeyConfig = signingKeyConfig;
        this.verificationKeyConfig = verificationKeyConfig;
        return this;
    }

    /**
     * Disables signature (default).
     *
     * @return this config for chaining
     */
    public SecurityConfig disableSignature() {
        this.signatureAlgorithm = SignatureAlgorithm.NONE;
        this.signingPrivateKey = null;
        this.verificationPublicKey = null;
        this.customSignatureAlgorithmName = null;
        this.signingKeyConfig = null;
        this.verificationKeyConfig = null;
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

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public PrivateKey getSigningPrivateKey() {
        return signingPrivateKey;
    }

    public PublicKey getVerificationPublicKey() {
        return verificationPublicKey;
    }

    public boolean isSignatureEnabled() {
        return signatureAlgorithm != SignatureAlgorithm.NONE || customSignatureAlgorithmName != null;
    }

    public String getCustomSignatureAlgorithmName() {
        return customSignatureAlgorithmName;
    }

    public Object getSigningKeyConfig() {
        return signingPrivateKey != null ? signingPrivateKey : signingKeyConfig;
    }

    public Object getVerificationKeyConfig() {
        return verificationPublicKey != null ? verificationPublicKey : verificationKeyConfig;
    }
}
