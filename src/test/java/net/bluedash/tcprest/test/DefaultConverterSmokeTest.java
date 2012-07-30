package net.bluedash.tcprest.test;

import net.bluedash.tcprest.conveter.Converter;
import net.bluedash.tcprest.conveter.DefaultConverter;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class DefaultConverterSmokeTest {

    @Test
    public void test() throws NoSuchMethodException {
        Converter converter = new DefaultConverter();
        String request = converter.convert(HelloWorldRestlet.class,
                HelloWorldRestlet.class.getMethod("oneTwoThree", String.class, int.class, boolean.class),
                new Object[]{"One", 2, true});

        System.out.println(request);
        assertEquals("net.bluedash.tcprest.test.HelloWorldRestlet/oneTwoThree({{One}}java.lang.String,{{2}}java.lang.Integer,{{true}}java.lang.Boolean)",request);

    }


}
