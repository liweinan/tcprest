package cn.huiwings.tcprest.pgp;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServerStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * End-to-end test for SIG:GPG signature with real server and client.
 *
 * <p>Ensures that when tcprest-pgp is on classpath and server/client use
 * {@link SecurityConfig#enableCustomSignature(String, Object, Object)} with "GPG"
 * and PGP key pairs, requests and responses carry SIG:GPG:base64 and verification succeeds.</p>
 */
public class GpgSignatureE2ETest {

    private static final int PORT = 23500;

    private TcpRestServer server;
    private EchoService client;

    public interface EchoService {
        String echo(String s);
        int add(int a, int b);
    }

    public static class EchoServiceImpl implements EchoService {
        @Override
        public String echo(String s) {
            return s;
        }

        @Override
        public int add(int a, int b) {
            return a + b;
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        PgpSignatureHandler.register();

        PgpTestKeyHelper.PgpKeyHolder serverKeys = PgpTestKeyHelper.generateKeyPair("server@tcprest.test");
        PgpTestKeyHelper.PgpKeyHolder clientKeys = PgpTestKeyHelper.generateKeyPair("client@tcprest.test");

        SecurityConfig serverConfig = new SecurityConfig()
                .enableCRC32()
                .enableCustomSignature("GPG", serverKeys.privateKey, clientKeys.publicKey);
        SecurityConfig clientConfig = new SecurityConfig()
                .enableCRC32()
                .enableCustomSignature("GPG", clientKeys.privateKey, serverKeys.publicKey);

        server = new SingleThreadTcpRestServer(PORT);
        server.setSecurityConfig(serverConfig);
        server.addResource(EchoServiceImpl.class);
        assertEquals(server.getStatus(), TcpRestServerStatus.CLOSED);
        server.up();
        Thread.sleep(500);
        assertEquals(server.getStatus(), TcpRestServerStatus.RUNNING);

        TcpRestClientFactory factory = new TcpRestClientFactory(EchoService.class, "localhost", PORT);
        factory.withSecurity(clientConfig);
        client = factory.getClient();
        assertNotNull(client);
    }

    @AfterClass
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
            assertEquals(server.getStatus(), TcpRestServerStatus.CLOSED);
        }
        Thread.sleep(300);
    }

    @Test
    public void testGpgSig_requestAndResponse_success() {
        assertEquals(client.echo("hello"), "hello");
        assertEquals(client.add(3, 5), 8);
    }
}
