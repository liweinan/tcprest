package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import cn.huiwings.tcprest.codec.DefaultProtocolCodec;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.mapper.MapperHelper;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class DefaultConverterSmokeTest {

    @Test
    public void test() throws NoSuchMethodException, MapperNotFoundException {
        ProtocolCodec codec = new DefaultProtocolCodec();
        String request = codec.encode(HelloWorldResource.class,
                HelloWorldResource.class.getMethod("oneTwoThree", String.class, int.class, boolean.class),
                new Object[]{"One", 2, true}, MapperHelper.DEFAULT_MAPPERS);

        // Verify new secure format: 0|{{base64(meta)}}|{{base64(params)}}
        assertTrue(request.startsWith("0|"), "Request should start with compression flag");

        // Split and verify components
        String[] components = request.split("\\|", -1);
        assertEquals(3, components.length, "Should have 3 components: compression|meta|params");

        // Decode and verify metadata
        String metaDecoded = ProtocolSecurity.decodeComponent(components[1]);
        assertEquals("cn.huiwings.tcprest.test.HelloWorldResource/oneTwoThree", metaDecoded);

        // Decode and verify parameters
        String paramsDecoded = ProtocolSecurity.decodeComponent(components[2]);
        String expectedParams = codec.encodeParam("One")
                + TcpRestProtocol.PARAM_SEPARATOR
                + codec.encodeParam("2")
                + TcpRestProtocol.PARAM_SEPARATOR
                + codec.encodeParam("true");
        assertEquals(expectedParams, paramsDecoded);

    }


}
