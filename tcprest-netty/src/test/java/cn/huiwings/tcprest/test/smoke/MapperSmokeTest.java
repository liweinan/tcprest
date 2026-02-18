package cn.huiwings.tcprest.test.smoke;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.RawTypeMapper;
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
import static org.testng.Assert.assertTrue;

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
        // Test custom mapper with V2 protocol
        // V2 supports intelligent mapper system: custom mappers have highest priority
        tcpRestServer.addResource(HelloWorldResource.class);
        tcpRestServer.addMapper(Color.class.getCanonicalName(),
                new ColorMapper());

        Map<String, Mapper> colorMapper = new HashMap<String, Mapper>();
        colorMapper.put(Color.class.getCanonicalName(), new ColorMapper());

        TcpRestClientFactory factory = new TcpRestClientFactory(
                HelloWorld.class, "localhost",
                tcpRestServer.getServerPort(), colorMapper);
        // V2 is default, but set explicitly for clarity

        HelloWorld client = (HelloWorld) factory.getInstance();
        Color color = new Color("Red");
        assertEquals("My favorite color is: Red", client.favoriteColor(color));
    }


    @Test
    public void autoSerializationTest() {
        // V2 auto-serialization test with Serializable objects
        // We don't register a Color mapper, so V2 will automatically use
        // auto-serialization (RawTypeMapper) because Color implements Serializable.
        // This is V2's Priority 2: Auto-Serialization for Serializable objects
        tcpRestServer.addSingletonResource(new AutoSerializationService());

        TcpRestClientFactory factory = new TcpRestClientFactory(AutoSerializationAPI.class,
                "localhost", tcpRestServer.getServerPort());

        AutoSerializationAPI client = (AutoSerializationAPI) factory.getInstance();

        // Test 1: Simple Serializable object (Color) without custom mapper
        Color red = new Color("Red");
        Color response1 = client.echoColor(red);
        assertEquals("Red", response1.getName());

        // Test 2: Serializable wrapper containing multiple Color objects
        ColorPair pair = new ColorPair(new Color("Red"), new Color("Blue"));
        ColorPair response2 = client.echoColorPair(pair);
        assertEquals("Red", response2.getFirst().getName());
        assertEquals("Blue", response2.getSecond().getName());
    }

    // V2-compatible service interface with proper type signatures
    public interface AutoSerializationAPI {
        Color echoColor(Color color);
        ColorPair echoColorPair(ColorPair pair);
    }

    public static class AutoSerializationService implements AutoSerializationAPI {
        @Override
        public Color echoColor(Color color) {
            return color;
        }

        @Override
        public ColorPair echoColorPair(ColorPair pair) {
            return pair;
        }
    }

    // Serializable wrapper for testing auto-serialization with nested objects
    public static class ColorPair implements java.io.Serializable {
        private Color first;
        private Color second;

        public ColorPair(Color first, Color second) {
            this.first = first;
            this.second = second;
        }

        public Color getFirst() { return first; }
        public Color getSecond() { return second; }
    }

}
