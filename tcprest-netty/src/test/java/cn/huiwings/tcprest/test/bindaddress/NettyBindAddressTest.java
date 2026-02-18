package cn.huiwings.tcprest.test.bindaddress;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParams;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Tests Netty server bind address functionality.
 *
 * <p>Verifies that NettyTcpRestServer can bind to specific IP addresses.</p>
 */
public class NettyBindAddressTest {

    // Use dedicated port range for this test class (30000-30999)
    private static final PortGenerator.PortRange portRange = PortGenerator.from(30000);

    private TcpRestServer server;

    @AfterMethod
    public void tearDown() throws Exception {
        if (server != null) {
            server.down();
            server = null;
        }
        Thread.sleep(300);
    }

    @Test
    public void testNetty_bindToLocalhostOnly() throws Exception {
        int port = portRange.next();
        server = new NettyTcpRestServer(port, "127.0.0.1");
        server.addResource(TestService.Impl.class);
        server.up();
        Thread.sleep(500);

        // Create client and verify it works
        TcpRestClientFactory factory = new TcpRestClientFactory(
            TestService.class, "127.0.0.1", port
        );
        TestService client = (TestService) factory.getClient();
        assertEquals(client.echo("netty-localhost"), "netty-localhost");
    }

    @Test
    public void testNetty_bindToLocalhostWithSSL() throws Exception {
        int port = portRange.next();

        SSLParams serverSSL = new SSLParams();
        serverSSL.setKeyStorePath("classpath:server_ks");
        serverSSL.setKeyStoreKeyPass("123123");
        serverSSL.setTrustStorePath("classpath:server_ks");
        serverSSL.setNeedClientAuth(true);

        server = new NettyTcpRestServer(port, "127.0.0.1", serverSSL);
        server.addResource(TestService.Impl.class);
        server.up();
        Thread.sleep(500);

        // Create SSL client
        SSLParams clientSSL = new SSLParams();
        clientSSL.setKeyStorePath("classpath:client_ks");
        clientSSL.setKeyStoreKeyPass("456456");
        clientSSL.setTrustStorePath("classpath:client_ks");
        clientSSL.setNeedClientAuth(true);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            TestService.class, "127.0.0.1", port, null, clientSSL
        );
        TestService client = (TestService) factory.getClient();
        assertEquals(client.echo("netty-ssl-localhost"), "netty-ssl-localhost");
    }

    @Test
    public void testNetty_protocolV2_withBindAddress() throws Exception {
        int port = portRange.next();
        server = new NettyTcpRestServer(port, "127.0.0.1");
        server.addResource(TestService.Impl.class);
        server.up();
        Thread.sleep(500);

        // Create v2 client
        TcpRestClientFactory factory = new TcpRestClientFactory(
            TestService.class, "127.0.0.1", port
        );
        TestService client = (TestService) factory.getClient();

        // Test overloaded methods work with bind address
        assertEquals(client.add(10, 20), 30);
        assertEquals(client.add("Netty", "Bind"), "NettyBind");
    }

    // ========== Test Service Interface ==========

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
