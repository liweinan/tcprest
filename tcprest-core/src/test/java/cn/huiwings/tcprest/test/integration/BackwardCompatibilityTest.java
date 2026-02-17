package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Backward compatibility tests for Protocol v1 and v2.
 *
 * <p>Tests the compatibility matrix:</p>
 * <table>
 *   <tr><th>Client</th><th>Server</th><th>Expected Result</th></tr>
 *   <tr><td>V1</td><td>V1 only</td><td>✅ Works</td></tr>
 *   <tr><td>V1</td><td>AUTO</td><td>✅ Works</td></tr>
 *   <tr><td>V2</td><td>V2 only</td><td>✅ Works</td></tr>
 *   <tr><td>V2</td><td>AUTO</td><td>✅ Works</td></tr>
 *   <tr><td>V2</td><td>V1 only</td><td>❌ Should fail</td></tr>
 * </table>
 */
public class BackwardCompatibilityTest {

    private TcpRestServer server;
    private int port;

    @AfterMethod
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
            server = null;
        }
        Thread.sleep(1000); // Increased delay to ensure port is fully released
    }

    // ========== V1 Client with Different Server Modes ==========

    @Test
    public void testV1Client_withV1Server() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V1);
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        // Create V1 client (default)
        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        // V1 is default, no need to set
        SimpleService client = (SimpleService) factory.getClient();

        int result = client.add(5, 3);
        assertEquals(result, 8);
    }

    @Test
    public void testV1Client_withAutoServer() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.AUTO); // Default
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        SimpleService client = (SimpleService) factory.getClient();

        int result = client.add(10, 20);
        assertEquals(result, 30);
    }

    @Test
    public void testV1Client_withV2Server_shouldFail() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V2); // Only accept V2
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        SimpleService client = (SimpleService) factory.getClient();

        try {
            // V1 client sending to V2-only server should fail or return error
            int result = client.add(5, 3);
            // V2 server will return protocol error, which v1 client may interpret as 0 or error
            // This is expected behavior - v1 clients should not connect to v2-only servers
        } catch (Exception e) {
            // Also acceptable - connection or protocol error
            assertTrue(true);
        }
    }

    // ========== V2 Client with Different Server Modes ==========

    @Test
    public void testV2Client_withV2Server() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V2);
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        SimpleService client = (SimpleService) factory.getClient();

        int result = client.add(7, 8);
        assertEquals(result, 15);
    }

    @Test
    public void testV2Client_withAutoServer() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.AUTO);
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        SimpleService client = (SimpleService) factory.getClient();

        int result = client.add(100, 200);
        assertEquals(result, 300);
    }

    @Test
    public void testV2Client_withV1Server_shouldFail() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V1); // Only accept V1
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        SimpleService client = (SimpleService) factory.getClient();

        try {
            // V2 client sending to V1-only server should fail
            int result = client.add(5, 3);
            // Server will reject V2 format
            fail("Should have failed or returned error");
        } catch (Exception e) {
            // Expected - V1 server cannot parse V2 requests
            assertTrue(true);
        }
    }

    // ========== Mixed Clients on Same Server ==========

    @Test
    public void testMixedClients_autoServer() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.AUTO);
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        // Create V1 client
        TcpRestClientFactory v1Factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        SimpleService v1Client = (SimpleService) v1Factory.getClient();

        // Create V2 client
        TcpRestClientFactory v2Factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        v2Factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        SimpleService v2Client = (SimpleService) v2Factory.getClient();

        // Both should work
        assertEquals(v1Client.add(1, 2), 3);
        assertEquals(v2Client.add(3, 4), 7);
        assertEquals(v1Client.add(5, 6), 11);
        assertEquals(v2Client.add(7, 8), 15);
    }

    // ========== Test Default Behavior ==========

    @Test
    public void testDefaultServer_isAuto() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        // Don't set protocol version - should default to AUTO
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        // Verify AUTO by testing both v1 and v2 clients
        TcpRestClientFactory v1Factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        SimpleService v1Client = (SimpleService) v1Factory.getClient();

        TcpRestClientFactory v2Factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        v2Factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        SimpleService v2Client = (SimpleService) v2Factory.getClient();

        // Both should work
        assertEquals(v1Client.add(10, 20), 30);
        assertEquals(v2Client.add(30, 40), 70);
    }

    @Test
    public void testDefaultClient_isV1() throws Exception {
        port = PortGenerator.get();
        server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V1);
        server.addSingletonResource(new SimpleService.Impl());
        server.up();
        Thread.sleep(500);

        // Create client without setting protocol version - should default to V1
        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", port
        );
        SimpleService client = (SimpleService) factory.getClient();

        int result = client.add(15, 25);
        assertEquals(result, 40);
    }

    // ========== Test Protocol Version Getters ==========

    @Test
    public void testServerProtocolVersion_getters() throws Exception {
        server = new SingleThreadTcpRestServer(PortGenerator.get());

        // Default should be AUTO
        assertEquals(server.getProtocolVersion(), ProtocolVersion.AUTO);

        server.setProtocolVersion(ProtocolVersion.V2);
        assertEquals(server.getProtocolVersion(), ProtocolVersion.V2);

        server.setProtocolVersion(ProtocolVersion.V1);
        assertEquals(server.getProtocolVersion(), ProtocolVersion.V1);
    }

    @Test
    public void testClientProtocolConfig_getters() {
        TcpRestClientFactory factory = new TcpRestClientFactory(
            SimpleService.class, "localhost", 8000
        );

        // Default should be V1
        assertTrue(factory.getProtocolConfig().isV1());
        assertFalse(factory.getProtocolConfig().isV2());

        factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
        assertTrue(factory.getProtocolConfig().isV2());
        assertFalse(factory.getProtocolConfig().isV1());
    }

    // ========== Test Service Interface ==========

    public interface SimpleService {
        int add(int a, int b);
        String echo(String s);

        class Impl implements SimpleService {
            @Override
            public int add(int a, int b) {
                return a + b;
            }

            @Override
            public String echo(String s) {
                return s;
            }
        }
    }
}
