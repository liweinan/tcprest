package io.tcprest.test.sandbox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Weinan Li
 * @created_at 08 20 2012
 */
public class CollectionTest {

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
}
