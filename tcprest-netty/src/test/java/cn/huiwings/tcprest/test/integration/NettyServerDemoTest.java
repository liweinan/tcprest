package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Integration test for Netty server with raw types (List, HashMap with Serializable elements).
 * Formerly {@code cn.huiwings.tcprest.example.NettyServerDemo} in main; moved to test.
 *
 * @author Weinan Li
 */
public class NettyServerDemoTest {

    public interface RawType {
        List getArrayList(List in);
        HashMap<String, List<Color>> getComplexType(HashMap<String, List<Color>> in);
    }

    public static class RawTypeResource implements RawType {
        @Override
        public List getArrayList(List in) {
            return in;
        }

        @Override
        public HashMap<String, List<Color>> getComplexType(HashMap<String, List<Color>> in) {
            return in;
        }
    }

    public static class Color implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public Color() {}

        public Color(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private TcpRestServer server;
    private int port;
    private RawType client;

    @BeforeClass
    public void setup() throws Exception {
        port = PortGenerator.get();
        server = new NettyTcpRestServer(port);
        server.addResource(RawTypeResource.class);
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            RawType.class, "localhost", port
        );
        client = factory.getClient();
    }

    @AfterClass
    public void teardown() throws Exception {
        if (server != null) {
            server.down();
            Thread.sleep(300);
        }
    }

    @Test
    public void testGetArrayList() {
        List request = new ArrayList();
        request.add(42);
        request.add(new Color("Red"));
        List result = client.getArrayList(request);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertEquals(result.get(0), 42);
        assertTrue(result.get(1) instanceof Color);
        assertEquals(((Color) result.get(1)).getName(), "Red");
    }

    @Test
    public void testGetComplexType() {
        List<Color> list = new ArrayList<>();
        list.add(new Color("Red"));
        HashMap<String, List<Color>> request = new HashMap<>();
        request.put("item", list);

        HashMap<String, List<Color>> result = client.getComplexType(request);
        assertNotNull(result);
        assertTrue(result.containsKey("item"));
        List<Color> resultList = result.get("item");
        assertNotNull(resultList);
        assertEquals(resultList.size(), 1);
        assertEquals(resultList.get(0).getName(), "Red");
    }
}
