package cn.huiwings.tcprest.test.protocol;

import cn.huiwings.tcprest.converter.v2.ProtocolV2Converter;
import cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor;
import cn.huiwings.tcprest.server.Context;
import org.testng.annotations.Test;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.*;

/**
 * Test V2 protocol support for complex object scenarios.
 *
 * <p>This test demonstrates that V2 protocol supports:</p>
 * <ul>
 *   <li>Nested objects (objects containing other objects)</li>
 *   <li>Class inheritance (subclasses and superclasses)</li>
 *   <li>Complex object graphs (collections of nested objects)</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Best Practice:</b></p>
 * <p>While these complex scenarios work, the <b>recommended approach</b> is to use
 * <b>interface-based design</b> rather than complex object hierarchies:</p>
 * <ul>
 *   <li>✅ <b>Good:</b> Simple DTOs with clear data structure</li>
 *   <li>✅ <b>Good:</b> Service interfaces with simple parameter types</li>
 *   <li>❌ <b>Avoid:</b> Deep inheritance hierarchies</li>
 *   <li>❌ <b>Avoid:</b> Complex nested object graphs</li>
 * </ul>
 *
 * <p>For complex data, consider:</p>
 * <ul>
 *   <li>Flattening the object structure</li>
 *   <li>Using custom mappers with JSON (Gson/Jackson)</li>
 *   <li>Keeping DTOs simple and focused</li>
 * </ul>
 *
 * @author Weinan Li
 * @created_at 2026-02-19
 */
public class V2ComplexObjectTest {

    // ========== Nested Object Scenario ==========

    /**
     * Simple nested object: Address contains City.
     */
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
            return street.equals(address.street) && city.equals(address.city);
        }
    }

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
            return name.equals(city.name) && country.equals(city.country);
        }
    }

    /**
     * Person contains nested Address which contains nested City.
     */
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
            return age == person.age && name.equals(person.name) && address.equals(person.address);
        }
    }

    // ========== Inheritance Scenario ==========

    /**
     * Base class for vehicles.
     */
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
    }

    /**
     * Car extends Vehicle (inheritance).
     */
    public static class Car extends Vehicle {
        private static final long serialVersionUID = 1L;

        private int doors;
        private String model;

        public Car() {}

        public Car(String brand, int year, String model, int doors) {
            super(brand, year);
            this.model = model;
            this.doors = doors;
        }

        public int getDoors() { return doors; }
        public void setDoors(int doors) { this.doors = doors; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
    }

    /**
     * Truck extends Vehicle (another subclass).
     */
    public static class Truck extends Vehicle {
        private static final long serialVersionUID = 1L;

        private double loadCapacity;  // in tons

        public Truck() {}

        public Truck(String brand, int year, double loadCapacity) {
            super(brand, year);
            this.loadCapacity = loadCapacity;
        }

        public double getLoadCapacity() { return loadCapacity; }
        public void setLoadCapacity(double loadCapacity) { this.loadCapacity = loadCapacity; }
    }

    // ========== Service Interface ==========

    public static class ComplexObjectService {
        public String processNestedPerson(Person person) {
            if (person == null) return "null";
            return person.getName() + " lives in " +
                   person.getAddress().getCity().getName() + ", " +
                   person.getAddress().getCity().getCountry();
        }

        public String processVehicle(Vehicle vehicle) {
            if (vehicle == null) return "null";
            if (vehicle instanceof Car) {
                Car car = (Car) vehicle;
                return "Car: " + car.getBrand() + " " + car.getModel() + " (" + car.getDoors() + " doors)";
            } else if (vehicle instanceof Truck) {
                Truck truck = (Truck) vehicle;
                return "Truck: " + truck.getBrand() + " (capacity: " + truck.getLoadCapacity() + " tons)";
            }
            return "Vehicle: " + vehicle.getBrand() + " " + vehicle.getYear();
        }

        public String processVehicleList(List<Vehicle> vehicles) {
            if (vehicles == null) return "null";
            return "Fleet size: " + vehicles.size();
        }

        public Map<String, Person> processPersonMap(Map<String, Person> personMap) {
            // Return the same map for verification
            return personMap;
        }
    }

    // ========== Test Cases ==========

    @Test
    public void testNestedObject_TwoLevels() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processNestedPerson", Person.class);

        // Create nested object: Person -> Address -> City
        City city = new City("Beijing", "China");
        Address address = new Address("123 Main St", city);
        Person person = new Person("Alice", 30, address);

        Object[] params = new Object[]{person};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        System.out.println("Nested object encoded: " + encoded);

        // Decode and verify all nested levels are preserved
        Context context = extractor.extract(encoded);
        Person decoded = (Person) context.getParams()[0];

        assertNotNull(decoded);
        assertEquals(decoded.getName(), "Alice");
        assertEquals(decoded.getAge(), 30);
        assertNotNull(decoded.getAddress(), "Address should not be null");
        assertEquals(decoded.getAddress().getStreet(), "123 Main St");
        assertNotNull(decoded.getAddress().getCity(), "City should not be null");
        assertEquals(decoded.getAddress().getCity().getName(), "Beijing");
        assertEquals(decoded.getAddress().getCity().getCountry(), "China");
    }

    @Test
    public void testInheritance_Car() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processVehicle", Vehicle.class);

        // Create Car (subclass of Vehicle)
        Car car = new Car("Toyota", 2023, "Camry", 4);

        Object[] params = new Object[]{car};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        System.out.println("Car (subclass) encoded: " + encoded);

        // Decode: Should preserve Car type (not just Vehicle)
        Context context = extractor.extract(encoded);
        Object decoded = context.getParams()[0];

        assertNotNull(decoded);
        assertTrue(decoded instanceof Car, "Should preserve Car type");
        assertTrue(decoded instanceof Vehicle, "Should also be a Vehicle");

        Car decodedCar = (Car) decoded;
        assertEquals(decodedCar.getBrand(), "Toyota");
        assertEquals(decodedCar.getYear(), 2023);
        assertEquals(decodedCar.getModel(), "Camry");
        assertEquals(decodedCar.getDoors(), 4);
    }

    @Test
    public void testInheritance_Truck() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processVehicle", Vehicle.class);

        // Create Truck (another subclass of Vehicle)
        Truck truck = new Truck("Volvo", 2022, 15.5);

        Object[] params = new Object[]{truck};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        System.out.println("Truck (subclass) encoded: " + encoded);

        // Decode: Should preserve Truck type
        Context context = extractor.extract(encoded);
        Object decoded = context.getParams()[0];

        assertNotNull(decoded);
        assertTrue(decoded instanceof Truck, "Should preserve Truck type");
        assertTrue(decoded instanceof Vehicle, "Should also be a Vehicle");

        Truck decodedTruck = (Truck) decoded;
        assertEquals(decodedTruck.getBrand(), "Volvo");
        assertEquals(decodedTruck.getYear(), 2022);
        assertEquals(decodedTruck.getLoadCapacity(), 15.5);
    }

    @Test
    public void testComplexGraph_ListOfInheritedObjects() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processVehicleList", List.class);

        // Create list with mixed subclass instances
        List<Vehicle> vehicles = new ArrayList<>();
        vehicles.add(new Car("Honda", 2021, "Accord", 4));
        vehicles.add(new Truck("Mercedes", 2020, 20.0));
        vehicles.add(new Car("BMW", 2023, "X5", 4));

        Object[] params = new Object[]{vehicles};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        System.out.println("List of mixed Vehicle subclasses encoded: " + encoded);

        // Decode: Should preserve all types
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        List<Vehicle> decoded = (List<Vehicle>) context.getParams()[0];

        assertNotNull(decoded);
        assertEquals(decoded.size(), 3);

        // Verify first element is Car
        assertTrue(decoded.get(0) instanceof Car, "First should be Car");
        Car car1 = (Car) decoded.get(0);
        assertEquals(car1.getBrand(), "Honda");
        assertEquals(car1.getModel(), "Accord");

        // Verify second element is Truck
        assertTrue(decoded.get(1) instanceof Truck, "Second should be Truck");
        Truck truck = (Truck) decoded.get(1);
        assertEquals(truck.getBrand(), "Mercedes");
        assertEquals(truck.getLoadCapacity(), 20.0);

        // Verify third element is Car
        assertTrue(decoded.get(2) instanceof Car, "Third should be Car");
        Car car2 = (Car) decoded.get(2);
        assertEquals(car2.getBrand(), "BMW");
    }

    @Test
    public void testComplexGraph_MapOfNestedObjects() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processPersonMap", Map.class);

        // Create map with nested Person objects
        Map<String, Person> personMap = new HashMap<>();
        personMap.put("user1", new Person("Alice", 30,
            new Address("123 Main", new City("Beijing", "China"))));
        personMap.put("user2", new Person("Bob", 25,
            new Address("456 Oak", new City("Shanghai", "China"))));

        Object[] params = new Object[]{personMap};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        System.out.println("Map of nested Person objects encoded: " + encoded);

        // Decode: Should preserve map structure and nested objects
        Context context = extractor.extract(encoded);

        @SuppressWarnings("unchecked")
        Map<String, Person> decoded = (Map<String, Person>) context.getParams()[0];

        assertNotNull(decoded);
        assertEquals(decoded.size(), 2);
        assertTrue(decoded.containsKey("user1"));
        assertTrue(decoded.containsKey("user2"));

        Person alice = decoded.get("user1");
        assertEquals(alice.getName(), "Alice");
        assertEquals(alice.getAge(), 30);
        assertEquals(alice.getAddress().getCity().getName(), "Beijing");

        Person bob = decoded.get("user2");
        assertEquals(bob.getName(), "Bob");
        assertEquals(bob.getAge(), 25);
        assertEquals(bob.getAddress().getCity().getName(), "Shanghai");
    }

    @Test
    public void testNestedObject_WithNull() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processNestedPerson", Person.class);

        // Create Person with null address (testing partial object graph)
        Person person = new Person("Charlie", 35, null);

        Object[] params = new Object[]{person};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        // Decode: Should handle null nested object gracefully
        Context context = extractor.extract(encoded);
        Person decoded = (Person) context.getParams()[0];

        assertNotNull(decoded);
        assertEquals(decoded.getName(), "Charlie");
        assertEquals(decoded.getAge(), 35);
        assertNull(decoded.getAddress(), "Address should be null");
    }

    @Test
    public void testInheritance_BaseClass() throws Exception {
        ProtocolV2Converter converter = new ProtocolV2Converter();
        ProtocolV2Extractor extractor = new ProtocolV2Extractor();

        Method method = ComplexObjectService.class.getMethod("processVehicle", Vehicle.class);

        // Create base class instance (not a subclass)
        Vehicle vehicle = new Vehicle("Generic", 2020);

        Object[] params = new Object[]{vehicle};
        String encoded = converter.encode(ComplexObjectService.class, method, params, null);

        // Decode: Should preserve base class type
        Context context = extractor.extract(encoded);
        Object decoded = context.getParams()[0];

        assertNotNull(decoded);
        assertEquals(decoded.getClass(), Vehicle.class, "Should be exact Vehicle class");
        assertFalse(decoded instanceof Car, "Should not be Car");
        assertFalse(decoded instanceof Truck, "Should not be Truck");

        Vehicle decodedVehicle = (Vehicle) decoded;
        assertEquals(decodedVehicle.getBrand(), "Generic");
        assertEquals(decodedVehicle.getYear(), 2020);
    }
}
