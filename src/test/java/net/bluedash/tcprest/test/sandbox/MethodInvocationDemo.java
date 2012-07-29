package net.bluedash.tcprest.test.sandbox;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
*
* @author Weinan Li
* CREATED AT: Jul 29 2012
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
