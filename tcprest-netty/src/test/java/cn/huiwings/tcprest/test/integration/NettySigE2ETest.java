package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.pgp.PgpSignatureHandler;
import cn.huiwings.tcprest.pgp.PgpTestKeyHelper;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * End-to-end tests for SIG (signature) with Netty server: RSA and GPG.
 *
 * <p>Verifies that when server and client enable CRC32 + signature (RSA or GPG),
 * request and response carry CHK and SIG segments and verification succeeds.</p>
 */
public class NettySigE2ETest {

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

    @Test
    public void testRsaSigE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair serverKeyPair = kpg.generateKeyPair();
            KeyPair clientKeyPair = kpg.generateKeyPair();

            SecurityConfig serverConfig = new SecurityConfig()
                    .enableCRC32()
                    .enableSignature(serverKeyPair.getPrivate(), clientKeyPair.getPublic());
            SecurityConfig clientConfig = new SecurityConfig()
                    .enableCRC32()
                    .enableSignature(clientKeyPair.getPrivate(), serverKeyPair.getPublic());

            server = new NettyTcpRestServer(port);
            server.setSecurityConfig(serverConfig);
            server.addResource(EchoServiceImpl.class);
            server.up();
            Thread.sleep(500);

            TcpRestClientFactory factory = new TcpRestClientFactory(EchoService.class, "localhost", port);
            factory.withSecurity(clientConfig);
            EchoService client = factory.getClient();
            assertNotNull(client);

            assertEquals(client.echo("hello"), "hello");
            assertEquals(client.add(3, 5), 8);
        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    @Test
    public void testGpgSigE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            PgpSignatureHandler.register();
            PgpTestKeyHelper.PgpKeyHolder serverKeys = PgpTestKeyHelper.generateKeyPair("server@netty.test");
            PgpTestKeyHelper.PgpKeyHolder clientKeys = PgpTestKeyHelper.generateKeyPair("client@netty.test");

            SecurityConfig serverConfig = new SecurityConfig()
                    .enableCRC32()
                    .enableCustomSignature("GPG", serverKeys.privateKey, clientKeys.publicKey);
            SecurityConfig clientConfig = new SecurityConfig()
                    .enableCRC32()
                    .enableCustomSignature("GPG", clientKeys.privateKey, serverKeys.publicKey);

            server = new NettyTcpRestServer(port);
            server.setSecurityConfig(serverConfig);
            server.addResource(EchoServiceImpl.class);
            server.up();
            Thread.sleep(500);

            TcpRestClientFactory factory = new TcpRestClientFactory(EchoService.class, "localhost", port);
            factory.withSecurity(clientConfig);
            EchoService client = factory.getClient();
            assertNotNull(client);

            assertEquals(client.echo("hello"), "hello");
            assertEquals(client.add(3, 5), 8);
        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }
}
