package cn.huiwings.tcprest.test.v2mapper;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import com.google.gson.Gson;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Simple test for Gson mapper debugging.
 */
public class SimpleGsonTest {

    private static final PortGenerator.PortRange portRange = PortGenerator.from(19100);

    @Test
    public void testGsonMapper() throws Exception {
        int port = portRange.next();
        TcpRestServer server = new SingleThreadTcpRestServer(port);
        server.setProtocolVersion(ProtocolVersion.V2);

        // Server side: Register Gson mapper
        server.addMapper(Person.class.getName(), new GsonPersonMapper());

        server.addResource(PersonServiceImpl.class);
        server.up();
        Thread.sleep(500);

        try {
            // Client side: Register Gson mapper
            Map<String, Mapper> mappers = new HashMap<>();
            mappers.put(Person.class.getName(), new GsonPersonMapper());

            TcpRestClientFactory factory = new TcpRestClientFactory(
                PersonService.class, "localhost", port, mappers
            );
            PersonService client = (PersonService) factory.getInstance();

            // Test
            Person person = new Person("Alice", 25);
            Person result = client.echo(person);

            assertEquals(result.name, "Alice");
            assertEquals(result.age, 25);

            System.out.println("✅ Gson mapper working: " + result);
        } finally {
            server.down();
            Thread.sleep(300);
        }
    }

    // Simple POJO (NOT Serializable - force using Gson)
    public static class Person {
        public String name;
        public int age;

        public Person() {}

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + '}';
        }
    }

    public interface PersonService {
        Person echo(Person person);
    }

    public static class PersonServiceImpl implements PersonService {
        @Override
        public Person echo(Person person) {
            System.out.println("Server received: " + person);
            return person;
        }
    }

    public static class GsonPersonMapper implements Mapper {
        private final Gson gson = new Gson();

        @Override
        public String objectToString(Object object) {
            String json = gson.toJson(object);
            System.out.println("  [Mapper] objectToString: " + object + " → " + json);
            return json;
        }

        @Override
        public Object stringToObject(String param) {
            System.out.println("  [Mapper] stringToObject: " + param);
            Person result = gson.fromJson(param, Person.class);
            System.out.println("  [Mapper] result: " + result);
            return result;
        }
    }
}
