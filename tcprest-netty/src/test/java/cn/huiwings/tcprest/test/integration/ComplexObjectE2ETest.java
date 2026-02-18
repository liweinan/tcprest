package cn.huiwings.tcprest.test.integration;

import cn.huiwings.tcprest.client.TcpRestClientFactory;
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
 * End-to-end test for complex object serialization with Netty server.
 *
 * <p>Tests complete client-server communication with:</p>
 * <ul>
 *   <li>Nested objects (Address contains City)</li>
 *   <li>Class inheritance (Car extends Vehicle)</li>
 *   <li>Collections of complex objects</li>
 *   <li>Maps with complex values</li>
 * </ul>
 *
 * <p><b>Important:</b> This is an E2E test demonstrating that complex objects
 * work across the network. However, <b>best practice</b> is to use simple,
 * flat DTOs instead of deep object hierarchies.</p>
 *
 * @author Weinan Li
 * @date 2026-02-19
 */
public class ComplexObjectE2ETest {

    private TcpRestServer server;
    private int port;
    private ComplexObjectService client;

    // ========== Test Domain Objects ==========

    public static class City implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private String country;

        public City() {}
        public City(String name, String country) {
            this.name = name;
            this.country = country;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof City)) return false;
            City city = (City) o;
            return Objects.equals(name, city.name) && Objects.equals(country, city.country);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, country);
        }
    }

    public static class Address implements Serializable {
        private static final long serialVersionUID = 1L;
        private String street;
        private City city;

        public Address() {}
        public Address(String street, City city) {
            this.street = street;
            this.city = city;
        }

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }
        public City getCity() { return city; }
        public void setCity(City city) { this.city = city; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Address)) return false;
            Address address = (Address) o;
            return Objects.equals(street, address.street) && Objects.equals(city, address.city);
        }

        @Override
        public int hashCode() {
            return Objects.hash(street, city);
        }
    }

    public static class Person implements Serializable {
        private static final long serialVersionUID = 1L;
        private String name;
        private int age;
        private Address address;

        public Person() {}
        public Person(String name, int age, Address address) {
            this.name = name;
            this.age = age;
            this.address = address;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        public Address getAddress() { return address; }
        public void setAddress(Address address) { this.address = address; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return age == person.age &&
                   Objects.equals(name, person.name) &&
                   Objects.equals(address, person.address);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age, address);
        }
    }

    // Base class
    public static class Vehicle implements Serializable {
        private static final long serialVersionUID = 1L;
        private String brand;
        private int year;

        public Vehicle() {}
        public Vehicle(String brand, int year) {
            this.brand = brand;
            this.year = year;
        }

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }
        public int getYear() { return year; }
        public void setYear(int year) { this.year = year; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Vehicle)) return false;
            Vehicle vehicle = (Vehicle) o;
            return year == vehicle.year && Objects.equals(brand, vehicle.brand);
        }

        @Override
        public int hashCode() {
            return Objects.hash(brand, year);
        }
    }

    // Subclass
    public static class Car extends Vehicle {
        private static final long serialVersionUID = 1L;
        private String model;
        private int doors;

        public Car() {}
        public Car(String brand, int year, String model, int doors) {
            super(brand, year);
            this.model = model;
            this.doors = doors;
        }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public int getDoors() { return doors; }
        public void setDoors(int doors) { this.doors = doors; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Car)) return false;
            if (!super.equals(o)) return false;
            Car car = (Car) o;
            return doors == car.doors && Objects.equals(model, car.model);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), model, doors);
        }
    }

    // ========== Service Interface ==========

    public interface ComplexObjectService {
        // Nested object test
        Person createPerson(String name, int age, Address address);

        // Inheritance test
        String processVehicle(Vehicle vehicle);

        // Collection of complex objects
        List<Person> getAllPersons(List<Person> persons);

        // Map with complex values
        Map<String, Address> getAddressMap(Map<String, Address> addresses);
    }

    // ========== Service Implementation ==========

    public static class ComplexObjectServiceImpl implements ComplexObjectService {
        @Override
        public Person createPerson(String name, int age, Address address) {
            return new Person(name, age, address);
        }

        @Override
        public String processVehicle(Vehicle vehicle) {
            if (vehicle instanceof Car) {
                Car car = (Car) vehicle;
                return "Car: " + car.getBrand() + " " + car.getModel() + " (" + car.getYear() + ")";
            }
            return "Vehicle: " + vehicle.getBrand() + " (" + vehicle.getYear() + ")";
        }

        @Override
        public List<Person> getAllPersons(List<Person> persons) {
            // Return same list to verify round-trip serialization
            return new ArrayList<>(persons);
        }

        @Override
        public Map<String, Address> getAddressMap(Map<String, Address> addresses) {
            // Return same map to verify round-trip serialization
            return new HashMap<>(addresses);
        }
    }

    // ========== Test Lifecycle ==========

    @BeforeClass
    public void setup() throws Exception {
        port = PortGenerator.get();
        server = new NettyTcpRestServer(port);
        server.addSingletonResource(new ComplexObjectServiceImpl());
        server.up();

        // Wait for Netty server to be fully ready
        Thread.sleep(500);

        // Create client
        TcpRestClientFactory factory = new TcpRestClientFactory(
            ComplexObjectService.class, "localhost", port
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
    public void testNestedObjectSerialization() {
        // Create nested object: Person -> Address -> City
        City city = new City("Beijing", "China");
        Address address = new Address("123 Main St", city);

        // Call service
        Person result = client.createPerson("Alice", 30, address);

        // Verify nested structure preserved
        assertNotNull(result);
        assertEquals(result.getName(), "Alice");
        assertEquals(result.getAge(), 30);
        assertNotNull(result.getAddress());
        assertEquals(result.getAddress().getStreet(), "123 Main St");
        assertNotNull(result.getAddress().getCity());
        assertEquals(result.getAddress().getCity().getName(), "Beijing");
        assertEquals(result.getAddress().getCity().getCountry(), "China");
    }

    @Test
    public void testInheritanceSerialization() {
        // Create subclass instance
        Car car = new Car("Toyota", 2023, "Camry", 4);

        // Pass as base class (Vehicle)
        String result = client.processVehicle(car);

        // Verify server received actual Car type, not just Vehicle
        assertTrue(result.contains("Car:"));
        assertTrue(result.contains("Toyota"));
        assertTrue(result.contains("Camry"));
        assertTrue(result.contains("2023"));
    }

    @Test
    public void testCollectionOfComplexObjects() {
        // Create list of persons with nested addresses
        List<Person> persons = new ArrayList<>();
        persons.add(new Person("Alice", 25, new Address("123 Main", new City("NYC", "USA"))));
        persons.add(new Person("Bob", 30, new Address("456 Oak", new City("LA", "USA"))));
        persons.add(new Person("Charlie", 35, new Address("789 Pine", new City("SF", "USA"))));

        // Call service
        List<Person> result = client.getAllPersons(persons);

        // Verify all persons and nested structures preserved
        assertNotNull(result);
        assertEquals(result.size(), 3);

        // Check first person
        assertEquals(result.get(0).getName(), "Alice");
        assertEquals(result.get(0).getAge(), 25);
        assertEquals(result.get(0).getAddress().getStreet(), "123 Main");
        assertEquals(result.get(0).getAddress().getCity().getName(), "NYC");

        // Check second person
        assertEquals(result.get(1).getName(), "Bob");
        assertEquals(result.get(1).getAge(), 30);

        // Check third person
        assertEquals(result.get(2).getName(), "Charlie");
        assertEquals(result.get(2).getAge(), 35);
    }

    @Test
    public void testMapWithComplexValues() {
        // Create map with complex Address values
        Map<String, Address> addresses = new HashMap<>();
        addresses.put("home", new Address("123 Home St", new City("Boston", "USA")));
        addresses.put("work", new Address("456 Work Ave", new City("Cambridge", "USA")));
        addresses.put("vacation", new Address("789 Beach Rd", new City("Miami", "USA")));

        // Call service
        Map<String, Address> result = client.getAddressMap(addresses);

        // Verify all map entries and nested structures preserved
        assertNotNull(result);
        assertEquals(result.size(), 3);

        // Check home address
        assertTrue(result.containsKey("home"));
        assertEquals(result.get("home").getStreet(), "123 Home St");
        assertEquals(result.get("home").getCity().getName(), "Boston");

        // Check work address
        assertTrue(result.containsKey("work"));
        assertEquals(result.get("work").getStreet(), "456 Work Ave");

        // Check vacation address
        assertTrue(result.containsKey("vacation"));
        assertEquals(result.get("vacation").getCity().getName(), "Miami");
    }

    @Test
    public void testNullHandlingInNestedObjects() {
        // Create person with null address
        Person result = client.createPerson("NoAddress", 40, null);

        // Verify null is preserved
        assertNotNull(result);
        assertEquals(result.getName(), "NoAddress");
        assertEquals(result.getAge(), 40);
        assertNull(result.getAddress());
    }
}
