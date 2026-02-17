package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.commons.PropertyParser;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
