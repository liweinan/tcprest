package cn.huiwings.tcprest.test.protocol;

import cn.huiwings.tcprest.converter.v2.ProtocolV2Converter;
import cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor;
import cn.huiwings.tcprest.server.Context;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Base64;

import static org.testng.Assert.*;

/**
 * Test for NULL marker conflicts in protocol.
 *
 * This test verifies that the string "NULL" can be safely transmitted
 * without being confused with the null value marker.
 *
 * @author Weinan Li
 * @date 2026-02-18
 */
public class NullConflictTest {

    // ========== V2 Protocol Tests ==========

    @Test
    public void testV2_nullValue() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        Method method = TestService.class.getMethod("echo", String.class);

        // Encode: null parameter
        Object[] params = new Object[]{null};
        String encoded = converter.encode(TestService.class, method, params, null);
        System.out.println("V2 null encoded: " + encoded);

        // Decode: should get null back
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();
        Context context = extractor.extract(encoded);

        assertNotNull(context);
        assertEquals(context.getParams().length, 1);
        assertNull(context.getParams()[0], "Null parameter should be decoded as null");
    }

    @Test
    public void testV2_stringNULL() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        Method method = TestService.class.getMethod("echo", String.class);

        // Encode: string "NULL" parameter
        Object[] params = new Object[]{"NULL"};
        String encoded = converter.encode(TestService.class, method, params, null);
        System.out.println("V2 string 'NULL' encoded: " + encoded);

        // The encoding process is:
        // "NULL" → Base64("NULL") = "TlVMTA==" → {{TlVMTA==}} → Base64("{{TlVMTA==}}") = "e3tUbFZNVEE9PX19"
        // So it should NOT contain the raw "NULL" marker
        assertFalse(encoded.contains("{{NULL}}"), "String 'NULL' should NOT be the raw NULL marker");

        // The outer Base64 encoding should contain the inner Base64-encoded version
        String base64NULL = Base64.getEncoder().encodeToString("NULL".getBytes());
        System.out.println("Base64 of 'NULL': " + base64NULL);
        System.out.println("Looking for pattern: {{" + base64NULL + "}}");

        // Decode: should get string "NULL" back, not null
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();
        Context context = extractor.extract(encoded);

        assertNotNull(context);
        assertEquals(context.getParams().length, 1);
        assertNotNull(context.getParams()[0], "String 'NULL' should NOT be decoded as null");
        assertEquals(context.getParams()[0], "NULL", "String 'NULL' should be preserved");
    }

    @Test
    public void testV2_differentNullValues() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = TestService.class.getMethod("process", String.class, String.class, String.class);

        // Encode: mix of null and string "NULL"
        Object[] params = {null, "NULL", "test"};
        String encoded = converter.encode(TestService.class, method, params, null);
        System.out.println("V2 mixed params encoded: " + encoded);

        // Decode
        Context context = extractor.extract(encoded);

        assertEquals(context.getParams().length, 3);
        assertNull(context.getParams()[0], "First param should be null");
        assertEquals(context.getParams()[1], "NULL", "Second param should be string 'NULL'");
        assertEquals(context.getParams()[2], "test", "Third param should be 'test'");
    }

    // ========== Edge Cases ==========

    @Test
    public void testV2_emptyString() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = TestService.class.getMethod("echo", String.class);

        // Encode: empty string (different from null)
        Object[] params = new Object[]{""};
        String encoded = converter.encode(TestService.class, method, params, null);

        // Decode
        Context context = extractor.extract(encoded);

        assertNotNull(context.getParams()[0], "Empty string is not null");
        assertEquals(context.getParams()[0], "", "Empty string should be preserved");
    }

    @Test
    public void testV2_specialStrings() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = TestService.class.getMethod("echo", String.class);

        String[] testStrings = {
                "NULL",
                "null",
                "Null",
                "TCPREST.NULL",
                "{{NULL}}",
                "CHK:NULL"
        };

        for (String testStr : testStrings) {
            Object[] params = new Object[]{testStr};
            String encoded = converter.encode(TestService.class, method, params, null);
            Context context = extractor.extract(encoded);

            assertNotNull(context.getParams()[0],
                    "String '" + testStr + "' should NOT be decoded as null");
            assertEquals(context.getParams()[0], testStr,
                    "String '" + testStr + "' should be preserved exactly");
        }
    }

    // ========== Test Service Interface ==========

    public interface TestService {
        String echo(String s);
        void process(String a, String b, String c);
    }
}
