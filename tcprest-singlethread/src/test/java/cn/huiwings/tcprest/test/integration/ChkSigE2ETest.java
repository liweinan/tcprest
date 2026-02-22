package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServerStatus;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.testng.Assert.*;

/**
 * End-to-end test for CHK + SIG (checksum + signature) with real server and client.
 *
 * <p>Verifies that when both server and client enable CRC32 and RSA signature,
 * request and response carry CHK and SIG segments and verification succeeds.</p>
 */
public class ChkSigE2ETest {

    private static final PortGenerator.PortRange PORT_RANGE = PortGenerator.from(22500);

    private TcpRestServer server;
    private int port;
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
        port = PORT_RANGE.next();

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

        server = new SingleThreadTcpRestServer(port);
        server.setSecurityConfig(serverConfig);
        server.addResource(EchoServiceImpl.class);
        assertEquals(server.getStatus(), TcpRestServerStatus.CLOSED);
        server.up();
        Thread.sleep(500);
        assertEquals(server.getStatus(), TcpRestServerStatus.RUNNING);

        TcpRestClientFactory factory = new TcpRestClientFactory(EchoService.class, "localhost", port);
        factory.withSecurity(clientConfig);
        client = factory.getClient();
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
    public void testChkSig_requestAndResponse_success() {
        assertEquals(client.echo("hello"), "hello");
        assertEquals(client.add(3, 5), 8);
    }
}
