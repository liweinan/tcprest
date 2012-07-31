package io.tcprest.test;

import io.tcprest.conveter.Converter;
import io.tcprest.conveter.DefaultConverter;
import io.tcprest.exception.MapperNotFoundException;
import io.tcprest.mapper.MapperHelper;
import io.tcprest.protocol.DefaultTcpRestProtocol;
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
        assertEquals("io.tcprest.test.HelloWorldResource/oneTwoThree({{One}}java.lang.String" + DefaultTcpRestProtocol.PATH_SEPERATOR + "{{2}}java.lang.Integer" + DefaultTcpRestProtocol.PATH_SEPERATOR + "{{true}}java.lang.Boolean)", request);

    }


}