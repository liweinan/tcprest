package cn.huiwings.tcprest.test.compression;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.*;

/**
 * Integration tests for compression feature between client and server.
 */
public class CompressionIntegrationTest {

    private static final int BASE_PORT = Math.abs(new Random().nextInt()) % 10000 + 7000;
    private TcpRestServer server;
    private int port;

    // Test resource interface
    public interface DataService {
        String echo(String message);
        String getLargeData();
        String processLargeInput(String input);
    }

    // Test resource implementation
    public static class DataServiceImpl implements DataService {
        public String echo(String message) {
            return message;
        }

        public String getLargeData() {
            // Return large repetitive data (compresses well)
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 500; i++) {
                sb.append("This is line ").append(i).append(" of test data. ");
            }
            return sb.toString();
        }

        public String processLargeInput(String input) {
            return "Processed: " + input.length() + " bytes";
        }
    }

    @BeforeMethod
    public void setup() throws Exception {
        port = BASE_PORT + new Random().nextInt(100);
        server = new SingleThreadTcpRestServer(port);
        server.addResource(DataServiceImpl.class);
        server.up();
        Thread.sleep(100); // Let server start
    }

    @AfterMethod
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
            Thread.sleep(200);
        }
    }

    @Test
    public void testBothEnabledCompression() throws Exception {
        // Enable compression on server
        server.enableCompression();

        // Enable compression on client
        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                .withCompression();
        DataService client = factory.getInstance();

        String result = client.echo("Hello, compressed world!");
        assertEquals(result, "Hello, compressed world!");
    }

    @Test
    public void testLargeDataWithCompression() throws Exception {
        // Enable compression on both sides
        server.setCompressionConfig(new CompressionConfig(true, 512, 9)); // Aggressive compression

        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                .withCompression(new CompressionConfig(true, 512, 9));
        DataService client = factory.getInstance();

        String largeData = client.getLargeData();
        assertTrue(largeData.length() > 10000, "Should return large data");
        assertTrue(largeData.contains("This is line"), "Should contain expected content");
    }

    @Test
    public void testCompressionBackwardCompatibility() throws Exception {
        // Server has compression disabled (default)
        // Client has compression enabled

        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                .withCompression();
        DataService client = factory.getInstance();

        // Should still work with mixed configuration
        String result = client.echo("Backward compatibility test");
        assertEquals(result, "Backward compatibility test");
    }

    @Test
    public void testUncompressedClientToCompressedServer() throws Exception {
        // Server has compression enabled
        server.enableCompression();

        // Client has compression disabled (will send with prefix though)
        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port);
        DataService client = factory.getInstance();

        String result = client.echo("Test message");
        assertEquals(result, "Test message");
    }

    @Test
    public void testLargeInputCompression() throws Exception {
        server.enableCompression();

        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                .withCompression();
        DataService client = factory.getInstance();

        // Send large input
        String largeInput = "X".repeat(10000);
        String result = client.processLargeInput(largeInput);
        assertEquals(result, "Processed: 10000 bytes");
    }

    @Test
    public void testCompressionWithDifferentLevels() throws Exception {
        // Test different compression levels
        for (int level : new int[]{1, 5, 9}) {
            server.setCompressionConfig(new CompressionConfig(true, 100, level));

            TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                    .withCompression(new CompressionConfig(true, 100, level));
            DataService client = factory.getInstance();

            String result = client.echo("Level " + level + " test");
            assertEquals(result, "Level " + level + " test");
        }
    }

    @Test
    public void testSmallDataNotCompressed() throws Exception {
        // Set high threshold so small messages won't be compressed
        server.setCompressionConfig(new CompressionConfig(true, 10000, 6));

        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                .withCompression(new CompressionConfig(true, 10000, 6));
        DataService client = factory.getInstance();

        String result = client.echo("Small message");
        assertEquals(result, "Small message");
    }

    @Test
    public void testMultipleRequestsWithCompression() throws Exception {
        server.enableCompression();

        TcpRestClientFactory factory = new TcpRestClientFactory(DataService.class, "localhost", port)
                .withCompression();
        DataService client = factory.getInstance();

        // Multiple requests should all work
        for (int i = 0; i < 10; i++) {
            String result = client.echo("Request " + i);
            assertEquals(result, "Request " + i);
        }
    }
}
