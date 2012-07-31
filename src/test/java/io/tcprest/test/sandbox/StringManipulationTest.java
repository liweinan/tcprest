package io.tcprest.test.sandbox;

import io.tcprest.protocol.DefaultTcpRestProtocol;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class StringManipulationTest {

    @Test
    public void testIndexOf() {
        String str = "abc";
        int idx = str.indexOf('/');
        System.out.println(idx);
        assertEquals(-1, idx);

        str = "abc/def";
        idx = str.indexOf('/');
        System.out.println(idx);
        assertEquals(3, idx);

        str = "{{Jack!}}java.lang.String";
        System.out.println(str.indexOf("{{"));
        assertEquals(0, str.indexOf("{{"));
        System.out.println(str.indexOf("}}"));
        assertEquals(7, str.indexOf("}}"));
        System.out.println(str.substring(str.indexOf("{{") + 2, str.indexOf("}}")));
        assertEquals("Jack!", str.substring(str.indexOf("{{") + 2, str.indexOf("}}")));
        System.out.println(str.substring(str.indexOf("}}") + 2, str.length()));
        assertEquals("java.lang.String", str.substring(str.indexOf("}}") + 2, str.length()));


        str = "{{x}}java.lang.String" + DefaultTcpRestProtocol.PATH_SEPERATOR
                + "{{2}}java.lang.Integer"
                + DefaultTcpRestProtocol.PATH_SEPERATOR
                + "{{false}}java.lang.Boolean";
        for (String s : str.split(DefaultTcpRestProtocol.PATH_SEPERATOR)) {
            System.out.println(s);
        }

    }


}