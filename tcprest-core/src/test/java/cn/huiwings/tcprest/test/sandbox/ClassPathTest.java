package cn.huiwings.tcprest.test.sandbox;

import org.testng.annotations.Test;

/**
 * @author Weinan Li
 * @created_at 08 25 2012
 */
public class ClassPathTest {

    @Test(enabled=false)
    public void testClasspath() {
        System.out.println(System.getProperty("java.class.path"));
    }
}
