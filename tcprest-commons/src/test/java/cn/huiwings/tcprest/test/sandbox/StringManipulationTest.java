package cn.huiwings.tcprest.test.sandbox;

import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class StringManipulationTest {

    @Test(enabled=false)
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


        str = "{{x}}java.lang.String" + TcpRestProtocol.PATH_SEPERATOR
                + "{{2}}java.lang.Integer"
                + TcpRestProtocol.PATH_SEPERATOR
                + "{{false}}java.lang.Boolean";
        for (String s : str.split(TcpRestProtocol.PATH_SEPERATOR)) {
            System.out.println(s);
        }

    }


}
