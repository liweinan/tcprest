package net.bluedash.tcprest.test.sandbox;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by IntelliJ IDEA.
 * User: weli
 * Date: 7/29/12
 * Time: 4:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class MethodInvocationDemo {

    // ABC must be declared as 'static'
    // or it couldn't be instantiated
    // by ABC.class.newInstance();
    public static class ABC {
        public ABC() {
        }

        public String method1(String arg) {
            return arg;
        }
    }

    @Test
    public void testMethodInvocation() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Object obj = ABC.class.newInstance();
        Method mtd = obj.getClass().getMethod("method1", String.class);
        assertNotNull(mtd);
        String response = (String) mtd.invoke(obj, "Hello, world!");
        assertEquals("Hello, world!", response);
    }
}
