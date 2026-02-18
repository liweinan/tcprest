package cn.huiwings.tcprest.test.v2mapper;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import com.google.gson.Gson;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Demonstration of Protocol V2's intelligent Mapper support.
 *
 * <p><b>V2 Mapper Features:</b></p>
 * <ul>
 *   <li><b>Auto Serialization</b>: Serializable objects are automatically handled</li>
 *   <li><b>Custom Mappers</b>: Users can register custom mappers for fine-grained control</li>
 *   <li><b>Transparent RPC</b>: Remote calls feel like local calls</li>
 * </ul>
 *
 * @since 2.0
 */
public class V2MapperDemoTest {

    // Use dedicated port range for V2 mapper tests (19000-19999)
    private static final PortGenerator.PortRange portRange = PortGenerator.from(19000);

    /**
     * Test 1: Auto Serialization - No mapper needed!
     *
     * <p>User class implements Serializable, so V2 automatically handles it.</p>
     */
    @Test
    public void testAutoSerialization() throws Exception {
        int port = portRange.next();
        TcpRestServer server = new SingleThreadTcpRestServer(port);
        server.addResource(UserServiceImpl.class);
        server.up();
        Thread.sleep(500);

        try {
            // Client-side: No mapper configuration needed
            TcpRestClientFactory factory = new TcpRestClientFactory(
                UserService.class, "localhost", port
            );
            // V2 is default since 2.0
            UserService client = (UserService) factory.getInstance();

            // Create a user object
            User user = new User("Alice", 25, "alice@example.com");

            // Remote call - feels like local call!
            User result = client.registerUser(user);

            // Verify
            assertNotNull(result);
            assertEquals(result.getName(), "Alice");
            assertEquals(result.getAge(), 25);
            assertEquals(result.getEmail(), "alice@example.com");

            System.out.println("✅ Auto Serialization: User object transmitted successfully!");
            System.out.println("   Input:  " + user);
            System.out.println("   Output: " + result);
        } finally {
            server.down();
            Thread.sleep(300);
        }
    }

    /**
     * Test 2: Custom Mapper using Gson
     *
     * <p>User provides custom mapper for special serialization logic.</p>
     */
    @Test
    public void testCustomMapperWithGson() throws Exception {
        int port = portRange.next();
        TcpRestServer server = new SingleThreadTcpRestServer(port);

        // Server side: Register custom Gson mapper
        server.addMapper(User.class.getName(), new GsonUserMapper());

        server.addResource(UserServiceImpl.class);
        server.up();
        Thread.sleep(500);

        try {
            // Client-side: Register custom Gson mapper
            Map<String, Mapper> mappers = new HashMap<>();
            mappers.put(User.class.getName(), new GsonUserMapper());

            TcpRestClientFactory factory = new TcpRestClientFactory(
                UserService.class, "localhost", port, mappers
            );
            UserService client = (UserService) factory.getInstance();

            // Remote call with custom serialization
            User user = new User("Bob", 30, "bob@example.com");
            User result = client.registerUser(user);

            assertNotNull(result);
            assertEquals(result.getName(), "Bob");
            assertEquals(result.getAge(), 30);
            assertEquals(result.getEmail(), "bob@example.com");

            System.out.println("✅ Custom Mapper (Gson): JSON serialization working!");
            System.out.println("   Input:  " + user);
            System.out.println("   Output: " + result);
        } finally {
            server.down();
            Thread.sleep(300);
        }
    }

    /**
     * Test 3: Mixed types - String + Serializable object
     *
     * <p>V2 intelligently chooses: custom mapper > auto serialization > built-in</p>
     */
    @Test
    public void testMixedTypes() throws Exception {
        int port = portRange.next();
        TcpRestServer server = new SingleThreadTcpRestServer(port);
        server.addResource(UserServiceImpl.class);
        server.up();
        Thread.sleep(500);

        try {
            TcpRestClientFactory factory = new TcpRestClientFactory(
                UserService.class, "localhost", port
            );
            UserService client = (UserService) factory.getInstance();

            // Test with primitive + Serializable object
            User user = new User("Charlie", 35, "charlie@example.com");
            String greeting = client.greetUser("Hello", user);

            assertEquals(greeting, "Hello, Charlie (age 35)!");

            System.out.println("✅ Mixed Types: String + User object");
            System.out.println("   Result: " + greeting);
        } finally {
            server.down();
            Thread.sleep(300);
        }
    }

    // ========== Test Service Interface ==========

    public interface UserService {
        User registerUser(User user);
        String greetUser(String greeting, User user);
    }

    public static class UserServiceImpl implements UserService {
        @Override
        public User registerUser(User user) {
            // Simulate registration logic
            System.out.println("Server: Registering user " + user.getName());
            return user; // Echo back
        }

        @Override
        public String greetUser(String greeting, User user) {
            return greeting + ", " + user.getName() + " (age " + user.getAge() + ")!";
        }
    }

    // ========== User DTO (Serializable) ==========

    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;

        private String name;
        private int age;
        private String email;

        public User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getEmail() { return email; }

        @Override
        public String toString() {
            return "User{name='" + name + "', age=" + age + ", email='" + email + "'}";
        }
    }

    // ========== Custom Mapper using Gson ==========

    /**
     * Custom mapper that uses Gson for JSON serialization.
     *
     * <p>This demonstrates how users can integrate their favorite JSON library.</p>
     */
    public static class GsonUserMapper implements Mapper {
        private final Gson gson = new Gson();

        @Override
        public String objectToString(Object object) {
            return gson.toJson(object);
        }

        @Override
        public Object stringToObject(String param) {
            return gson.fromJson(param, User.class);
        }
    }
}
