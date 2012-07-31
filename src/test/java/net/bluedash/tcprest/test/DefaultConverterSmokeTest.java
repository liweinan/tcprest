package net.bluedash.tcprest.test;

import net.bluedash.tcprest.conveter.Converter;
import net.bluedash.tcprest.conveter.DefaultConverter;
import net.bluedash.tcprest.exception.MapperNotFoundException;
import net.bluedash.tcprest.mapper.MapperHelper;
import net.bluedash.tcprest.protocol.DefaultTcpRestProtocol;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class DefaultConverterSmokeTest {

    @Test
    public void test() throws NoSuchMethodException, MapperNotFoundException {
        Converter converter = new DefaultConverter();
        String request = converter.convert(HelloWorldResource.class,
                HelloWorldResource.class.getMethod("oneTwoThree", String.class, int.class, boolean.class),
                new Object[]{"One", 2, true}, MapperHelper.DEFAULT_MAPPERS);

        System.out.println(request);
        assertEquals("net.bluedash.tcprest.test.HelloWorldResource/oneTwoThree({{One}}java.lang.String" + DefaultTcpRestProtocol.PATH_SEPERATOR + "{{2}}java.lang.Integer" + DefaultTcpRestProtocol.PATH_SEPERATOR + "{{true}}java.lang.Boolean)", request);

    }


}
