package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;

import static org.testng.Assert.*;

/**
 * End-to-end tests for array handling over Netty: client → Netty server → response.
 *
 * <p>Covers Protocol V2 array encode/decode over the wire:</p>
 * <ul>
 *   <li>Primitive arrays (int[], etc.) and String[]</li>
 *   <li>Object arrays (Serializable[]) with RawTypeMapper</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 2026-02-19
 */
public class NettyArrayE2ETest {

    private TcpRestServer server;
    private int port;
    private ArrayE2EService client;

    public static class PersonDto implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int age;

        public PersonDto() {}

        public PersonDto(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PersonDto)) return false;
            PersonDto that = (PersonDto) o;
            return age == that.age && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    public interface ArrayE2EService {
        int[] echoIntArray(int[] arr);
        String[] echoStringArray(String[] arr);
        PersonDto[] echoPersonArray(PersonDto[] arr);
    }

    public static class ArrayE2EServiceImpl implements ArrayE2EService {
        @Override
        public int[] echoIntArray(int[] arr) {
            return arr == null ? null : arr.clone();
        }

        @Override
        public String[] echoStringArray(String[] arr) {
            return arr == null ? null : arr.clone();
        }

        @Override
        public PersonDto[] echoPersonArray(PersonDto[] arr) {
            return arr == null ? null : arr.clone();
        }
    }

    @BeforeClass
    public void setup() throws Exception {
        port = PortGenerator.get();
        server = new NettyTcpRestServer(port);
        server.addSingletonResource(new ArrayE2EServiceImpl());
        server.up();
        Thread.sleep(500);

        TcpRestClientFactory factory = new TcpRestClientFactory(
            ArrayE2EService.class, "localhost", port
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
    public void testIntArrayE2E() {
        int[] input = {1, 2, 3, 10, 20};
        int[] result = client.echoIntArray(input);
        assertNotNull(result);
        assertEquals(result.length, input.length);
        assertTrue(Arrays.equals(result, input));
    }

    @Test
    public void testIntArrayEmptyE2E() {
        int[] input = {};
        int[] result = client.echoIntArray(input);
        assertNotNull(result);
        assertEquals(result.length, 0);
    }

    @Test
    public void testStringArrayE2E() {
        String[] input = {"a", "b", "hello"};
        String[] result = client.echoStringArray(input);
        assertNotNull(result);
        assertEquals(result.length, input.length);
        assertEquals(result[0], "a");
        assertEquals(result[1], "b");
        assertEquals(result[2], "hello");
    }

    @Test
    public void testObjectArrayE2E() {
        PersonDto[] input = {
            new PersonDto("Alice", 25),
            new PersonDto("Bob", 30)
        };
        PersonDto[] result = client.echoPersonArray(input);
        assertNotNull(result);
        assertEquals(result.length, 2);
        assertEquals(result[0], input[0]);
        assertEquals(result[1], input[1]);
        assertEquals(result[0].getName(), "Alice");
        assertEquals(result[0].getAge(), 25);
        assertEquals(result[1].getName(), "Bob");
        assertEquals(result[1].getAge(), 30);
    }
}
