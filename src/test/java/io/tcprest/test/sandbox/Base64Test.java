package io.tcprest.test.sandbox;

import io.tcprest.commons.Base64;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * http://www.mkyong.com/java/how-do-convert-byte-array-to-string-in-java/
 * @author Weinan Li
 * @created_at 08 20 2012
 */
public class Base64Test {

    @Test(enabled=false)
    public void testEncodeAndDecode() {
        String encodedStr = Base64.encode("abc".getBytes());
        String decodedStr = new String(Base64.decode(encodedStr));
        assertEquals("abc", decodedStr);

        System.out.println(Base64.encode("Jack".getBytes()));
        System.out.println(Base64.encode("Lucy".getBytes()));
    }
}
