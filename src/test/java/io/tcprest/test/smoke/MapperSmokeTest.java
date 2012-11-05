package io.tcprest.test.smoke;

import io.tcprest.client.TcpRestClientFactory;
import io.tcprest.mapper.Mapper;
import io.tcprest.mapper.RawTypeMapper;
import io.tcprest.server.NettyTcpRestServer;
import io.tcprest.server.NioTcpRestServer;
import io.tcprest.server.SingleThreadTcpRestServer;
import io.tcprest.server.TcpRestServer;
import io.tcprest.test.Color;
import io.tcprest.test.ColorMapper;
import io.tcprest.test.HelloWorld;
import io.tcprest.test.HelloWorldResource;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class MapperSmokeTest {
    protected TcpRestServer tcpRestServer;

    public MapperSmokeTest(TcpRestServer tcpRestServer) {
        this.tcpRestServer = tcpRestServer;
    }

    @Factory
    public static Object[] create()
            throws Exception {
        List result = new ArrayList();
        result.add(new MapperSmokeTest(new SingleThreadTcpRestServer(PortGenerator.get())));
        result.add(new MapperSmokeTest(new NioTcpRestServer(PortGenerator.get())));
        result.add(new MapperSmokeTest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    @BeforeTest
    public void startTcpRestServer()
            throws Exception {
        tcpRestServer.up();
    }

    @AfterTest
    public void stopTcpRestServer()
            throws Exception {
        tcpRestServer.down();
    }

    @Test
    public void extendMapperTest() {
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");
        tcpRestServer.addResource(HelloWorldResource.class);
        tcpRestServer.addMapper(Color.class.getCanonicalName(),
                new ColorMapper());

        Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
        colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());

        TcpRestClientFactory factory = new TcpRestClientFactory(
                HelloWorld.class, "localhost",
                tcpRestServer.getServerPort(), colorMapper);

        HelloWorld client = (HelloWorld) factory.getInstance();
        Color color = new Color("Red");
        assertEquals("My favorite color is: Red", client.favoriteColor(color));
    }


    @Test
    public void testArrayListMapper() {
        List list = new ArrayList();
        list.add(1);
        list.add(2);
        list.add(3);
        RawTypeMapper mapper = new RawTypeMapper();
        System.out.println(mapper.objectToString(list));
    }

    public interface RawType {
        public List getArrayList(List in);

        public HashMap<String, List<Color>> getComplexType(
                HashMap<String, List<Color>> in);
    }

    public class RawTypeResource implements RawType {
        public List getArrayList(List in) {
            return in;
        }

        public HashMap<String, List<Color>> getComplexType(
                HashMap<String, List<Color>> in) {
            return in;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void rawTypeTest() {
        System.out.println("-----------------------------------" + tcpRestServer.getClass().getCanonicalName() + "--------------------------------");
        // We don't put Color mapper into server,
        // so server will fallback to use RawTypeMapper to decode Color.class
        // because Color is serializable.
        tcpRestServer.addSingletonResource(new RawTypeResource());

        TcpRestClientFactory factory = new TcpRestClientFactory(RawType.class,
                "localhost", tcpRestServer.getServerPort());

        RawType client = (RawType) factory.getInstance();
        Color red = new Color("Red");

        {
            List request = new ArrayList();
            request.add(42);
            request.add(red);

            List response = client.getArrayList(request);

            assertEquals(42, response.get(0));
            assertEquals(red.getName(), ((Color) response.get(1)).getName());
        }

        {
            List<Color> list = new ArrayList<Color>();
            list.add(red);
            HashMap<String, List<Color>> request = new HashMap<String, List<Color>>();
            request.put("item", list);

            HashMap<String, List<Color>> response = client.getComplexType(request);

            assertEquals("item", response.keySet().iterator().next());
            assertEquals(red.getName(), response.get("item").get(0).getName());

        }
    }
}
