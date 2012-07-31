package net.bluedash.tcprest.test;

import net.bluedash.tcprest.client.TcpRestClientFactory;
import net.bluedash.tcprest.mapper.Mapper;
import net.bluedash.tcprest.server.SingleThreadTcpRestServer;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class MapperSmokeTest extends TcpClientFactorySmokeTest {

    @Test
    public void extendMapperTest() {
        tcpRestServer.addResource(HelloWorldResource.class);
        tcpRestServer.addMapper(Color.class.getCanonicalName(), new ColorMapper());

        Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
        colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());
        TcpRestClientFactory factory =
                new TcpRestClientFactory(HelloWorld.class, "localhost",
                        ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort(), colorMapper);

        HelloWorld client = (HelloWorld) factory.getInstance();
        Color color = new Color("Red");
        assertEquals("My favorite color is: Red", client.favoriteColor(color));

    }
}
