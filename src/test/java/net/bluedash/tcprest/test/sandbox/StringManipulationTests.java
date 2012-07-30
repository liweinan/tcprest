package net.bluedash.tcprest.test.sandbox;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date Jul 30 2012
 */
public class StringManipulationTests {

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
    }


}
