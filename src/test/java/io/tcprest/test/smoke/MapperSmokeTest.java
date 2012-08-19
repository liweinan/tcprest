package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.mapper.RawTypeMapper;
import io.tcprest.mapper.Mapper;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.test.Color;
import io.tcprest.test.ColorMapper;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Test
    public void testArrayListMapper() {
        List l = new ArrayList();
        l.add(1);
        l.add(2);
        l.add(3);
        RawTypeMapper mapper = new RawTypeMapper();
        System.out.println(mapper.objectToString(l));
    }

    public interface RawType {
        public List getArrayList();
    }

    public class RawTypeResource implements RawType {
        public List getArrayList() {
            List lst = new ArrayList();
            lst.add(42);
            lst.add(new Color("Red"));

            return lst;
        }
    }

    @Test
    public void rawTypeTest() {
        tcpRestServer.addSingletonResource(new RawTypeResource());
        tcpRestServer.addMapper(Color.class.getCanonicalName(), new ColorMapper());
        Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
        colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());

        TcpRestClientFactory factory =
                new TcpRestClientFactory(RawType.class, "localhost",
                        ((SingleThreadTcpRestServer) tcpRestServer).getServerSocket().getLocalPort(), colorMapper);

        RawType client = (RawType) factory.getInstance();
        List resp = client.getArrayList();

        assertEquals(42, resp.get(0));
        Color c = new Color("Red");
        assertEquals(c.getName(), ((Color) resp.get(1)).getName());
    }
}
