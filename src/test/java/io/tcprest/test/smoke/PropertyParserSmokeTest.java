package io.tcprest.test.smoke;

import io.tcprest.commons.PropertyParser;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class PropertyParserSmokeTest {

    @Test
    public void smokeTest() {
        String filePath = "classpath:abc";
        assertTrue(PropertyParser.isRelativeFilePath(filePath));
        assertEquals("abc", PropertyParser.extractFilePath(filePath));
    }
}
