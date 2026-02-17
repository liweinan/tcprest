package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.conveter.DefaultConverter;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class DefaultConverterSmokeTest {

    @Test
    public void test() throws NoSuchMethodException, MapperNotFoundException {
        Converter converter = new DefaultConverter();
        String request = converter.encode(HelloWorldResource.class,
                HelloWorldResource.class.getMethod("oneTwoThree", String.class, int.class, boolean.class),
                new Object[]{"One", 2, true}, MapperHelper.DEFAULT_MAPPERS);

        // Debug request: request
        assertEquals("cn.huiwings.tcprest.test.HelloWorldResource/oneTwoThree(" + converter.encodeParam("One") + TcpRestProtocol.PATH_SEPERATOR + converter.encodeParam("2") + TcpRestProtocol.PATH_SEPERATOR + converter.encodeParam("true") + ")", request);

    }


}
