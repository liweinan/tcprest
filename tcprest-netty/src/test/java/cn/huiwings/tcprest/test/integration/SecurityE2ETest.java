package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * End-to-end test for SecurityConfig with Netty server.
 *
 * <p>Tests complete client-server communication with security features:</p>
 * <ul>
 *   <li>CRC32 checksum verification</li>
 *   <li>HMAC-SHA256 message authentication</li>
 *   <li>Class whitelist access control</li>
 *   <li>Security violation detection</li>
 * </ul>
 *
 * <p>This E2E test verifies that security features work correctly across
 * the network with real Netty server.</p>
 *
 * @author Weinan Li
 * @date 2026-02-19
 */
public class SecurityE2ETest {

    // ========== Service Interface ==========

    public interface SecureService {
        String publicMethod(String input);
        int calculate(int a, int b);
        List<String> getItems();
    }

    public interface RestrictedService {
        String restrictedMethod();
    }

    public static class SecureServiceImpl implements SecureService {
        @Override
        public String publicMethod(String input) {
            return "Processed: " + input;
        }

        @Override
        public int calculate(int a, int b) {
            return a + b;
        }

        @Override
        public List<String> getItems() {
            List<String> items = new ArrayList<>();
            items.add("item1");
            items.add("item2");
            items.add("item3");
            return items;
        }
    }

    public static class RestrictedServiceImpl implements RestrictedService {
        @Override
        public String restrictedMethod() {
            return "This should not be accessible";
        }
    }

    // ========== Test 1: CRC32 Checksum ==========

    @Test
    public void testCRC32ChecksumE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            // Create server with CRC32 security
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig().enableCRC32();
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client with matching CRC32 security
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            SecurityConfig clientConfig = new SecurityConfig().enableCRC32();
            factory.withSecurity(clientConfig);
            SecureService client = factory.getClient();

            // Test basic method call with CRC32
            String result = client.publicMethod("test");
            assertEquals(result, "Processed: test");

            // Test method with multiple parameters
            int sum = client.calculate(10, 20);
            assertEquals(sum, 30);

            // Test collection return
            List<String> items = client.getItems();
            assertEquals(items.size(), 3);
            assertEquals(items.get(0), "item1");

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    @Test(expectedExceptions = Exception.class)
    public void testCRC32MismatchDetection() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            // Create server with CRC32
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig().enableCRC32();
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client WITHOUT CRC32 (mismatch)
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            // No security config - client sends requests without checksum
            SecureService client = factory.getClient();

            // This should fail due to checksum mismatch
            client.publicMethod("test");
            fail("Should have thrown exception due to missing checksum");

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    // ========== Test 2: HMAC-SHA256 ==========

    @Test
    public void testHMACSecurityE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            String secret = "test-secret-key-min-32-characters-long-12345";

            // Create server with HMAC
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig().enableHMAC(secret);
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client with matching HMAC secret
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            SecurityConfig clientConfig = new SecurityConfig().enableHMAC(secret);
            factory.withSecurity(clientConfig);
            SecureService client = factory.getClient();

            // Test authenticated call
            String result = client.publicMethod("secure");
            assertEquals(result, "Processed: secure");

            // Test multiple calls with same HMAC
            assertEquals(client.calculate(5, 7), 12);
            assertEquals(client.calculate(100, 200), 300);

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    @Test(expectedExceptions = Exception.class)
    public void testHMACWrongSecretDetection() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            String serverSecret = "server-secret-key-min-32-characters-long-12345";
            String clientSecret = "client-secret-key-min-32-characters-long-WRONG";

            // Create server with secret
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig().enableHMAC(serverSecret);
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client with DIFFERENT secret
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            SecurityConfig clientConfig = new SecurityConfig().enableHMAC(clientSecret);
            factory.withSecurity(clientConfig);
            SecureService client = factory.getClient();

            // This should fail due to HMAC mismatch
            client.publicMethod("test");
            fail("Should have thrown exception due to HMAC mismatch");

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    // ========== Test 3: Class Whitelist ==========

    @Test
    public void testClassWhitelistAllowedE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            // Create server with whitelist
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig()
                .enableClassWhitelist()
                .allowClass(SecureService.class.getName());
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            SecureService client = factory.getClient();

            // This should work - SecureService is whitelisted
            String result = client.publicMethod("allowed");
            assertEquals(result, "Processed: allowed");

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    @Test(expectedExceptions = Exception.class)
    public void testClassWhitelistBlockedE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            // Create server with whitelist that DOES NOT include RestrictedService
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig()
                .enableClassWhitelist()
                .allowClass(SecureService.class.getName());
            // Note: RestrictedService is NOT in whitelist
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new RestrictedServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client for RestrictedService
            TcpRestClientFactory factory = new TcpRestClientFactory(
                RestrictedService.class, "localhost", port
            );
            RestrictedService client = factory.getClient();

            // This should fail - RestrictedService is not whitelisted
            client.restrictedMethod();
            fail("Should have thrown exception due to whitelist violation");

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    // ========== Test 4: Combined Security ==========

    @Test
    public void testCombinedSecurityFeaturesE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            String secret = "combined-secret-key-min-32-characters-long-123";

            // Create server with HMAC + Whitelist
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig()
                .enableHMAC(secret)
                .enableClassWhitelist()
                .allowClass(SecureService.class.getName());
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client with matching security
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            SecurityConfig clientConfig = new SecurityConfig().enableHMAC(secret);
            factory.withSecurity(clientConfig);
            SecureService client = factory.getClient();

            // Test with combined security features
            String result = client.publicMethod("secure-and-whitelisted");
            assertEquals(result, "Processed: secure-and-whitelisted");

            // Test multiple operations
            assertEquals(client.calculate(1, 2), 3);
            assertEquals(client.calculate(10, 20), 30);

            List<String> items = client.getItems();
            assertEquals(items.size(), 3);

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }

    // ========== Test 5: Security with Complex Objects ==========

    @Test
    public void testSecurityWithComplexObjectsE2E() throws Exception {
        int port = PortGenerator.get();
        TcpRestServer server = null;

        try {
            String secret = "complex-secret-key-min-32-characters-long-1234";

            // Create server with HMAC
            server = new NettyTcpRestServer(port);
            SecurityConfig serverConfig = new SecurityConfig().enableHMAC(secret);
            server.setSecurityConfig(serverConfig);
            server.addSingletonResource(new SecureServiceImpl());
            server.up();
            Thread.sleep(500);

            // Create client with matching HMAC
            TcpRestClientFactory factory = new TcpRestClientFactory(
                SecureService.class, "localhost", port
            );
            SecurityConfig clientConfig = new SecurityConfig().enableHMAC(secret);
            factory.withSecurity(clientConfig);
            SecureService client = factory.getClient();

            // Test with collection (complex object)
            List<String> items = client.getItems();
            assertNotNull(items);
            assertEquals(items.size(), 3);
            assertEquals(items.get(0), "item1");
            assertEquals(items.get(1), "item2");
            assertEquals(items.get(2), "item3");

        } finally {
            if (server != null) {
                server.down();
                Thread.sleep(300);
            }
        }
    }
}
