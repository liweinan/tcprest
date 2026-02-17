package cn.huiwings.tcprest.test.security;

import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.exception.SecurityException;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

/**
 * Security utility tests.
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class SecurityTest {

    @Test
    public void testEncodeDecodeComponent() {
        String original = "cn.huiwings.tcprest.test.HelloWorld/sayHello";
        String encoded = ProtocolSecurity.encodeComponent(original);

        System.out.println("Original: " + original);
        System.out.println("Encoded:  " + encoded);

        // Encoded string should be different
        assertNotEquals(original, encoded);

        // Should not contain special characters that could cause issues
        assertFalse(encoded.contains("/"));
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("="));

        // Decode should give back original
        String decoded = ProtocolSecurity.decodeComponent(encoded);
        assertEquals(decoded, original);
    }

    @Test
    public void testEncodeDecodeSpecialCharacters() {
        String original = "Test/with/../path()|&special?chars=value";
        String encoded = ProtocolSecurity.encodeComponent(original);
        String decoded = ProtocolSecurity.decodeComponent(encoded);

        assertEquals(decoded, original);
    }

    @Test(expectedExceptions = SecurityException.class)
    public void testDecodeEmptyComponent() {
        ProtocolSecurity.decodeComponent("");
    }

    @Test(expectedExceptions = SecurityException.class)
    public void testDecodeNullComponent() {
        ProtocolSecurity.decodeComponent(null);
    }

    @Test
    public void testCRC32Checksum() {
        String message = "0|metadata|params";
        SecurityConfig config = new SecurityConfig().enableCRC32();

        String checksum1 = ProtocolSecurity.calculateChecksum(message, config);
        String checksum2 = ProtocolSecurity.calculateChecksum(message, config);

        // Same message should produce same checksum
        assertEquals(checksum1, checksum2);

        // Checksum should have correct format
        assertTrue(checksum1.startsWith("CHK:"));

        // Verify checksum
        assertTrue(ProtocolSecurity.verifyChecksum(message, checksum1, config));

        // Modified message should fail verification
        String tamperedMessage = "0|metadata|TAMPERED";
        assertFalse(ProtocolSecurity.verifyChecksum(tamperedMessage, checksum1, config));
    }

    @Test
    public void testHMACChecksum() {
        String message = "0|metadata|params";
        SecurityConfig config = new SecurityConfig().enableHMAC("my-secret-key");

        String checksum = ProtocolSecurity.calculateChecksum(message, config);

        assertTrue(checksum.startsWith("CHK:"));
        assertTrue(ProtocolSecurity.verifyChecksum(message, checksum, config));

        // Tampering detection
        assertFalse(ProtocolSecurity.verifyChecksum(message + "x", checksum, config));
    }

    @Test
    public void testChecksumDisabled() {
        String message = "test";
        SecurityConfig config = new SecurityConfig(); // No checksum

        String checksum = ProtocolSecurity.calculateChecksum(message, config);
        assertEquals(checksum, ""); // Empty when disabled

        // Verification always succeeds when disabled
        assertTrue(ProtocolSecurity.verifyChecksum(message, "", config));
    }

    @Test
    public void testSplitChecksum() {
        String messageWithChecksum = "0|meta|params|CHK:abc123";
        String[] parts = ProtocolSecurity.splitChecksum(messageWithChecksum);

        assertEquals(parts[0], "0|meta|params");
        assertEquals(parts[1], "CHK:abc123");

        // Message without checksum
        String messageNoChecksum = "0|meta|params";
        String[] parts2 = ProtocolSecurity.splitChecksum(messageNoChecksum);

        assertEquals(parts2[0], "0|meta|params");
        assertEquals(parts2[1], "");
    }

    @Test
    public void testValidClassName() {
        // Valid class names
        assertTrue(ProtocolSecurity.isValidClassName("com.example.MyClass"));
        assertTrue(ProtocolSecurity.isValidClassName("HelloWorld"));
        assertTrue(ProtocolSecurity.isValidClassName("java.util.List"));
        assertTrue(ProtocolSecurity.isValidClassName("$MyClass"));
        assertTrue(ProtocolSecurity.isValidClassName("_MyClass"));

        // Invalid class names (injection attempts)
        assertFalse(ProtocolSecurity.isValidClassName("com/example/MyClass")); // Contains /
        assertFalse(ProtocolSecurity.isValidClassName("../../../EvilClass")); // Path traversal
        assertFalse(ProtocolSecurity.isValidClassName("com.example..MyClass")); // Path traversal
        assertFalse(ProtocolSecurity.isValidClassName("com.example.MyClass<script>")); // XSS attempt
        assertFalse(ProtocolSecurity.isValidClassName("")); // Empty
        assertFalse(ProtocolSecurity.isValidClassName(null)); // Null
    }

    @Test
    public void testValidMethodName() {
        // Valid method names
        assertTrue(ProtocolSecurity.isValidMethodName("sayHello"));
        assertTrue(ProtocolSecurity.isValidMethodName("getValue"));
        assertTrue(ProtocolSecurity.isValidMethodName("_privateMethod"));
        assertTrue(ProtocolSecurity.isValidMethodName("$method"));

        // Invalid method names
        assertFalse(ProtocolSecurity.isValidMethodName("say/Hello")); // Contains /
        assertFalse(ProtocolSecurity.isValidMethodName("say.Hello")); // Contains .
        assertFalse(ProtocolSecurity.isValidMethodName("say(Hello)")); // Contains ()
        assertFalse(ProtocolSecurity.isValidMethodName("")); // Empty
        assertFalse(ProtocolSecurity.isValidMethodName(null)); // Null
    }

    @Test
    public void testClassWhitelist() {
        SecurityConfig config = new SecurityConfig()
                .enableClassWhitelist()
                .allowClass("com.example.AllowedClass")
                .allowClasses("com.example.Class1", "com.example.Class2");

        // Allowed classes
        assertTrue(config.isClassAllowed("com.example.AllowedClass"));
        assertTrue(config.isClassAllowed("com.example.Class1"));
        assertTrue(config.isClassAllowed("com.example.Class2"));

        // Not allowed
        assertFalse(config.isClassAllowed("com.example.EvilClass"));
        assertFalse(config.isClassAllowed("java.lang.Runtime"));

        // Whitelist disabled - all allowed
        SecurityConfig configNoWhitelist = new SecurityConfig();
        assertTrue(configNoWhitelist.isClassAllowed("any.class.Name"));
    }

    @Test
    public void testSecurityConfigChaining() {
        SecurityConfig config = new SecurityConfig()
                .enableCRC32()
                .enableClassWhitelist()
                .allowClass("Test");

        assertTrue(config.isChecksumEnabled());
        assertTrue(config.isClassWhitelistEnabled());
        assertTrue(config.isClassAllowed("Test"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testHMACWithNullSecret() {
        new SecurityConfig().enableHMAC(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testHMACWithEmptySecret() {
        new SecurityConfig().enableHMAC("");
    }
}
