package io.tcprest.test.sandbox;

import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Weinan Li
 * @created_at 08 20 2012
 */
public class CollectionTest {

    @Ignore
    @Test
    public void smokeTests() {

        List objs = new ArrayList();
        objs.add("a");
        objs.add(1);
        objs.add(true);

        System.out.println(objs.getClass().getCanonicalName());

        for (Object obj : objs) {
            System.out.println(obj.getClass().getCanonicalName());
        }

        for (Class clazz : objs.getClass().getInterfaces()) {
            System.out.println(clazz.equals(java.io.Serializable.class));
        }


    }

    @Ignore
    @Test
    public void mapTest() {
        for (Class clazz : HashMap.class.getInterfaces()) {
            System.out.println(clazz.getCanonicalName());
        }

        System.out.println(Map.class.getCanonicalName());
        List<?> l = new ArrayList<Object>();
        System.out.println(l.getClass().getCanonicalName());
    }

    @Ignore
    @Test
    public void arrayTest() {
        System.out.println(String[].class.isArray());
    }
}
