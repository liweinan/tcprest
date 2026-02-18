package cn.huiwings.tcprest.test.ssl;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.ssl.SSLParams;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests SSL/TLS support combined with compression feature.
 * Verifies that encryption and compression work together correctly.
 */
public class SSLWithCompressionTest {

    private TcpRestServer server;
    private int port;

    @BeforeMethod
    public void setup() throws Exception {
        port = Math.abs(new Random().nextInt()) % 10000 + 8000;

        // Configure SSL
        SSLParams serverSSLParams = new SSLParams();
        serverSSLParams.setTrustStorePath("classpath:server_ks");
        serverSSLParams.setKeyStorePath("classpath:server_ks");
        serverSSLParams.setKeyStoreKeyPass("123123");
        serverSSLParams.setNeedClientAuth(true);

        // Create server with SSL
        server = new SingleThreadTcpRestServer(port, serverSSLParams);

        // Enable compression
        server.enableCompression();

        server.addResource(HelloWorldResource.class);
        server.up();
        Thread.sleep(100);
    }

    @AfterMethod
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
            Thread.sleep(200);
        }
    }

    @Test
    public void testSSLWithCompression() throws Exception {
        // Configure client SSL
        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setKeyStorePath("classpath:client_ks");
        clientSSLParams.setKeyStoreKeyPass("456456");
        clientSSLParams.setNeedClientAuth(true);

        // Create client with SSL and compression
        TcpRestClientFactory factory = new TcpRestClientFactory(
            HelloWorld.class, "localhost", port, null, clientSSLParams
        ).withCompression();

        HelloWorld client = factory.getInstance();

        // Test basic call
        String result = client.sayHelloTo("Secure World");
        assertEquals(result, "Hello, Secure World");
    }

    @Test
    public void testSSLWithCompressionLargeData() throws Exception {
        // Configure client SSL
        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setKeyStorePath("classpath:client_ks");
        clientSSLParams.setKeyStoreKeyPass("456456");
        clientSSLParams.setNeedClientAuth(true);

        // Create client with SSL and aggressive compression
        TcpRestClientFactory factory = new TcpRestClientFactory(
            HelloWorld.class, "localhost", port, null, clientSSLParams
        ).withCompression(new CompressionConfig(true, 100, 9));

        HelloWorld client = factory.getInstance();

        // Test with large input that benefits from compression
        StringBuilder largeInput = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            largeInput.append("Test data ");
        }
        String input = largeInput.toString();

        String result = client.sayHelloTo(input);
        assertTrue(result.startsWith("Hello, Test data"));
    }

    @Test
    public void testSSLWithoutClientCompression() throws Exception {
        // Server has compression enabled
        // Client doesn't enable compression (backward compatibility test)

        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setKeyStorePath("classpath:client_ks");
        clientSSLParams.setKeyStoreKeyPass("456456");
        clientSSLParams.setNeedClientAuth(true);

        // Client without compression
        TcpRestClientFactory factory = new TcpRestClientFactory(
            HelloWorld.class, "localhost", port, null, clientSSLParams
        );

        HelloWorld client = factory.getInstance();

        String result = client.sayHelloTo("Mixed Config");
        assertEquals(result, "Hello, Mixed Config");
    }

    @Test
    public void testMultipleSSLConnectionsWithCompression() throws Exception {
        SSLParams clientSSLParams = new SSLParams();
        clientSSLParams.setTrustStorePath("classpath:client_ks");
        clientSSLParams.setKeyStorePath("classpath:client_ks");
        clientSSLParams.setKeyStoreKeyPass("456456");
        clientSSLParams.setNeedClientAuth(true);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            HelloWorld.class, "localhost", port, null, clientSSLParams
        ).withCompression();

        HelloWorld client = factory.getInstance();

        // Multiple requests over SSL with compression
        for (int i = 0; i < 5; i++) {
            String result = client.sayHelloTo("Request " + i);
            assertEquals(result, "Hello, Request " + i);
        }
    }
}
