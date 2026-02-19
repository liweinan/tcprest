package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import static org.testng.Assert.*;

/**
 * Client-server integration tests for Protocol V2 array handling and Deque mapper support.
 *
 * <p>Covers:</p>
 * <ul>
 *   <li><b>Arrays:</b> Primitive arrays (int[], etc.) and String[] over the wire.</li>
 *   <li><b>Deque:</b> MapperHelper.DEFAULT_MAPPERS includes Deque with RawTypeMapper.</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 2026-02-19
 */
public class ArrayAndDequeIntegrationTest {

    private TcpRestServer server;
    private int port;
    private ArrayAndDequeService client;

    public interface ArrayAndDequeService {
        int[] echoIntArray(int[] arr);
        String[] echoStringArray(String[] arr);
        Deque<String> echoDeque(Deque<String> deque);
    }

    public static class ArrayAndDequeServiceImpl implements ArrayAndDequeService {
        @Override
        public int[] echoIntArray(int[] arr) {
            return arr == null ? null : arr.clone();
        }

        @Override
        public String[] echoStringArray(String[] arr) {
            return arr == null ? null : arr.clone();
        }

        @Override
        public Deque<String> echoDeque(Deque<String> deque) {
            if (deque == null) return null;
            return new LinkedList<>(deque);
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        port = PortGenerator.get();
        server = new NettyTcpRestServer(port);
        server.addSingletonResource(new ArrayAndDequeServiceImpl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            ArrayAndDequeService.class, "localhost", port
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
    public void testPrimitiveIntArrayRoundTrip() {
        int[] input = {1, 2, 3, 10, 20};
        int[] result = client.echoIntArray(input);
        assertNotNull(result);
        assertEquals(result.length, input.length);
        assertTrue(Arrays.equals(result, input));
    }

    @Test
    public void testPrimitiveIntArrayEmpty() {
        int[] input = {};
        int[] result = client.echoIntArray(input);
        assertNotNull(result);
        assertEquals(result.length, 0);
    }

    @Test
    public void testStringArrayRoundTrip() {
        String[] input = {"a", "b", "hello"};
        String[] result = client.echoStringArray(input);
        assertNotNull(result);
        assertEquals(result.length, input.length);
        assertEquals(result[0], "a");
        assertEquals(result[1], "b");
        assertEquals(result[2], "hello");
    }

    @Test
    public void testDequeRoundTrip() {
        Deque<String> input = new ArrayDeque<>();
        input.addLast("first");
        input.addLast("second");
        input.addLast("third");

        Deque<String> result = client.echoDeque(input);
        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.pollFirst(), "first");
        assertEquals(result.pollFirst(), "second");
        assertEquals(result.pollFirst(), "third");
    }

    @Test
    public void testDequeLinkedListRoundTrip() {
        Deque<String> input = new LinkedList<>();
        input.add("x");
        input.add("y");

        Deque<String> result = client.echoDeque(input);
        assertNotNull(result);
        assertEquals(result.size(), 2);
        assertTrue(result.contains("x"));
        assertTrue(result.contains("y"));
    }
}
