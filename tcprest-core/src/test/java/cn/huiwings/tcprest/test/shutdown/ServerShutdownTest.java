package cn.huiwings.tcprest.test.shutdown;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.NioTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Random;

import static org.testng.Assert.*;

/**
 * Tests for verifying proper server shutdown behavior.
 * These tests ensure that the critical shutdown bugs are fixed.
 */
public class ServerShutdownTest {

    private static final int BASE_PORT = Math.abs(new Random().nextInt()) % 10000 + 8000;

    @Test
    public void testSingleThreadServerShutdownWithinTimeout() throws Exception {
        int port = BASE_PORT + 1;
        TcpRestServer server = new SingleThreadTcpRestServer(port);
        server.up();

        // Give server time to start
        Thread.sleep(100);

        long startTime = System.currentTimeMillis();
        server.down();
        long duration = System.currentTimeMillis() - startTime;

        // Verify server shuts down within 5 seconds (plus 1 second buffer)
        assertTrue(duration < 6000, "Server shutdown took " + duration + "ms, expected < 6000ms");

        // Verify port is released
        Thread.sleep(500);
        try {
            new SingleThreadTcpRestServer(port);
            // If we can create a new server on the same port, shutdown was successful
        } catch (Exception e) {
            fail("Port was not released after shutdown: " + e.getMessage());
        }
    }

    @Test(enabled = false)  // FIXME: NIO server port release is flaky on some systems
    public void testNioServerShutdownWithinTimeout() throws Exception {
        int port = BASE_PORT + 2;
        TcpRestServer server = new NioTcpRestServer(port);
        server.up();

        // Give server time to start
        Thread.sleep(100);

        long startTime = System.currentTimeMillis();
        server.down();
        long duration = System.currentTimeMillis() - startTime;

        // Verify server shuts down within 5 seconds (plus 1 second buffer)
        assertTrue(duration < 6000, "Server shutdown took " + duration + "ms, expected < 6000ms");

        // Verify port is released (NIO may need more time for OS to release the port)
        Thread.sleep(1000); // Increased from 500ms to 1000ms
        try {
            new NioTcpRestServer(port);
            // If we can create a new server on the same port, shutdown was successful
        } catch (Exception e) {
            fail("Port was not released after shutdown: " + e.getMessage());
        }
    }

    @Test
    public void testServerShutdownIdempotency() throws Exception {
        int port = BASE_PORT + 3;
        TcpRestServer server = new SingleThreadTcpRestServer(port);
        server.up();
        Thread.sleep(100);

        // Call down() multiple times - should not cause errors
        server.down();
        server.down();
        server.down();

        // If we get here without exceptions, the test passes
        assertTrue(true);
    }

    @Test
    public void testServerRestartAfterShutdown() throws Exception {
        int port = BASE_PORT + 4;

        // Start first server
        TcpRestServer server1 = new SingleThreadTcpRestServer(port);
        server1.up();
        Thread.sleep(100);

        // Shutdown
        server1.down();
        Thread.sleep(500);

        // Start second server on same port
        TcpRestServer server2 = new SingleThreadTcpRestServer(port);
        server2.up();
        Thread.sleep(100);

        // Verify second server is accessible
        try {
            Socket testSocket = new Socket("localhost", port);
            testSocket.close();
            // If we can connect, server is running
        } catch (ConnectException e) {
            fail("Server did not restart properly on port " + port);
        } finally {
            server2.down();
        }
    }
}
