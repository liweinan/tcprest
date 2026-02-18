package cn.huiwings.tcprest.codec.v2;

import cn.huiwings.tcprest.protocol.v2.StatusCode;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.testng.Assert.*;

/**
 * Tests for ProtocolV2Codec.
 */
public class ProtocolV2CodecTest {

    private ProtocolV2Codec codec;

    @BeforeClass
    public void setup() {
        codec = new ProtocolV2Codec();
    }

    // ========== Test Encoding Requests ==========

    @Test
    public void testEncode_primitiveInt() throws Exception {
        Method method = TestService.class.getMethod("add", int.class, int.class);
        Object[] params = {1, 2};

        String encoded = codec.encode(TestService.class, method, params, null);

        // Verify secure format: V2|0|{{base64(meta)}}|{{base64(params)}}
        String fullClassName = TestService.class.getName();
        assertSecureV2Request(encoded, fullClassName + "/add(II)");
    }

    @Test
    public void testEncode_primitiveDouble() throws Exception {
        Method method = TestService.class.getMethod("add", double.class, double.class);
        Object[] params = {1.5, 2.5};

        String encoded = codec.encode(TestService.class, method, params, null);

        String fullClassName = TestService.class.getName();
        assertSecureV2Request(encoded, fullClassName + "/add(DD)");
    }

    @Test
    public void testEncode_string() throws Exception {
        Method method = TestService.class.getMethod("echo", String.class);
        Object[] params = {"hello"};

        String encoded = codec.encode(TestService.class, method, params, null);

        String fullClassName = TestService.class.getName();
        assertSecureV2Request(encoded, fullClassName + "/echo(Ljava/lang/String;)");
    }

    @Test
    public void testEncode_mixed() throws Exception {
        Method method = TestService.class.getMethod("process", String.class, int.class, boolean.class);
        Object[] params = {"test", 42, true};

        String encoded = codec.encode(TestService.class, method, params, null);

        String fullClassName = TestService.class.getName();
        assertSecureV2Request(encoded, fullClassName + "/process(Ljava/lang/String;IZ)");
    }

    @Test
    public void testEncode_noParameters() throws Exception {
        Method method = TestService.class.getMethod("noParams");
        Object[] params = {};

        String encoded = codec.encode(TestService.class, method, params, null);

        String fullClassName = TestService.class.getName();
        assertSecureV2Request(encoded, fullClassName + "/noParams()");
    }

    @Test
    public void testEncode_nullParameter() throws Exception {
        Method method = TestService.class.getMethod("echo", String.class);
        Object[] params = {null};

        String encoded = codec.encode(TestService.class, method, params, null);

        // Verify secure format
        String fullClassName = TestService.class.getName();
        assertSecureV2Request(encoded, fullClassName + "/echo(Ljava/lang/String;)");

        // Verify ~ marker for null parameter in array format
        String[] parts = ProtocolSecurity.splitChecksum(encoded)[0].split("\\|", 4);
        String paramsArray = parts[3];
        assertTrue(paramsArray.equals("[~]"), "Should contain ~ marker for null in array: " + paramsArray);
    }

    // Compression is handled separately (not in this converter)
    // Removed testEncode_withCompression()

    // ========== Test Decoding Responses ==========

    @Test
    public void testDecode_successInt() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("42".getBytes());
        String response = "V2|0|0|{{" + base64 + "}}";

        Object result = codec.decode(response, int.class);

        assertEquals(result, 42);
    }

    @Test
    public void testDecode_successDouble() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("3.14".getBytes());
        String response = "V2|0|0|{{" + base64 + "}}";

        Object result = codec.decode(response, double.class);

        assertEquals(result, 3.14);
    }

    @Test
    public void testDecode_successString() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("hello".getBytes());
        String response = "V2|0|0|{{" + base64 + "}}";

        Object result = codec.decode(response, String.class);

        assertEquals(result, "hello");
    }

    @Test
    public void testDecode_successBoolean() throws Exception {
        String base64 = Base64.getEncoder().encodeToString("true".getBytes());
        String response = "V2|0|0|{{" + base64 + "}}";

        Object result = codec.decode(response, boolean.class);

        assertEquals(result, true);
    }

    @Test
    public void testDecode_successNull() throws Exception {
        String response = "V2|0|0|null";

        Object result = codec.decode(response, String.class);

        assertNull(result);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDecode_businessException() throws Exception {
        String errorMsg = "ValidationException: Invalid input";
        String base64 = Base64.getEncoder().encodeToString(errorMsg.getBytes());
        String response = "V2|0|1|{{" + base64 + "}}";

        codec.decode(response, String.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDecode_serverError() throws Exception {
        String errorMsg = "NullPointerException: Object is null";
        String base64 = Base64.getEncoder().encodeToString(errorMsg.getBytes());
        String response = "V2|0|2|{{" + base64 + "}}";

        codec.decode(response, String.class);
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testDecode_protocolError() throws Exception {
        String response = "V2|0|3|Malformed request";

        codec.decode(response, String.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testDecode_invalidFormat() throws Exception {
        String response = "V2|0|0"; // Missing body

        codec.decode(response, String.class);
    }

    @Test
    public void testDecode_emptyResponse() throws Exception {
        Object result = codec.decode("", String.class);

        assertNull(result);
    }

    @Test
    public void testDecode_nullResponse() throws Exception {
        Object result = codec.decode(null, String.class);

        assertNull(result);
    }

    // ========== Test Encoding Responses ==========

    @Test
    public void testEncodeResponse_success() {
        String response = codec.encodeResponse(42, StatusCode.SUCCESS);

        assertTrue(response.startsWith("V2|0|0|"));
        assertTrue(response.contains("{{"));
        assertTrue(response.contains("}}"));
    }

    @Test
    public void testEncodeResponse_successString() {
        String response = codec.encodeResponse("hello", StatusCode.SUCCESS);

        assertTrue(response.startsWith("V2|0|0|"));
        String base64 = Base64.getEncoder().encodeToString("hello".getBytes());
        assertTrue(response.contains(base64));
    }

    @Test
    public void testEncodeResponse_successNull() {
        String response = codec.encodeResponse(null, StatusCode.SUCCESS);

        assertTrue(response.startsWith("V2|0|0|"));
        assertTrue(response.contains("null"));
    }

    // Compression is handled separately (not in this converter)
    // Removed testEncodeResponse_withCompression()

    // ========== Test Encoding Exceptions ==========

    @Test
    public void testEncodeException_businessException() {
        Exception ex = new IllegalArgumentException("Invalid input");

        String response = codec.encodeException(ex, StatusCode.BUSINESS_EXCEPTION);

        assertTrue(response.startsWith("V2|0|1|"));
        assertTrue(response.contains("{{"));

        // Decode and verify
        String[] parts = response.split("\\|", 4);
        String body = parts[3];
        String base64 = body.substring(2, body.length() - 2);
        String decoded = new String(Base64.getDecoder().decode(base64));
        assertTrue(decoded.contains("IllegalArgumentException"));
        assertTrue(decoded.contains("Invalid input"));
    }

    @Test
    public void testEncodeException_serverError() {
        Exception ex = new NullPointerException("Object is null");

        String response = codec.encodeException(ex, StatusCode.SERVER_ERROR);

        assertTrue(response.startsWith("V2|0|2|"));

        // Decode and verify
        String[] parts = response.split("\\|", 4);
        String body = parts[3];
        String base64 = body.substring(2, body.length() - 2);
        String decoded = new String(Base64.getDecoder().decode(base64));
        assertTrue(decoded.contains("NullPointerException"));
        assertTrue(decoded.contains("Object is null"));
    }

    @Test
    public void testEncodeException_noMessage() {
        Exception ex = new RuntimeException();

        String response = codec.encodeException(ex, StatusCode.SERVER_ERROR);

        assertTrue(response.startsWith("V2|0|2|"));
    }

    // Compression is handled separately (not in this converter)
    // Removed testEncodeException_withCompression()

    // ========== Test Type Conversion ==========

    @Test
    public void testDecode_allPrimitiveTypes() throws Exception {
        // Test all primitive types
        assertEquals(codec.decode("V2|0|0|{{" + base64("127") + "}}", byte.class), (byte) 127);
        assertEquals(codec.decode("V2|0|0|{{" + base64("32767") + "}}", short.class), (short) 32767);
        assertEquals(codec.decode("V2|0|0|{{" + base64("42") + "}}", int.class), 42);
        assertEquals(codec.decode("V2|0|0|{{" + base64("123456789") + "}}", long.class), 123456789L);
        assertEquals((Float) codec.decode("V2|0|0|{{" + base64("3.14") + "}}", float.class), 3.14f, 0.001);
        assertEquals(codec.decode("V2|0|0|{{" + base64("3.14159") + "}}", double.class), 3.14159);
        assertEquals(codec.decode("V2|0|0|{{" + base64("true") + "}}", boolean.class), true);
        assertEquals(codec.decode("V2|0|0|{{" + base64("A") + "}}", char.class), 'A');
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    /**
     * Helper method to verify simplified V2 request format and extract metadata.
     *
     * <p><b>New simplified format:</b> V2|0|{{base64(meta)}}|[param1,param2,param3]|CHK:value (optional)</p>
     *
     * @param encoded the encoded request
     * @param expectedMeta the expected metadata (e.g., "TestService/add(II)")
     */
    private void assertSecureV2Request(String encoded, String expectedMeta) {
        // Verify prefix
        assertTrue(encoded.startsWith("V2|0|"), "Should start with V2|0|");

        // Split checksum
        String[] checksumParts = ProtocolSecurity.splitChecksum(encoded);
        String messageWithoutChecksum = checksumParts[0];

        // Split components: V2|0|{{metaBase64}}|[params]
        String[] parts = messageWithoutChecksum.split("\\|", 4);
        assertTrue(parts.length >= 3, "Should have at least 3 parts");

        // Extract and unwrap metadata from {{...}}
        String metaWrapped = parts[2];
        assertTrue(metaWrapped.startsWith("{{") && metaWrapped.endsWith("}}"),
                  "Metadata should be wrapped in {{...}}");

        String metaBase64 = metaWrapped.substring(2, metaWrapped.length() - 2);
        String decodedMeta = ProtocolSecurity.decodeComponent(metaBase64);

        // Verify metadata
        assertEquals(decodedMeta, expectedMeta, "Metadata should match");
    }

    // ========== Test Helper Interface ==========

    public interface TestService {
        int add(int a, int b);
        double add(double a, double b);
        String echo(String s);
        void process(String s, int i, boolean b);
        void noParams();
    }
}
