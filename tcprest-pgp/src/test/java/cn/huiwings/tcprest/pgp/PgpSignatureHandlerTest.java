package cn.huiwings.tcprest.pgp;

import cn.huiwings.tcprest.security.SignatureHandler;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Unit tests for PgpSignatureHandler sign/verify round-trip.
 */
public class PgpSignatureHandlerTest {

    private PGPPrivateKey privateKey;
    private PGPPublicKey publicKey;

    @BeforeClass
    public void generateKeys() throws Exception {
        PgpSignatureHandler.register();
        PgpTestKeyHelper.PgpKeyHolder holder = PgpTestKeyHelper.generateKeyPair("test@tcprest");
        this.privateKey = holder.privateKey;
        this.publicKey = holder.publicKey;
        assertNotNull(privateKey);
        assertNotNull(publicKey);
    }

    @Test
    public void testSignAndVerify_roundTrip() {
        SignatureHandler handler = new PgpSignatureHandler();
        String message = "V2|0|0|{{aGVsbG8=}}|CHK:abc123";
        String sig = handler.sign(message, privateKey);
        assertNotNull(sig);
        assertFalse(sig.isEmpty());
        assertTrue(handler.verify(message, sig, publicKey));
    }

    @Test
    public void testVerify_tamperedMessage_fails() {
        SignatureHandler handler = new PgpSignatureHandler();
        String message = "V2|0|0|{{aGVsbG8=}}";
        String sig = handler.sign(message, privateKey);
        assertFalse(handler.verify(message + "x", sig, publicKey));
    }

    @Test
    public void testVerify_wrongKey_fails() throws Exception {
        PgpTestKeyHelper.PgpKeyHolder other = PgpTestKeyHelper.generateKeyPair("other@tcprest");
        SignatureHandler handler = new PgpSignatureHandler();
        String message = "hello";
        String sig = handler.sign(message, privateKey);
        assertFalse(handler.verify(message, sig, other.publicKey));
    }
}
