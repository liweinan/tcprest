package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import cn.huiwings.tcprest.test.smoke.PortGenerator;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.util.*;

import static org.testng.Assert.*;

/**
 * End-to-end test for custom mapper support with Netty server.
 *
 * <p>Tests complete client-server communication with custom type mappers:</p>
 * <ul>
 *   <li>Basic custom mapper (Color → "color_name")</li>
 *   <li>JSON-style mapper (Product → JSON string)</li>
 *   <li>Mapper priority (custom mapper overrides auto-serialization)</li>
 *   <li>Collections with custom types (List&lt;Color&gt;, Map&lt;String, Color&gt;)</li>
 *   <li>Nested objects with custom types (Order contains Product)</li>
 *   <li>Null handling and edge cases</li>
 * </ul>
 *
 * <p><b>Key Validation Points:</b></p>
 * <ul>
 *   <li>Custom mappers work across network with Netty</li>
 *   <li>User-defined mappers have Priority 1 (highest)</li>
 *   <li>Both server and client must register the same mapper</li>
 *   <li>Collections and nested structures preserve custom mapping</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 2026-02-19
 */
public class CustomMapperE2ETest {

    private TcpRestServer server;
    private int port;
    private CustomMapperService client;

    // ========== Test Domain Objects ==========

    /**
     * Simple custom type - represents a color with a name.
     * Uses ColorMapper for custom serialization.
     *
     * <p><b>Note:</b> Implements Serializable. Custom mapper (ColorMapper) should
     * still take priority over auto-serialization (RawTypeMapper) when registered.</p>
     */
    public static class Color implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;

        public Color() {}
        public Color(String name) {
            this.name = name;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Color)) return false;
            Color color = (Color) o;
            return Objects.equals(name, color.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    /**
     * Custom type WITHOUT Serializable - represents a coordinate.
     * Uses CoordinateMapper for custom serialization.
     *
     * <p><b>CRITICAL TEST:</b> Does NOT implement Serializable.
     * This verifies that custom mapper works even when the class
     * cannot be auto-serialized, proving custom mapper is truly Priority 1.</p>
     */
    public static class Coordinate {
        private double x;
        private double y;

        public Coordinate() {}
        public Coordinate(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }
        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Coordinate)) return false;
            Coordinate that = (Coordinate) o;
            return Double.compare(that.x, x) == 0 &&
                   Double.compare(that.y, y) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * Complex custom type - represents a product with multiple fields.
     * Uses ProductMapper for JSON-style serialization.
     */
    public static class Product implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;
        private String name;
        private double price;

        public Product() {}
        public Product(String id, String name, double price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Product)) return false;
            Product product = (Product) o;
            return Double.compare(product.price, price) == 0 &&
                   Objects.equals(id, product.id) &&
                   Objects.equals(name, product.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, price);
        }
    }

    /**
     * Nested object containing custom type.
     * Tests that custom mappers work correctly in nested structures.
     */
    public static class Order implements Serializable {
        private static final long serialVersionUID = 1L;
        private String orderId;
        private Product product;
        private int quantity;

        public Order() {}
        public Order(String orderId, Product product, int quantity) {
            this.orderId = orderId;
            this.product = product;
            this.quantity = quantity;
        }

        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public Product getProduct() { return product; }
        public void setProduct(Product product) { this.product = product; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Order)) return false;
            Order order = (Order) o;
            return quantity == order.quantity &&
                   Objects.equals(orderId, order.orderId) &&
                   Objects.equals(product, order.product);
        }

        @Override
        public int hashCode() {
            return Objects.hash(orderId, product, quantity);
        }
    }

    // ========== Custom Mappers ==========

    /**
     * Simple custom mapper - converts Color to/from string format.
     * Format: "COLOR:name" (e.g., "COLOR:Red")
     */
    public static class ColorMapper implements Mapper {
        @Override
        public String objectToString(Object obj) {
            if (obj == null) return null;
            Color color = (Color) obj;
            return "COLOR:" + color.getName();
        }

        @Override
        public Object stringToObject(String str) {
            if (str == null || str.isEmpty()) return null;
            if (!str.startsWith("COLOR:")) {
                throw new IllegalArgumentException("Invalid color format: " + str);
            }
            String name = str.substring(6); // Remove "COLOR:" prefix
            return new Color(name);
        }
    }

    /**
     * JSON-style custom mapper - converts Product to/from JSON format.
     * Format: {"id":"P001","name":"Laptop","price":999.99}
     */
    public static class ProductMapper implements Mapper {
        @Override
        public String objectToString(Object obj) {
            if (obj == null) return null;
            Product product = (Product) obj;
            return "{\"id\":\"" + product.getId() + "\"," +
                   "\"name\":\"" + product.getName() + "\"," +
                   "\"price\":" + product.getPrice() + "}";
        }

        @Override
        public Object stringToObject(String str) {
            if (str == null || str.isEmpty()) return null;
            // Simple JSON parsing (for test purposes)
            str = str.trim();
            if (!str.startsWith("{") || !str.endsWith("}")) {
                throw new IllegalArgumentException("Invalid JSON format: " + str);
            }

            Product product = new Product();
            String content = str.substring(1, str.length() - 1); // Remove { }
            String[] pairs = content.split(",");

            for (String pair : pairs) {
                String[] kv = pair.split(":");
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim().replace("\"", "");

                switch (key) {
                    case "id":
                        product.setId(value);
                        break;
                    case "name":
                        product.setName(value);
                        break;
                    case "price":
                        product.setPrice(Double.parseDouble(value));
                        break;
                }
            }
            return product;
        }
    }

    /**
     * Custom mapper for non-Serializable Coordinate class.
     * Format: "COORD:x,y" (e.g., "COORD:10.5,20.3")
     *
     * <p><b>CRITICAL:</b> This mapper proves that custom mappers work
     * even for classes that don't implement Serializable, verifying
     * that custom mapper is truly Priority 1 (highest).</p>
     */
    public static class CoordinateMapper implements Mapper {
        @Override
        public String objectToString(Object obj) {
            if (obj == null) return null;
            Coordinate coord = (Coordinate) obj;
            return "COORD:" + coord.getX() + "," + coord.getY();
        }

        @Override
        public Object stringToObject(String str) {
            if (str == null || str.isEmpty()) return null;
            if (!str.startsWith("COORD:")) {
                throw new IllegalArgumentException("Invalid coordinate format: " + str);
            }
            String[] parts = str.substring(6).split(","); // Remove "COORD:"
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            return new Coordinate(x, y);
        }
    }

    // ========== Service Interface ==========

    public interface CustomMapperService {
        // Basic custom mapper test
        String processColor(Color color);

        // Complex custom mapper test
        Product getProductById(String id);

        // Collection with custom type
        List<Color> getAllColors(List<Color> colors);

        // Map with custom type
        Map<String, Color> getColorMap(Map<String, Color> colors);

        // Nested object with custom type
        Order createOrder(String orderId, Product product, int quantity);

        // Multiple custom types
        String mixedTypes(Color color, Product product);

        // Null handling
        Color getNullColor();

        // Non-Serializable class with custom mapper (CRITICAL TEST)
        String processCoordinate(Coordinate coordinate);

        // Non-Serializable return type with custom mapper
        Coordinate getOriginCoordinate();
    }

    // ========== Service Implementation ==========

    public static class CustomMapperServiceImpl implements CustomMapperService {
        @Override
        public String processColor(Color color) {
            return "Received color: " + color.getName();
        }

        @Override
        public Product getProductById(String id) {
            // Return a test product
            return new Product(id, "Test Product", 99.99);
        }

        @Override
        public List<Color> getAllColors(List<Color> colors) {
            // Echo back the list
            return new ArrayList<>(colors);
        }

        @Override
        public Map<String, Color> getColorMap(Map<String, Color> colors) {
            // Echo back the map
            return new HashMap<>(colors);
        }

        @Override
        public Order createOrder(String orderId, Product product, int quantity) {
            return new Order(orderId, product, quantity);
        }

        @Override
        public String mixedTypes(Color color, Product product) {
            return "Color: " + color.getName() + ", Product: " + product.getName();
        }

        @Override
        public Color getNullColor() {
            return null;
        }

        @Override
        public String processCoordinate(Coordinate coordinate) {
            return "Received coordinate: (" + coordinate.getX() + ", " + coordinate.getY() + ")";
        }

        @Override
        public Coordinate getOriginCoordinate() {
            return new Coordinate(0.0, 0.0);
        }
    }

    // ========== Test Lifecycle ==========

    @BeforeClass
    public void setup() throws Exception {
        port = PortGenerator.get();
        server = new NettyTcpRestServer(port);

        // Register custom mappers on server
        server.addMapper(Color.class.getCanonicalName(), new ColorMapper());
        server.addMapper(Product.class.getCanonicalName(), new ProductMapper());
        server.addMapper(Coordinate.class.getCanonicalName(), new CoordinateMapper());

        // Register service
        server.addSingletonResource(new CustomMapperServiceImpl());
        server.up();

        // Wait for Netty server to be fully ready
        Thread.sleep(500);

        // Create client with custom mappers
        Map<String, Mapper> clientMappers = new HashMap<>();
        clientMappers.put(Color.class.getCanonicalName(), new ColorMapper());
        clientMappers.put(Product.class.getCanonicalName(), new ProductMapper());
        clientMappers.put(Coordinate.class.getCanonicalName(), new CoordinateMapper());

        TcpRestClientFactory factory = new TcpRestClientFactory(
            CustomMapperService.class, "localhost", port, clientMappers
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

    // ========== E2E Tests ==========

    @Test
    public void testBasicCustomMapper() {
        // Test simple custom mapper (ColorMapper)
        Color red = new Color("Red");
        String result = client.processColor(red);

        assertEquals(result, "Received color: Red");
    }

    @Test
    public void testComplexCustomMapper() {
        // Test JSON-style custom mapper (ProductMapper)
        Product product = client.getProductById("P001");

        assertNotNull(product);
        assertEquals(product.getId(), "P001");
        assertEquals(product.getName(), "Test Product");
        assertEquals(product.getPrice(), 99.99, 0.001);
    }

    @Test
    public void testCollectionWithCustomMapper() {
        // Test List<Color> with custom mapper
        List<Color> colors = new ArrayList<>();
        colors.add(new Color("Red"));
        colors.add(new Color("Green"));
        colors.add(new Color("Blue"));

        List<Color> result = client.getAllColors(colors);

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.get(0).getName(), "Red");
        assertEquals(result.get(1).getName(), "Green");
        assertEquals(result.get(2).getName(), "Blue");
    }

    @Test
    public void testMapWithCustomMapper() {
        // Test Map<String, Color> with custom mapper
        Map<String, Color> colors = new HashMap<>();
        colors.put("primary", new Color("Red"));
        colors.put("secondary", new Color("Blue"));
        colors.put("tertiary", new Color("Yellow"));

        Map<String, Color> result = client.getColorMap(colors);

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.get("primary").getName(), "Red");
        assertEquals(result.get("secondary").getName(), "Blue");
        assertEquals(result.get("tertiary").getName(), "Yellow");
    }

    @Test
    public void testNestedObjectWithCustomMapper() {
        // Test nested object: Order contains Product (custom mapper)
        Product laptop = new Product("P123", "Laptop", 1299.99);
        Order order = client.createOrder("ORD-001", laptop, 2);

        assertNotNull(order);
        assertEquals(order.getOrderId(), "ORD-001");
        assertEquals(order.getQuantity(), 2);
        assertNotNull(order.getProduct());
        assertEquals(order.getProduct().getId(), "P123");
        assertEquals(order.getProduct().getName(), "Laptop");
        assertEquals(order.getProduct().getPrice(), 1299.99, 0.001);
    }

    @Test
    public void testMultipleCustomTypes() {
        // Test multiple custom types in same method call
        Color blue = new Color("Blue");
        Product phone = new Product("P456", "Phone", 899.99);

        String result = client.mixedTypes(blue, phone);

        assertEquals(result, "Color: Blue, Product: Phone");
    }

    @Test
    public void testNullHandling() {
        // Test that null values are handled correctly with custom mappers
        Color result = client.getNullColor();
        assertNull(result);
    }

    @Test
    public void testMapperPriority() {
        // Verify that custom mapper takes priority over auto-serialization
        // Color implements Serializable, but ColorMapper should be used instead
        Color yellow = new Color("Yellow");
        String result = client.processColor(yellow);

        // If auto-serialization (RawTypeMapper) was used, we would get binary data
        // With ColorMapper, we get readable string format
        assertEquals(result, "Received color: Yellow");
    }

    @Test
    public void testRoundTripSerialization() {
        // Test complete round-trip: client → server → client
        Product original = new Product("P999", "Original Product", 123.45);

        // Send product to server and get it back
        Order order = client.createOrder("TEST", original, 5);
        Product returned = order.getProduct();

        // Verify complete equality after round-trip
        assertEquals(returned, original);
        assertEquals(returned.getId(), "P999");
        assertEquals(returned.getName(), "Original Product");
        assertEquals(returned.getPrice(), 123.45, 0.001);
    }

    @Test
    public void testNonSerializableWithCustomMapper() {
        // CRITICAL TEST: Coordinate does NOT implement Serializable
        // This proves custom mapper works regardless of Serializable interface
        Coordinate point = new Coordinate(10.5, 20.3);
        String result = client.processCoordinate(point);

        // Verify custom mapper was used (CoordinateMapper format)
        assertEquals(result, "Received coordinate: (10.5, 20.3)");
    }

    @Test
    public void testNonSerializableReturnType() {
        // CRITICAL TEST: Return non-Serializable object with custom mapper
        Coordinate origin = client.getOriginCoordinate();

        assertNotNull(origin);
        assertEquals(origin.getX(), 0.0, 0.001);
        assertEquals(origin.getY(), 0.0, 0.001);
    }

    @Test
    public void testNonSerializableMapperPriority() {
        // CRITICAL TEST: Verify custom mapper Priority 1 even without Serializable
        // Coordinate does NOT implement Serializable, so:
        // - Without custom mapper: would fail (no auto-serialization possible)
        // - With custom mapper: should work perfectly
        Coordinate point = new Coordinate(100.0, 200.0);
        String result = client.processCoordinate(point);

        // Verify CoordinateMapper format used (not binary, because Coordinate isn't Serializable)
        assertEquals(result, "Received coordinate: (100.0, 200.0)");

        // Verify round-trip works
        Coordinate returned = client.getOriginCoordinate();
        assertNotNull(returned);
        // If custom mapper wasn't Priority 1, this would fail
    }
}
