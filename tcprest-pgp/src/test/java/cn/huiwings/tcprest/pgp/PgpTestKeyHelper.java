package cn.huiwings.tcprest.pgp;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

/**
 * Test helper to generate in-memory PGP key pairs for E2E tests.
 * Uses empty passphrase; keys are signing-only.
 */
public final class PgpTestKeyHelper {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private PgpTestKeyHelper() {
    }

    /**
     * Generates a PGP signing key pair (RSA 2048) in memory.
     *
     * @param identity user ID for the key
     * @return (privateKey, publicKey) for use with SecurityConfig.enableCustomSignature("GPG", ...)
     */
    public static PgpKeyHolder generateKeyPair(String identity) throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        Date now = new Date();
        PGPKeyPair pgpKeyPair = new JcaPGPKeyPair(PGPPublicKey.RSA_GENERAL, kp, now);

        PGPSignatureSubpacketGenerator subpackets = new PGPSignatureSubpacketGenerator();
        subpackets.setKeyFlags(true, KeyFlags.SIGN_DATA);
        var digestCalcProvider = new JcaPGPDigestCalculatorProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build();
        PBESecretKeyEncryptor encryptor = new JcePBESecretKeyEncryptorBuilder(
                SymmetricKeyAlgorithmTags.AES_256,
                digestCalcProvider.get(HashAlgorithmTags.SHA1))
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build("".toCharArray());

        PGPKeyRingGenerator gen = new PGPKeyRingGenerator(
                PGPSignature.POSITIVE_CERTIFICATION,
                pgpKeyPair,
                identity,
                digestCalcProvider.get(HashAlgorithmTags.SHA1),
                subpackets.generate(),
                null,
                new JcaPGPContentSignerBuilder(pgpKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA256).setProvider(BouncyCastleProvider.PROVIDER_NAME),
                encryptor);

        PGPSecretKeyRing secretKeyRing = gen.generateSecretKeyRing();
        Iterator<PGPPublicKey> pubIt = secretKeyRing.getPublicKeys();
        PGPPublicKey publicKey = pubIt.next();
        PGPPrivateKey privateKey = secretKeyRing.getSecretKey().extractPrivateKey(
                new JcePBESecretKeyDecryptorBuilder(digestCalcProvider)
                        .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                        .build("".toCharArray()));
        return new PgpKeyHolder(privateKey, publicKey);
    }

    public static final class PgpKeyHolder {
        public final PGPPrivateKey privateKey;
        public final PGPPublicKey publicKey;

        PgpKeyHolder(PGPPrivateKey privateKey, PGPPublicKey publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
        }
    }
}
