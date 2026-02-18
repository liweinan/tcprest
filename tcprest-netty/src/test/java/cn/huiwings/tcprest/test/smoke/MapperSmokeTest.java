package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.RawTypeMapper;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.Color;
import cn.huiwings.tcprest.test.ColorMapper;
import cn.huiwings.tcprest.test.HelloWorld;
import cn.huiwings.tcprest.test.HelloWorldResource;
import org.testng.annotations.*;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;

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
        result.add(new MapperSmokeTest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    @BeforeClass
    public void startTcpRestServer()
            throws Exception {
        tcpRestServer.up();
        // Delay to ensure async servers (NioTcpRestServer, NettyTcpRestServer) are fully started
        Thread.sleep(500);
    }

    @AfterClass
    public void stopTcpRestServer()
            throws Exception {
        tcpRestServer.down();
        // Wait for port to be fully released
        Thread.sleep(300);
    }

    @Test
    public void extendMapperTest() {
        // Test with: tcpRestServer.getClass().getCanonicalName()
        // Note: Mapper is a V1 feature, so we explicitly set server to V1
        tcpRestServer.setProtocolVersion(ProtocolVersion.V1);
        tcpRestServer.addResource(HelloWorldResource.class);
        tcpRestServer.addMapper(Color.class.getCanonicalName(),
                new ColorMapper());

        Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
        colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());

        TcpRestClientFactory factory = new TcpRestClientFactory(
                HelloWorld.class, "localhost",
                tcpRestServer.getServerPort(), colorMapper);
        // Mapper requires V1 protocol
        factory.getProtocolConfig().setVersion(ProtocolVersion.V1);

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
        // Debug: mapper.objectToString(list)
    }

    public interface RawType {
        public List getArrayList(List in);

        public HashMap<String, List<Color>> getComplexType(
                HashMap<String, List<Color>> in);
    }

    public static class RawTypeResource implements RawType {
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
        // Test with: tcpRestServer.getClass().getCanonicalName()
        // We don't put Color mapper into server,
        // so server will fallback to use RawTypeMapper to decode Color.class
        // because Color is serializable.
        // Note: RawTypeMapper is a V1 feature, so we explicitly set server to V1
        tcpRestServer.setProtocolVersion(ProtocolVersion.V1);
        tcpRestServer.addSingletonResource(new RawTypeResource());

        TcpRestClientFactory factory = new TcpRestClientFactory(RawType.class,
                "localhost", tcpRestServer.getServerPort());
        // RawTypeMapper requires V1 protocol
        factory.getProtocolConfig().setVersion(ProtocolVersion.V1);

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
