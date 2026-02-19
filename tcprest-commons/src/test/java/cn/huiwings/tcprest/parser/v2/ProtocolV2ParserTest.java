package cn.huiwings.tcprest.parser.v2;

import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.mapper.RawTypeMapper;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.server.Context;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Base64;

import static org.testng.Assert.*;

/**
 * Tests for ProtocolV2Parser.
 */
public class ProtocolV2ParserTest {

    private ProtocolV2Parser parser;

    @BeforeClass
    public void setup() {
        parser = new ProtocolV2Parser();
    }

    // ========== Test Basic Extraction ==========

    @Test
    public void testExtract_primitiveInt() throws Exception {
        String request = buildRequest("add", "(II)", base64("1"), base64("2"));

        Context context = parser.parse(request);

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

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getName(), "add");
        assertEquals(context.getTargetMethod().getParameterTypes()[0], double.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], double.class);
        assertEquals(context.getParams()[0], 1.5);
        assertEquals(context.getParams()[1], 2.5);
    }

    @Test
    public void testExtract_string() throws Exception {
        String request = buildRequest("echo", "(Ljava/lang/String;)", base64("hello"));

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getName(), "echo");
        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getParams()[0], "hello");
    }

    @Test
    public void testExtract_mixed() throws Exception {
        String request = buildRequest("process",
                "(Ljava/lang/String;IZ)",
                base64("test"), base64("42"), base64("true"));

        Context context = parser.parse(request);

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

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getName(), "noParams");
        assertEquals(context.getParams().length, 0);
    }

    @Test
    public void testExtract_nullParameter() throws Exception {
        String request = buildRequest("echo", "(Ljava/lang/String;)", "~");  // ~ marker for null

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getName(), "echo");
        assertNull(context.getParams()[0]);
    }

    // ========== Test Overloaded Methods ==========

    @Test
    public void testExtract_overloadedInt() throws Exception {
        String request = buildRequest("add", "(II)", base64("1"), base64("2"));

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], int.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], int.class);
    }

    @Test
    public void testExtract_overloadedDouble() throws Exception {
        String request = buildRequest("add", "(DD)", base64("1.5"), base64("2.5"));

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], double.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], double.class);
    }

    @Test
    public void testExtract_overloadedString() throws Exception {
        String request = buildRequest("add",
                "(Ljava/lang/String;Ljava/lang/String;)",
                base64("hello"), base64("world"));

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], String.class);
    }

    @Test
    public void testExtract_overloadedMixed() throws Exception {
        String request = buildRequest("process",
                "(Ljava/lang/String;I)",
                base64("test"), base64("42"));

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], String.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], int.class);
    }

    @Test
    public void testExtract_overloadedMixedReverse() throws Exception {
        String request = buildRequest("process",
                "(ILjava/lang/String;)",
                base64("42"), base64("test"));

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getParameterTypes()[0], int.class);
        assertEquals(context.getTargetMethod().getParameterTypes()[1], String.class);
    }

    // ========== Test Type Conversions ==========

    /**
     * Object array parameter: server receives Base64-serialized array (not "[...]" format).
     * Verifies Priority 1 in parseParameter handles array types with non-primitive component.
     */
    @Test
    public void testExtract_objectArrayParameter() throws Exception {
        PersonDto[] input = {
            new PersonDto("Alice", 25),
            new PersonDto("Bob", 30)
        };
        RawTypeMapper rawMapper = new RawTypeMapper();
        String serializedBase64 = rawMapper.objectToString(input);

        // Signature for PersonDto[]: [L fully.qualified.PersonDto;
        String signature = "([L" + PersonDto.class.getName().replace('.', '/') + ";)";
        String meta = TestServiceWithArray.class.getName() + "/echoPersonArray" + signature;
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String paramsArray = "[" + serializedBase64 + "]";
        String request = "V2|0|{{" + metaBase64 + "}}|" + paramsArray;

        Context context = parser.parse(request);

        assertEquals(context.getTargetMethod().getName(), "echoPersonArray");
        assertEquals(context.getTargetMethod().getParameterTypes().length, 1);
        assertTrue(context.getTargetMethod().getParameterTypes()[0].isArray());
        assertEquals(context.getTargetMethod().getParameterTypes()[0].getComponentType(), PersonDto.class);

        PersonDto[] decoded = (PersonDto[]) context.getParams()[0];
        assertNotNull(decoded);
        assertEquals(decoded.length, 2);
        assertEquals(decoded[0].getName(), "Alice");
        assertEquals(decoded[0].getAge(), 25);
        assertEquals(decoded[1].getName(), "Bob");
        assertEquals(decoded[1].getAge(), 30);
    }

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

        Context context = parser.parse(request);

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

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_nullRequest() throws Exception {
        parser.parse(null);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_emptyRequest() throws Exception {
        parser.parse("");
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_notV2Request() throws Exception {
        parser.parse("SomeClass/method()()");
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_invalidFormat() throws Exception {
        parser.parse("V2|0");
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_missingClassMethodSeparator() throws Exception {
        parser.parse("V2|0|TestServiceadd(II)()");
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_missingSignature() throws Exception {
        // Invalid format: missing method signature
        String meta = "TestService/add";  // Missing (SIGNATURE)
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String request = "V2|0|" + metaBase64 + "|";
        parser.parse(request);
    }

    @Test(expectedExceptions = ClassNotFoundException.class)
    public void testExtract_classNotFound() throws Exception {
        // Non-existent class (new simplified format)
        String meta = "NonExistentClass/method()";
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String metaWrapped = "{{" + metaBase64 + "}}";
        String paramsArray = "[" + base64("test") + "]";
        String request = "V2|0|" + metaWrapped + "|" + paramsArray;
        parser.parse(request);
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testExtract_methodNotFound() throws Exception {
        String request = buildRequest("nonExistent", "(II)", base64("1"), base64("2"));
        parser.parse(request);
    }

    @Test(expectedExceptions = NoSuchMethodException.class)
    public void testExtract_wrongSignature() throws Exception {
        String request = buildRequest("add", "(FFF)", base64("1.0"), base64("2.0"), base64("3.0"));
        parser.parse(request);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_parameterCountMismatch() throws Exception {
        // Signature says 2 params, but only 1 provided
        String request = buildRequest("add", "(II)", base64("1"));
        parser.parse(request);
    }

    @Test(expectedExceptions = ProtocolException.class)
    public void testExtract_invalidParameterFormat() throws Exception {
        // Invalid parameter format (not {{base64}})
        String meta = TestService.class.getName() + "/add(II)";
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String paramsBase64 = ProtocolSecurity.encodeComponent("INVALID_PARAM");  // Missing {{}} wrapper
        String request = "V2|0|" + metaBase64 + "|" + paramsBase64;
        parser.parse(request);
    }

    // ========== Test Static Methods ==========

    @Test
    public void testIsV2Request_valid() {
        assertTrue(ProtocolV2Parser.isV2Request("V2|0|TestService/method()()"));
    }

    @Test
    public void testIsV2Request_invalid() {
        assertFalse(ProtocolV2Parser.isV2Request("TestService/method()()"));
    }

    @Test
    public void testIsV2Request_null() {
        assertFalse(ProtocolV2Parser.isV2Request(null));
    }

    @Test
    public void testIsV2Request_empty() {
        assertFalse(ProtocolV2Parser.isV2Request(""));
    }

    // ========== Helper Methods ==========

    /**
     * Build secure V2 request.
     *
     * <p>New format: V2|0|{{base64(ClassName/methodName(SIGNATURE))}}|{{base64(PARAMS)}}</p>
     *
     * @param methodName method name
     * @param signature method signature (e.g., "(II)")
     * @param params base64-encoded parameters
     * @return secure V2 request string
     */
    /**
     * Build a V2 request with simplified format.
     *
     * <p><b>New format:</b> V2|0|{{base64(META)}}|[param1,param2,param3]</p>
     */
    private String buildRequest(String methodName, String signature, String... params) {
        // Step 1: Build metadata (ClassName/methodName(SIGNATURE))
        String meta = TestService.class.getName() + "/" + methodName + signature;

        // Step 2: Build parameters array (JSON-style format)
        StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append("[");
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                paramsBuilder.append(",");
            }
            paramsBuilder.append(params[i]);
        }
        paramsBuilder.append("]");
        String paramsArray = paramsBuilder.toString();

        // Step 3: Encode metadata and wrap with {{}}
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String metaWrapped = "{{" + metaBase64 + "}}";

        // Step 4: Build protocol message: V2|0|{{META}}|[PARAMS]
        return "V2|0|" + metaWrapped + "|" + paramsArray;
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

    /** DTO for object-array parser test. */
    public static class PersonDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int age;

        public PersonDto() {}

        public PersonDto(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    /** Service with object-array method for parser test. */
    public interface TestServiceWithArray {
        PersonDto[] echoPersonArray(PersonDto[] arr);
    }
}
