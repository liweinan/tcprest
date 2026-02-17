package cn.huiwings.tcprest.extractor.v2;

import cn.huiwings.tcprest.server.Context;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Base64;

import static org.testng.Assert.*;

/**
 * Tests for ProtocolV2Extractor.
 */
public class ProtocolV2ExtractorTest {

    private ProtocolV2Extractor extractor;

    @BeforeClass
    public void setup() {
        extractor = new ProtocolV2Extractor();
    }

    // ========== Test Basic Extraction ==========

    @Test
    public void testExtract_primitiveInt() throws Exception {
        String request = buildRequest("add", "(II)", base64("1"), base64("2"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetClass().getName(), TestService.class.getName());
        assertEquals(context.getTargetMethod().getName(), "add");
        assertEquals(context.getTargetMethod().getName(), "add");
        assertEquals(context.getTargetMethod().getParameterTypes()[0], int.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], int.class);
        assertEquals(context.getParams().length, 2);
        assertEquals(context.getParams()[0], 1);
        assertEquals(context.getParams()[1], 2);
    }

    @Test
    public void testExtract_primitiveDouble() throws Exception {
        String request = buildRequest("add", "(DD)", base64("1.5"), base64("2.5"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getName(), "add");
        assertEquals(context.getTargetMethod().getParameterTypes()[0], double.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], double.class);
        assertEquals(context.getParams()[0], 1.5);
        assertEquals(context.getParams()[1], 2.5);
    }

    @Test
    public void testExtract_string() throws Exception {
        String request = buildRequest("echo", "(Ljava/lang/String;)", base64("hello"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getName(), "echo");
        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getParams()[0], "hello");
    }

    @Test
    public void testExtract_mixed() throws Exception {
        String request = buildRequest("process",
                "(Ljava/lang/String;IZ)",
                base64("test"), base64("42"), base64("true"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getName(), "process");
        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], int.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[2], boolean.class);
        assertEquals(context.getParams()[0], "test");
        assertEquals(context.getParams()[1], 42);
        assertEquals(context.getParams()[2], true);
    }

    @Test
    public void testExtract_noParameters() throws Exception {
        String request = buildRequest("noParams", "()", new String[0]);

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getName(), "noParams");
        assertEquals(context.getParams().length, 0);
    }

    @Test
    public void testExtract_nullParameter() throws Exception {
        String request = buildRequest("echo", "(Ljava/lang/String;)", "NULL");  // NULL marker

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getName(), "echo");
        assertNull(context.getParams()[0]);
    }

    // ========== Test Overloaded Methods ==========

    @Test
    public void testExtract_overloadedInt() throws Exception {
        String request = buildRequest("add", "(II)", base64("1"), base64("2"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], int.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], int.class);
    }

    @Test
    public void testExtract_overloadedDouble() throws Exception {
        String request = buildRequest("add", "(DD)", base64("1.5"), base64("2.5"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], double.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], double.class);
    }

    @Test
    public void testExtract_overloadedString() throws Exception {
        String request = buildRequest("add",
                "(Ljava/lang/String;Ljava/lang/String;)",
                base64("hello"), base64("world"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], String.class);
    }

    @Test
    public void testExtract_overloadedMixed() throws Exception {
        String request = buildRequest("process",
                "(Ljava/lang/String;I)",
                base64("test"), base64("42"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], int.class);
    }

    @Test
    public void testExtract_overloadedMixedReverse() throws Exception {
        String request = buildRequest("process",
                "(ILjava/lang/String;)",
                base64("42"), base64("test"));

        Context context = extractor.extract(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], int.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], String.class);
    }

    // ========== Test Type Conversions ==========

    @Test
    public void testExtract_allPrimitiveTypes() throws Exception {
        String request = buildRequest("allTypes",
                "(BSIJFDZC)",
                base64("127"),      // byte
                base64("32767"),    // short
                base64("42"),       // int
                base64("123456789"),// long
                base64("3.14"),     // float
                base64("2.718"),    // double
                base64("true"),     // boolean
                base64("A"));       // char

        Context context = extractor.extract(request);

        assertEquals(context.getParams()[0], (byte) 127);
        assertEquals(context.getParams()[1], (short) 32767);
        assertEquals(context.getParams()[2], 42);
        assertEquals(context.getParams()[3], 123456789L);
        assertEquals((Float) context.getParams()[4], 3.14f, 0.001);
        assertEquals((Double) context.getParams()[5], 2.718, 0.001);
        assertEquals(context.getParams()[6], true);
        assertEquals(context.getParams()[7], 'A');
    }

    // ========== Test Error Handling ==========

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_nullRequest() throws Exception {
        extractor.extract(null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_emptyRequest() throws Exception {
        extractor.extract("");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_notV2Request() throws Exception {
        extractor.extract("SomeClass/method()()");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_invalidFormat() throws Exception {
        extractor.extract("V2|0");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_missingClassMethodSeparator() throws Exception {
        extractor.extract("V2|0|TestServiceadd(II)()");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_missingSignature() throws Exception {
        extractor.extract("V2|0|TestService/add");
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void testExtract_classNotFound() throws Exception {
        String request = "V2|0|NonExistentClass/method()({{" + base64("test") + "}})";
        extractor.extract(request);
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testExtract_methodNotFound() throws Exception {
        String request = buildRequest("nonExistent", "(II)", base64("1"), base64("2"));
        extractor.extract(request);
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testExtract_wrongSignature() throws Exception {
        String request = buildRequest("add", "(FFF)", base64("1.0"), base64("2.0"), base64("3.0"));
        extractor.extract(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_parameterCountMismatch() throws Exception {
        // Signature says 2 params, but only 1 provided
        String request = buildRequest("add", "(II)", base64("1"));
        extractor.extract(request);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testExtract_invalidParameterFormat() throws Exception {
        String request = "V2|0|" + TestService.class.getName() + "/add(II)(INVALID_PARAM)";
        extractor.extract(request);
    }

    // ========== Test Static Methods ==========

    @Test
    public void testIsV2Request_valid() {
        assertTrue(ProtocolV2Extractor.isV2Request("V2|0|TestService/method()()"));
    }

    @Test
    public void testIsV2Request_invalid() {
        assertFalse(ProtocolV2Extractor.isV2Request("TestService/method()()"));
    }

    @Test
    public void testIsV2Request_null() {
        assertFalse(ProtocolV2Extractor.isV2Request(null));
    }

    @Test
    public void testIsV2Request_empty() {
        assertFalse(ProtocolV2Extractor.isV2Request(""));
    }

    // ========== Helper Methods ==========

    private String buildRequest(String methodName, String signature, String... params) {
        StringBuilder sb = new StringBuilder();
        sb.append("V2|0|");
        sb.append(TestService.class.getName());
        sb.append("/");
        sb.append(methodName);
        sb.append(signature);
        sb.append("(");

        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                sb.append(":::");
            }
            sb.append("{{");
            sb.append(params[i]);
            sb.append("}}");
        }

        sb.append(")");
        return sb.toString();
    }

    private String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes());
    }

    // ========== Test Service Interface ==========

    public interface TestService {
        int add(int a, int b);
        double add(double a, double b);
        String add(String a, String b);
        String echo(String s);
        void process(String s, int i, boolean b);
        void process(String s, int i);
        void process(int i, String s);
        void noParams();
        void allTypes(byte b, short s, int i, long l, float f, double d, boolean bool, char c);
    }
}
