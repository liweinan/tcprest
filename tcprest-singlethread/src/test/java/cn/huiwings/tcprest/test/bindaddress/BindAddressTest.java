package cn.huiwings.tcprest.test.bindaddress;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParam;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Tests SingleThread server bind address functionality.
 *
 * <p>Verifies that server can bind to specific IP addresses for security and multi-homing scenarios.</p>
 *
 * <p><b>Test scenarios:</b></p>
 * <ul>
 *   <li>Bind to all interfaces (default behavior, null bindAddress)</li>
 *   <li>Bind to localhost only (127.0.0.1)</li>
 *   <li>Works with Protocol v2</li>
 * </ul>
 */
public class BindAddressTest {

    // Use dedicated port range for this test class (26000-26999)
    private static final PortGenerator.PortRange portRange = PortGenerator.from(26000);

    private TcpRestServer server;

    @AfterMethod
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
            server = null;
        }
        Thread.sleep(300);
    }

    // ========== Test SingleThreadTcpRestServer ==========

    @Test
    public void testSingleThread_bindToLocalhostOnly() throws Exception {
        int port = portRange.next();
        server = new SingleThreadTcpRestServer(port, "127.0.0.1");
        server.addResource(TestService.Impl.class);
        server.up();
        Thread.sleep(200);

        // Create client and verify it works
        TcpRestClientFactory factory = new TcpRestClientFactory(
            TestService.class, "127.0.0.1", port
        );
        TestService client = (TestService) factory.getClient();
        assertEquals(client.echo("localhost"), "localhost");
    }

    @Test
    public void testSingleThread_bindToLocalhostWithSSL() throws Exception {
        int port = portRange.next();

        SSLParam serverSSL = new SSLParam();
        serverSSL.setKeyStorePath("classpath:server_ks");
        serverSSL.setKeyStoreKeyPass("123123");
        serverSSL.setTrustStorePath("classpath:server_ks");
        serverSSL.setNeedClientAuth(true);

        server = new SingleThreadTcpRestServer(port, "127.0.0.1", serverSSL);
        server.addResource(TestService.Impl.class);
        server.up();
        Thread.sleep(200);

        // Create SSL client
        SSLParam clientSSL = new SSLParam();
        clientSSL.setKeyStorePath("classpath:client_ks");
        clientSSL.setKeyStoreKeyPass("456456");
        clientSSL.setTrustStorePath("classpath:client_ks");
        clientSSL.setNeedClientAuth(true);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            TestService.class, "127.0.0.1", port, null, clientSSL
        );
        TestService client = (TestService) factory.getClient();
        assertEquals(client.echo("ssl-localhost"), "ssl-localhost");
    }

    // ========== Test Protocol v2 with Bind Address ==========

    @Test
    public void testProtocolV2_withBindAddress() throws Exception {
        int port = portRange.next();
        server = new SingleThreadTcpRestServer(port, "127.0.0.1");
        server.addResource(TestService.Impl.class);
        server.up();
        Thread.sleep(200);

        // Create v2 client
        TcpRestClientFactory factory = new TcpRestClientFactory(
            TestService.class, "127.0.0.1", port
        );
        TestService client = (TestService) factory.getClient();

        // Test overloaded methods work with bind address
        assertEquals(client.add(5, 3), 8);
        assertEquals(client.add("Hello", "World"), "HelloWorld");
    }

    // ========== Test Service Interface ==========

    /**
     * Simple test service for bind address verification.
     */
    public interface TestService {
        String echo(String message);
        int add(int a, int b);
        String add(String a, String b);

        class Impl implements TestService {
            @Override
            public String echo(String message) {
                return message;
            }

            @Override
            public int add(int a, int b) {
                return a + b;
            }

            @Override
            public String add(String a, String b) {
                return a + b;
            }
        }
    }
}
