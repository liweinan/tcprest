package io.tcprest.test.sandbox;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class ClassPathTest {

    @Test
    @Ignore
    public void testClasspath() {
        System.out.println(System.getProperty("java.class.path"));
    }
}
