package cn.huiwings.tcprest.pgp;

import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SignatureHandler;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Base64;

/**
 * GPG/OpenPGP signature handler for TcpRest (wire format: SIG:GPG:base64).
 * Uses Bouncy Castle; signing key = {@link PGPPrivateKey}, verification key = {@link PGPPublicKey}.
 *
 * <p>Registers itself as "GPG" on class load so that commons uses it when
 * {@link cn.huiwings.tcprest.security.SecurityConfig#enableCustomSignature(String, Object, Object)}
 * is called with algorithm name "GPG".
 */
public final class PgpSignatureHandler implements SignatureHandler {

    private static final String ALGORITHM_NAME = "GPG";

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        ProtocolSecurity.registerSignatureHandler(ALGORITHM_NAME, new PgpSignatureHandler());
    }

    /**
     * Ensures class is loaded so that the static initializer runs and registers the handler.
     * Call this if you use the handler without having referenced this class elsewhere.
     */
    public static void register() {
        // no-op; static block already ran on class load
    }

    @Override
    public String getAlgorithmName() {
        return ALGORITHM_NAME;
    }

    @Override
    public String sign(String message, Object signingKeyConfig) {
        if (message == null || signingKeyConfig == null) {
            throw new IllegalArgumentException("message and signingKeyConfig must be non-null");
        }
        if (!(signingKeyConfig instanceof PGPPrivateKey)) {
            throw new IllegalArgumentException("Signing key must be PGPPrivateKey, got " + signingKeyConfig.getClass().getName());
        }
        PGPPrivateKey privateKey = (PGPPrivateKey) signingKeyConfig;
        try {
            int keyAlgo = privateKey.getPublicKeyPacket().getAlgorithm();
            PGPSignatureGenerator generator = new PGPSignatureGenerator(
                    new JcaPGPContentSignerBuilder(keyAlgo, PGPUtil.SHA256).setProvider(BouncyCastleProvider.PROVIDER_NAME));
            generator.init(PGPSignature.BINARY_DOCUMENT, privateKey);
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            generator.update(messageBytes);
            PGPSignature signature = generator.generate();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (BCPGOutputStream bcpgOut = new BCPGOutputStream(out)) {
                signature.encode(bcpgOut);
            }
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("GPG sign failed", e);
        }
    }

    @Override
    public boolean verify(String message, String signatureBase64, Object verificationKeyConfig) {
        if (message == null || signatureBase64 == null || verificationKeyConfig == null) {
            return false;
        }
        if (!(verificationKeyConfig instanceof PGPPublicKey)) {
            throw new IllegalArgumentException("Verification key must be PGPPublicKey, got " + verificationKeyConfig.getClass().getName());
        }
        PGPPublicKey publicKey = (PGPPublicKey) verificationKeyConfig;
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            try (ByteArrayInputStream in = new ByteArrayInputStream(signatureBytes);
                 BCPGInputStream bcpgIn = new BCPGInputStream(in)) {
                JcaPGPObjectFactory factory = new JcaPGPObjectFactory(bcpgIn);
                Object obj = factory.nextObject();
                PGPSignature signature;
                if (obj instanceof PGPSignatureList) {
                    PGPSignatureList list = (PGPSignatureList) obj;
                    if (list.isEmpty()) {
                        return false;
                    }
                    signature = list.get(0);
                } else if (obj instanceof PGPSignature) {
                    signature = (PGPSignature) obj;
                } else {
                    return false;
                }
                signature.init(new JcaPGPContentVerifierBuilderProvider().setProvider(BouncyCastleProvider.PROVIDER_NAME), publicKey);
                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                signature.update(messageBytes);
                return signature.verify();
            }
        } catch (Exception e) {
            return false;
        }
    }
}
