package cn.huiwings.tcprest.security;

/**
 * SPI for custom signature algorithms (e.g. GPG/OpenPGP).
 * Commons implements RSA only; optional modules (e.g. tcprest-pgp) register a handler for "GPG".
 *
 * <p>Key config is opaque (e.g. PGPPrivateKey/PGPPublicKey from Bouncy Castle).</p>
 *
 * @since 1.1.0
 */
public interface SignatureHandler {

    /**
     * Algorithm name used in wire format, e.g. "GPG" for {@code SIG:GPG:base64}.
     *
     * @return algorithm name (non-null, no colon)
     */
    String getAlgorithmName();

    /**
     * Sign the message and return Base64-encoded signature (no algorithm prefix).
     *
     * @param message payload to sign (UTF-8)
     * @param signingKeyConfig opaque key config (e.g. PGP private key)
     * @return Base64-encoded signature
     */
    String sign(String message, Object signingKeyConfig);

    /**
     * Verify the signature.
     *
     * @param message original payload (UTF-8)
     * @param signatureBase64 Base64-encoded signature (no algorithm prefix)
     * @param verificationKeyConfig opaque key config (e.g. PGP public key)
     * @return true if valid
     */
    boolean verify(String message, String signatureBase64, Object verificationKeyConfig);
}
