package io.tcprest.test.sandbox;

import org.testng.annotations.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class MethodInvocationTest {

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

    @Test(enabled=false)
    public void testMethodInvocation() throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        {
            Object obj = ABC.class.newInstance();
            Method mtd = obj.getClass().getMethod("method1", String.class);
            assertNotNull(mtd);
            String response = (String) mtd.invoke(obj, "Hello, world!");
            assertEquals("Hello, world!", response);
        }

        {
            Object obj = ABC.class.newInstance();
            for (Method mtd : obj.getClass().getMethods()) {
                System.out.print(mtd.getName() + ": ");
                for (Class clazz : mtd.getParameterTypes()) {
                    System.out.print(clazz.getCanonicalName() + " ");
                }
                System.out.println("");
            }
        }
    }



}
