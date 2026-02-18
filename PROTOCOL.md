# TcpRest Protocol Specification

This document explains the TcpRest wire protocol format (Protocol V2 only, as of v2.0).

## Design Philosophy

TcpRest uses a simple, text-based protocol that is:
- **Human-readable**: Debugging with `tcpdump` or `wireshark` is straightforward
- **Language-agnostic**: Any language can implement a client/server
- **Backward-compatible**: New protocol versions work with old clients
- **Efficient**: Base64 encoding with optional compression

---

## Protocol V2

The TcpRest protocol provides **method signatures**, **status codes**, and **intelligent mapper support** for robust RPC communication.

### Request Format

```
V2|[COMPRESSION]|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,...]
```

**Components:**
- `V2`: Version prefix for auto-detection
- `COMPRESSION`: `0` (uncompressed) or `1` (gzip)
- `{{base64(META)}}`: Base64-encoded metadata wrapped in `{{}}`
  - `ClassName`: Fully qualified class name
  - `methodName`: Method name
  - `TYPE_SIGNATURE`: JVM internal type signature (e.g., `(II)`, `(DD)`, `(Ljava/lang/String;)`)
- `[param1,param2,...]`: JSON-style parameter array
  - Each parameter is individually Base64-encoded
  - Parameters separated by commas
  - Special markers: `~` for null (tilde, not in Base64 charset), empty string for empty string

**Example:**
```
V2|0|{{Y24uZXhhbXBsZS5DYWxjdWxhdG9yL2FkZChJSSk=}}|[NQ==,Mw==]
```

**Breakdown:**
- `V2|0`: Protocol version 2, uncompressed
- `{{Y24uZXhhbXBsZS5DYWxjdWxhdG9yL2FkZChJSSk=}}`: Base64(`"com.example.Calculator/add(II)"`)
- `[NQ==,Mw==]`: Array of Base64(`"5"`) and Base64(`"3"`)

Decoded: Call `Calculator.add(int, int)` with params `5` and `3`

### Response Format

```
V2|[COMPRESSION]|STATUS|BODY
```

**Components:**
- `STATUS`: Status code (see below)
- `BODY`: Base64-encoded result or error message

**Example (Success):**
```
V2|0|0|{{OA==}}
```

Decoded: Success, result is `8`

**Example (Business Exception):**
```
V2|0|1|{{VmFsaWRhdGlvbkV4Y2VwdGlvbjogQWdlIG11c3QgYmUgcG9zaXRpdmU=}}
```

Decoded: Business exception with message "ValidationException: Age must be positive"

### Status Codes

| Code | Name | Meaning | Client Behavior |
|------|------|---------|-----------------|
| 0 | SUCCESS | Method executed successfully | Decode and return result |
| 1 | BUSINESS_EXCEPTION | Business logic error (extends BusinessException) | Throw RemoteBusinessException |
| 2 | SERVER_ERROR | Server-side error (NullPointerException, etc.) | Throw RemoteInvocationException |
| 3 | PROTOCOL_ERROR | Malformed request or unsupported version | Throw ProtocolException |

### Type Signatures (JVM Internal Format)

Method signatures uniquely identify overloaded methods using JVM internal type descriptors.

#### Primitive Types

| Java Type | Signature | Example Method | Full Signature |
|-----------|-----------|----------------|----------------|
| int | I | add(int, int) | (II) |
| double | D | add(double, double) | (DD) |
| long | J | multiply(long, long) | (JJ) |
| float | F | calculate(float) | (F) |
| boolean | Z | isValid(boolean) | (Z) |
| byte | B | process(byte) | (B) |
| short | S | convert(short) | (S) |
| char | C | getChar(char) | (C) |

#### Object Types

| Java Type | Signature | Example Method | Full Signature |
|-----------|-----------|----------------|----------------|
| String | Ljava/lang/String; | process(String) | (Ljava/lang/String;) |
| Integer | Ljava/lang/Integer; | box(Integer) | (Ljava/lang/Integer;) |
| Custom class | Lcom/example/Foo; | handle(Foo) | (Lcom/example/Foo;) |

#### Array Types

| Java Type | Signature | Example Method | Full Signature |
|-----------|-----------|----------------|----------------|
| int[] | [I | sum(int[]) | ([I) |
| String[] | [Ljava/lang/String; | join(String[]) | ([Ljava/lang/String;) |
| int[][] | [[I | matrix(int[][]) | ([[I) |

#### Complex Examples

```java
// Method: public int add(int a, int b)
// Signature: (II)

// Method: public double add(double a, double b)
// Signature: (DD)

// Method: public String process(String s, int n, boolean flag)
// Signature: (Ljava/lang/String;IZ)

// Method: public int sum(int[] numbers)
// Signature: ([I)

// Method: public void configure(Map<String, Object> config)
// Signature: (Ljava/util/Map;)  // Generics are erased
```

### Parameter Encoding

Parameters are Base64-encoded individually and placed in a JSON-style array `[param1,param2,...]`.

**Encoding Rules:**
- `null` → `~` marker (tilde, not in Base64 charset, not Base64-encoded)
- Empty string `""` → empty string (consecutive commas: `[a,,c]` or empty array `[]` for single param)
- Primitives → `toString()` then Base64
- Arrays → `Arrays.toString()` format then Base64 (e.g., `[1, 2, 3]`)
- Objects → `toString()` then Base64

#### Null Marker Design Rationale

**Why tilde (`~`) for null?**

Protocol V2 initially used `"NULL"` as the null marker, but this was optimized to `~` (single tilde character) for efficiency:

1. **Size Reduction**: 75% smaller (1 byte vs 4 bytes)
   - `"NULL"` = 4 characters
   - `"~"` = 1 character
   - Significant savings when null values are common

2. **No Base64 Charset Conflict**: Tilde is NOT in the Base64 character set
   - Base64 uses: `A-Z`, `a-z`, `0-9`, `+`, `/`, `=`
   - Tilde (`~`) is safe as an unencoded marker
   - No ambiguity with Base64-encoded parameters

3. **Distinguishable from Empty String**:
   - `null` → `~` (tilde marker)
   - `""` → `` (empty string, consecutive commas)
   - Clear semantic difference

**Why empty string for empty string?**

Empty strings are represented by omitting content between commas:
- Single empty param: `[]` (empty array content)
- Empty in array: `[a,,c]` (consecutive commas)
- No encoding overhead (0 bytes instead of 5 bytes for `"EMPTY"`)

**Examples:**
```
[~]        → one null parameter
[]         → one empty string parameter
[~,~]      → two null parameters
[,]        → two empty string parameters
[a,,c]     → ["a", "", "c"]
[~,,c]     → [null, "", "c"]
```

**Examples:**

| Java Value | Encoded in Array | Decoded |
|------------|------------------|---------|
| `5` | `NQ==` | `"5"` |
| `null` | `~` | `null` |
| `""` | `` (empty) | `""` |
| `"hello"` | `aGVsbG8=` | `"hello"` |
| `new int[]{1, 2, 3}` | `WzEsIDIsIDNd` | `"[1, 2, 3]"` |

**Full Request Example:**
```
V2|0|{{Y24uZXhhbXBsZS5UZXN0L21ldGhvZChMamF2YS9sYW5nL1N0cmluZztJTGphdmEvbGFuZy9TdHJpbmc7KQ==}}|[aGVsbG8=,~,d29ybGQ=]
```
Calls: `Test.method(String, int, String)` with params `("hello", null, "world")`

**Note:** The `~` marker represents `null`, replacing the verbose `NULL` marker for better efficiency.

### V2 Intelligent Mapper Support

Protocol V2 includes an intelligent 3-tier type mapping system that automatically handles complex objects without requiring manual mapper implementation for every class.

#### Mapper Priority System

When encoding/decoding parameters and return values, V2 uses the following priority:

**Priority 1: User-Defined Mappers (Highest)**
- Custom mappers registered via `server.addMapper()` or client `mappers` parameter
- Provides fine-grained control over serialization format
- Example: Using Gson for JSON serialization

**Priority 2: Common Collection Interfaces (High)**
- Built-in support for `List`, `Map`, `Set`, `Queue`, `Deque`, `Collection` interfaces
- No custom mapper needed - works automatically
- Actual implementation classes (ArrayList, HashMap, etc.) are automatically serialized
- Deserialized as-is (e.g., ArrayList → ArrayList, HashMap → HashMap)
- **Note:** Without generic type info, collections are deserialized with their runtime types (e.g., `List<Object>`)
- For specific generic types, use custom mappers (e.g., Gson with TypeToken)

**Priority 3: Automatic Serialization (Medium)**
- Any class implementing `java.io.Serializable` is automatically handled
- Uses Java's built-in serialization via `RawTypeMapper`
- No mapper registration required
- Supports `transient` fields (automatically excluded)

**Priority 4: Built-in Conversion (Lowest)**
- Primitives (`int`, `double`, `boolean`, etc.)
- Primitive wrappers (`Integer`, `Double`, `Boolean`, etc.)
- Strings
- Arrays (primitive and object arrays)

#### Collection Interface Support

Protocol V2 provides built-in support for common Java collection interfaces without requiring custom mappers.

**Supported Interfaces:**
- `java.util.List` - ArrayList, LinkedList, Vector, etc.
- `java.util.Map` - HashMap, TreeMap, LinkedHashMap, etc.
- `java.util.Set` - HashSet, TreeSet, LinkedHashSet, etc.
- `java.util.Queue` - LinkedList, PriorityQueue, etc.
- `java.util.Deque` - ArrayDeque, LinkedList, etc.
- `java.util.Collection` - Parent interface for all collections

**How It Works:**

When a method declares a collection interface parameter (e.g., `List<String>`), V2 protocol:

1. **Encoding (Client → Server):** Serializes the actual implementation (ArrayList, HashMap, etc.) using Java serialization
2. **Decoding (Server):** Deserializes back to the same implementation class
3. **Type Preservation:** ArrayList remains ArrayList, HashMap remains HashMap

**Example:**

```java
// Service interface - uses interface types, not concrete classes
public interface DataService {
    String processList(List<String> items);      // List interface
    String processMap(Map<String, Integer> data); // Map interface
    String processSet(Set<String> unique);       // Set interface
}

// Server implementation
public class DataServiceImpl implements DataService {
    @Override
    public String processList(List<String> items) {
        return "Received " + items.size() + " items: " + String.join(", ", items);
    }

    @Override
    public String processMap(Map<String, Integer> data) {
        return "Map with " + data.size() + " entries";
    }

    @Override
    public String processSet(Set<String> unique) {
        return "Set with " + unique.size() + " unique items";
    }
}

// Server setup - no custom mapper needed!
TcpRestServer server = new SingleThreadTcpRestServer(8080);
server.setProtocolVersion(ProtocolVersion.V2);
server.addResource(new DataServiceImpl());
server.up();

// Client setup - no custom mapper needed!
TcpRestClientFactory factory = new TcpRestClientFactory(
    DataService.class, "localhost", 8080
);
DataService client = factory.getClient();

// Client calls - pass any compatible implementation
List<String> myList = new ArrayList<>();
myList.add("apple");
myList.add("banana");
String result1 = client.processList(myList);  // ArrayList → List → ArrayList

Map<String, Integer> myMap = new HashMap<>();
myMap.put("x", 100);
String result2 = client.processMap(myMap);    // HashMap → Map → HashMap

Set<String> mySet = new TreeSet<>();
mySet.add("alpha");
String result3 = client.processSet(mySet);    // TreeSet → Set → TreeSet
```

**Wire Protocol:**

```
# List parameter (ArrayList with ["apple", "banana"])
V2|0|{{...DataService/processList(Ljava/util/List;)...}}|[rO0ABXNyABNqYXZhLnV0aWwuQXJyYXlMaXN0...]

# Map parameter (HashMap with {"x": 100})
V2|0|{{...DataService/processMap(Ljava/util/Map;)...}}|[rO0ABXNyABFqYXZhLnV0aWwuSGFzaE1hcA...]

# Set parameter (TreeSet with ["alpha"])
V2|0|{{...DataService/processSet(Ljava/util/Set;)...}}|[rO0ABXNyABFqYXZhLnV0aWwuVHJlZVNldD...]
```

**Important Notes:**

1. **Generic Type Erasure:** Due to Java type erasure, collections are deserialized without generic type information
   - `List<String>` is deserialized as `List` (raw type) with runtime element types
   - Elements retain their actual types (String, Integer, etc.)
   - This works fine for most use cases

2. **When to Use Custom Mappers:**
   - **Specific generic types needed:** Use Gson/Jackson with TypeToken for `List<User>`, `Map<String, Order>`, etc.
   - **Cross-language compatibility:** Use JSON/XML mappers for non-Java clients
   - **Human-readable format:** Use JSON mappers for debugging/logging

3. **Performance:** Collection serialization uses binary Java serialization
   - Fast for Java-to-Java communication
   - Larger than JSON for simple data
   - Includes metadata for object graphs and type info

**Custom Mapper for Specific Types:**

```java
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

// Custom mapper for List<User> with specific generic type
public class UserListMapper implements Mapper {
    private final Gson gson = new Gson();
    private final Type type = new TypeToken<List<User>>(){}.getType();

    @Override
    public String objectToString(Object object) {
        return gson.toJson(object);
    }

    @Override
    public Object stringToObject(String param) {
        return gson.fromJson(param, type);  // Preserves List<User> type
    }
}

// Register for the specific interface method parameter
// Note: This requires registering by parameter position, which is advanced usage
```

#### Auto-Serialization Example

**Without mapper (automatic):**
```java
// DTO class - just implement Serializable
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private int age;
    private transient String password;  // Excluded from serialization

    // Constructor, getters, setters...
}

// Service interface
public interface UserService {
    User getUser(int id);        // Works automatically!
    List<User> getAllUsers();    // Collections work too!
}

// Server setup - no mapper needed
TcpRestServer server = new SingleThreadTcpRestServer(8001);
server.setProtocolVersion(ProtocolVersion.V2);
server.addSingletonResource(new UserServiceImpl());
server.up();

// Client setup - no mapper needed
TcpRestClientFactory factory = new TcpRestClientFactory(
    UserService.class, "localhost", 8001
);
UserService client = factory.getClient();

User user = client.getUser(123);  // Just works!
```

**Wire Protocol (Auto-Serialization):**

The `RawTypeMapper` uses Java serialization and returns Base64-encoded serialized bytes directly:
```
V2|0|{{...metadata...}}|[rO0ABXNyABFjb24uZXhhbXBsZS5Vc2VyAAAAAAAAAAECAAJJAANhZ2VMAANuYW1ldAAST...]
```

The serialized object includes all non-transient fields.

#### Custom Mapper Example

**With Gson mapper (custom control):**
```java
import com.google.gson.Gson;
import cn.huiwings.tcprest.mapper.Mapper;

// Custom Gson mapper for JSON serialization
public class GsonUserMapper implements Mapper {
    private final Gson gson = new Gson();

    @Override
    public String objectToString(Object object) {
        return gson.toJson(object);  // Convert to JSON
    }

    @Override
    public Object stringToObject(String param) {
        return gson.fromJson(param, User.class);  // Parse from JSON
    }
}

// Server side: Register custom mapper
server.addMapper(User.class.getCanonicalName(), new GsonUserMapper());

// Client side: Register custom mapper
Map<String, Mapper> mappers = new HashMap<>();
mappers.put(User.class.getCanonicalName(), new GsonUserMapper());

TcpRestClientFactory factory = new TcpRestClientFactory(
    UserService.class, "localhost", 8001, mappers
);
```

**Wire Protocol (Custom Mapper with Gson):**
```
V2|0|{{...metadata...}}|[eyJuYW1lIjoiQWxpY2UiLCJhZ2UiOjI1fQ==]
```

Base64 decodes to: `{"name":"Alice","age":25}` (human-readable JSON)

#### When to Use Each Approach

| Approach | Use Case | Pros | Cons |
|----------|----------|------|------|
| **Collection Interfaces** | List, Map, Set parameters in method signatures | Zero configuration, works with any implementation, type-preserving | Type erasure (no generic type info), Java-only, binary format |
| **Auto-Serialization** | Internal microservices, DTOs you control | Zero configuration, handles complex objects, supports transient | Binary format, larger size, Java-only |
| **Custom Mapper** | Public APIs, cross-language, human-readable wire format, specific generic types | Full control, readable format, efficient, preserves generics | Requires manual implementation |
| **Built-in** | Primitives, strings, simple types | Fast, minimal overhead | Limited to basic types |

#### Best Practices

1. **For collection parameters**: Use interface types (List, Map, Set) in method signatures
   ```java
   // Good: Interface types work automatically
   public String process(List<String> items, Map<String, Integer> data);

   // Also works: Concrete types
   public String process(ArrayList<String> items, HashMap<String, Integer> data);
   ```

2. **For new DTOs**: Implement `Serializable` first (zero effort)
   ```java
   public class MyDTO implements Serializable {
       private static final long serialVersionUID = 1L;
       // Fields...
   }
   ```

3. **For public APIs**: Use custom mappers with JSON
   ```java
   server.addMapper(MyDTO.class.getCanonicalName(), new GsonMapper());
   ```

4. **For sensitive data**: Mark fields `transient` or use custom mapper
   ```java
   private transient String password;  // Not serialized
   ```

5. **For nested objects**: All nested objects must be Serializable or have mappers
   ```java
   public class Order implements Serializable {
       private User user;        // User must also be Serializable
       private Product product;  // Product must also be Serializable
   }
   ```

6. **Avoid complex object hierarchies** - Use simple, flat DTOs instead of deep inheritance
   ```java
   // ❌ Avoid: Deep inheritance and complex object graphs
   public class Car extends Vehicle extends MovableObject { ... }
   public class Person {
       private Address address;          // nested
       private Company company;          // nested
       private List<Skill> skills;       // nested collection
       private Map<String, Certificate> certs;  // nested map
   }

   // ✅ Better: Simple, focused DTOs
   public class PersonBasicInfo implements Serializable {
       private String name;
       private int age;
       private String city;  // Flatten nested Address.city
   }

   // ✅ Best: Use service interfaces with simple parameters
   public interface UserService {
       UserInfo getUserInfo(String userId);
       boolean updateUser(String userId, String name, String city);
   }
   ```

#### Mapper Resolution Algorithm

```java
// Encoding (client → server)
String encodeParam(Object param, Map<String, Mapper> mappers) {
    // 1. Check user-defined mapper
    if (mappers != null) {
        Mapper mapper = mappers.get(param.getClass().getCanonicalName());
        if (mapper != null) {
            return base64(mapper.objectToString(param));
        }
    }

    // 2. Check if Serializable (auto-serialization)
    if (param instanceof Serializable &&
        !(param instanceof String) &&
        !param.getClass().isArray() &&
        !isWrapperType(param.getClass())) {
        RawTypeMapper rawMapper = new RawTypeMapper();
        return rawMapper.objectToString(param);  // Already Base64
    }

    // 3. Use built-in conversion
    return base64(param.toString());
}

// Decoding (server → client)
Object decodeParam(String paramStr, Class<?> paramType, Map<String, Mapper> mappers) {
    // 1. Check user-defined mapper
    if (mappers != null) {
        Mapper mapper = mappers.get(paramType.getCanonicalName());
        if (mapper != null) {
            return mapper.stringToObject(unbase64(paramStr));
        }
    }

    // 2. Check common collection interfaces (List, Map, Set, etc.)
    if (isCommonCollectionInterface(paramType)) {
        RawTypeMapper rawMapper = new RawTypeMapper();
        return rawMapper.stringToObject(paramStr);
    }

    // 3. Check if Serializable (auto-deserialization)
    if (Serializable.class.isAssignableFrom(paramType) && ...) {
        RawTypeMapper rawMapper = new RawTypeMapper();
        return rawMapper.stringToObject(paramStr);
    }

    // 4. Use built-in conversion
    return convertPrimitive(unbase64(paramStr), paramType);
}
```

#### Complete Working Example

See test files for complete examples:
- **Auto-serialization**: `tcprest-singlethread/src/test/java/.../v2mapper/V2MapperDemoTest.java#testAutoSerialization`
- **Custom Gson mapper**: `tcprest-singlethread/src/test/java/.../v2mapper/V2MapperDemoTest.java#testCustomMapperWithGson`
- **Mixed types**: `tcprest-singlethread/src/test/java/.../v2mapper/V2MapperDemoTest.java#testMixedTypes`
- **Collection interfaces**: `tcprest-commons/src/test/java/.../protocol/V2CollectionInterfaceTest.java`
- **Complex objects**: `tcprest-commons/src/test/java/.../protocol/V2ComplexObjectTest.java`

---

### Complex Object Support (Nested Objects & Inheritance)

V2 protocol supports complex object scenarios through Java serialization:
- ✅ **Nested objects** (objects containing other objects)
- ✅ **Class inheritance** (subclasses and superclasses)
- ✅ **Complex object graphs** (collections of nested objects)

**How It Works:**

Java serialization automatically handles object graphs:
1. Serializes entire object tree (including nested objects)
2. Preserves exact class types (Car remains Car, not just Vehicle)
3. Handles circular references (automatically detects and resolves)
4. Maintains object identity (same object referenced twice = one serialization)

**Example - Nested Objects:**

```java
// Nested object structure: Person -> Address -> City
public class Person implements Serializable {
    private String name;
    private Address address;  // Nested object
}

public class Address implements Serializable {
    private String street;
    private City city;  // Nested object (2 levels deep)
}

public class City implements Serializable {
    private String name;
    private String country;
}

// Service interface
public interface UserService {
    String processUser(Person person);  // Works automatically!
}

// Client call
Person person = new Person("Alice",
    new Address("123 Main St",
        new City("Beijing", "China")));
String result = client.processUser(person);  // All nested objects preserved
```

**Example - Inheritance:**

```java
// Base class
public class Vehicle implements Serializable {
    private String brand;
    private int year;
}

// Subclass
public class Car extends Vehicle {
    private String model;
    private int doors;
}

// Service interface
public interface GarageService {
    String processVehicle(Vehicle vehicle);  // Base class parameter
}

// Client call with subclass
Car car = new Car("Toyota", 2023, "Camry", 4);
String result = client.processVehicle(car);  // Car type preserved, not just Vehicle!

// Server can check actual type
public String processVehicle(Vehicle vehicle) {
    if (vehicle instanceof Car) {
        Car car = (Car) vehicle;
        return "Car: " + car.getModel();
    }
    return "Vehicle: " + vehicle.getBrand();
}
```

**Example - Collections of Complex Objects:**

```java
// List of mixed subclass instances
List<Vehicle> fleet = new ArrayList<>();
fleet.add(new Car("Honda", 2021, "Accord", 4));
fleet.add(new Truck("Volvo", 2022, 15.5));  // Different subclass
fleet.add(new Car("BMW", 2023, "X5", 4));

String result = client.processFleet(fleet);  // All types preserved!
```

**⚠️ IMPORTANT - Best Practice Recommendation:**

While V2 supports these complex scenarios, **we strongly recommend keeping object structures simple**:

**❌ Avoid:**
- Deep inheritance hierarchies (Vehicle → Car → SportsCar → FerrariF40)
- Deeply nested objects (Person → Address → Building → Floor → Room → Desk)
- Complex object graphs with circular references
- Large collections of nested objects

**✅ Prefer:**
- **Interface-based design**: Define service interfaces with simple parameter types
- **Flat DTOs**: Single-level objects with primitive fields
- **Separate concerns**: Multiple simple methods instead of one complex method
- **Custom mappers**: Use Gson/Jackson for complex data with controlled serialization

**Why Simple is Better:**

| Aspect | Complex Objects | Simple DTOs |
|--------|----------------|-------------|
| **Wire size** | Large (includes class metadata, object graphs) | Small (just data) |
| **Debugging** | Hard to inspect binary serialization | Easy with JSON mappers |
| **Cross-language** | Java-only | JSON works everywhere |
| **Versioning** | `serialVersionUID` required | Easy to evolve |
| **Performance** | Slower (deep object graph traversal) | Faster |
| **Clarity** | Hard to understand API contract | Clear, explicit parameters |

**Migration Path:**

If you have complex objects, consider refactoring:

```java
// ❌ Complex: One method with nested object
public interface UserService {
    void updateUser(Person person);  // Person contains Address, Company, etc.
}

// ✅ Simple: Multiple focused methods
public interface UserService {
    void updateBasicInfo(String userId, String name, int age);
    void updateAddress(String userId, String city, String country);
    void updateCompany(String userId, String companyName);
}
```

**When Complex Objects Make Sense:**

Use complex objects for:
- **Internal microservices**: Where both ends are Java and you control both
- **Rapid prototyping**: Get working quickly, refactor later
- **Existing systems**: Gradual migration (V2 supports legacy code)

**Test Coverage:**

See `V2ComplexObjectTest.java` for comprehensive examples:
- ✅ Nested objects (2+ levels deep)
- ✅ Inheritance (base class, subclass preservation)
- ✅ Mixed collections (List\<Vehicle\> with Car and Truck instances)
- ✅ Maps of nested objects (Map\<String, Person\>)
- ✅ Null handling in nested structures

---

## Protocol Format

All requests and responses use the Protocol V2 format with the `V2|` prefix.

---

## Exception Handling

**Server Behavior:**
```java
try {
    Object result = method.invoke(instance, params);
    return "V2|0|0|" + encodeResult(result);  // SUCCESS
} catch (BusinessException e) {
    return "V2|0|1|" + encodeException(e);    // BUSINESS_EXCEPTION
} catch (Exception e) {
    return "V2|0|2|" + encodeException(e);    // SERVER_ERROR
}
```

**Client Receives:** Proper exception with type and message

**Exception Categories:**

1. **Business Exceptions** (Status Code 1)
   - Extend `cn.huiwings.tcprest.exception.BusinessException`
   - Represent expected error conditions (validation failures, business rule violations)
   - Client receives: `RemoteBusinessException` with original message

2. **Server Errors** (Status Code 2)
   - Any other exception (NullPointerException, IllegalArgumentException, etc.)
   - Represent unexpected errors
   - Client receives: `RemoteInvocationException` with stack trace info

3. **Protocol Errors** (Status Code 3)
   - Malformed requests, unsupported versions
   - Client receives: `ProtocolException`

**Example:**

```java
// Server-side service
public class UserService {
    public void validateAge(int age) {
        if (age < 0) {
            throw new ValidationException("Age must be non-negative");
        }
    }
}

// Client-side call (V2)
try {
    userService.validateAge(-1);
} catch (RuntimeException e) {
    // Receives: RuntimeException wrapping original ValidationException
    // Message: "ValidationException: Age must be non-negative"
}
```

---

## Compression Support

Both protocols support optional gzip compression to reduce bandwidth.

### Compression Flag

| Value | Meaning | When to Use |
|-------|---------|-------------|
| 0 | Uncompressed | Small messages (<1KB) |
| 1 | Gzip compressed | Large messages (>1KB) |

### Compression Configuration

**Server:**
```java
TcpRestServer server = new SingleThreadTcpRestServer(8080);
server.enableCompression();  // Automatic compression for large responses
```

**Client:**
```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
)
    .withCompression();  // Compress large requests
```

### Compression Threshold

Default threshold: 1024 bytes

```java
CompressionConfig config = new CompressionConfig();
config.setEnabled(true);
config.setThreshold(2048);  // Only compress if >2KB
config.setLevel(6);         // Compression level (0-9)
```

**Note:** Compression is applied AFTER Base64 encoding, so the threshold checks the encoded size.

---

## Wire Protocol Examples

### Example 1: Simple Method Call (V2)

**Java Code:**
```java
calculator.add(10, 20);
```

**Wire Request:**
```
V2|0|{{Y29tLmV4YW1wbGUuQ2FsY3VsYXRvci9hZGQoSUkp}}|[MTA=,MjA=]
```

**Breakdown:**
- `V2|0`: Protocol version 2, uncompressed
- `{{Y29tLmV4YW1wbGUuQ2FsY3VsYXRvci9hZGQoSUkp}}`: Base64(`"com.example.Calculator/add(II)"`)
  - Class: `com.example.Calculator`
  - Method: `add`
  - Signature: `(II)` for `add(int, int)`
- `[MTA=,MjA=]`: Array with Base64(`"10"`) and Base64(`"20"`)

**Wire Response:**
```
V2|0|0|{{MzA=}}
```

**Breakdown:**
- `V2|0`: Version + uncompressed
- `0`: SUCCESS status
- `{{MzA=}}`: Base64(`"30"`)

### Example 2: Method Overloading (V2)

**Java Code:**
```java
calculator.add(2.5, 3.5);  // Different overload!
```

**Wire Request:**
```
V2|0|{{Y29tLmV4YW1wbGUuQ2FsY3VsYXRvci9hZGQoREQp}}|[Mi41,My41]
```

**Key Difference:**
- `{{Y29tLmV4YW1wbGUuQ2FsY3VsYXRvci9hZGQoREQp}}`: Metadata includes `(DD)` signature, not `(II)`
- Server uses signature to find correct overload: `add(double, double)` instead of `add(int, int)`

### Example 3: Exception Handling (V2)

**Java Code:**
```java
validator.validateAge(-5);
```

**Wire Request:**
```
V2|0|{{Y29tLmV4YW1wbGUuVmFsaWRhdG9yL3ZhbGlkYXRlQWdlKEkp}}|[LTU=]
```

**Breakdown:**
- `{{Y29tLmV4YW1wbGUuVmFsaWRhdG9yL3ZhbGlkYXRlQWdlKEkp}}`: Base64(`"com.example.Validator/validateAge(I)"`)
- `[LTU=]`: Array with Base64(`"-5"`)

**Wire Response (Error):**
```
V2|0|1|{{VmFsaWRhdGlvbkV4Y2VwdGlvbjogQWdlIG11c3QgYmUgbm9uLW5lZ2F0aXZl}}
```

**Breakdown:**
- `1`: BUSINESS_EXCEPTION status
- Body: Base64(`"ValidationException: Age must be non-negative"`)

### Example 4: Array Parameter (V2)

**Java Code:**
```java
calculator.sum(new int[]{1, 2, 3, 4, 5});
```

**Wire Request:**
```
V2|0|{{Y29tLmV4YW1wbGUuQ2FsY3VsYXRvci9zdW0oW0kp}}|[WzEsIDIsIDMsIDQsIDVd]
```

**Breakdown:**
- `{{Y29tLmV4YW1wbGUuQ2FsY3VsYXRvci9zdW0oW0kp}}`: Base64(`"com.example.Calculator/sum([I)"`)
  - Signature `([I)` indicates `sum(int[])`
- `[WzEsIDIsIDMsIDQsIDVd]`: Array with Base64(`"[1, 2, 3, 4, 5]"`)

**Wire Response:**
```
V2|0|0|{{MTU=}}
```

Result: `15`

---

## Implementation Notes

### Type Signature Generation

The `TypeSignatureUtil` class generates JVM signatures from Java `Method` objects:

```java
Method method = Calculator.class.getMethod("add", int.class, int.class);
String signature = TypeSignatureUtil.getMethodSignature(method);
// Returns: "(II)"
```

### Method Lookup

Finding the exact overload:

```java
Class<?> clazz = Calculator.class;
String methodName = "add";
String signature = "(DD)";

Method method = TypeSignatureUtil.findMethodBySignature(clazz, methodName, signature);
// Returns: public double add(double, double)
```

### Interface-to-Implementation Mapping

The server automatically maps interface calls to registered implementations:

```java
// Client sends: "com.example.MyService/doWork(I)"
// Server looks up: registered instance implementing MyService
// Server finds: MyServiceImpl instance and invokes it
```

No explicit registration of implementation class name is needed.

---

## Performance Considerations

### Protocol Overhead

**V1 Request Overhead:** ~50 bytes + Base64 encoding (33% inflation)
**V2 Request Overhead:** ~60 bytes + Base64 encoding + signature (~10-30 bytes)

**Example Comparison:**

| Payload | V1 Size | V2 Size | Overhead Increase |
|---------|---------|---------|-------------------|
| 100 bytes | ~183 bytes | ~203 bytes | +11% |
| 1KB | ~1.4KB | ~1.42KB | +1.4% |
| 10KB | ~13.4KB | ~13.42KB | +0.15% |

**Conclusion:** V2 overhead is negligible for typical use cases.

### Signature Caching

Type signatures are computed once per method and cached:

```java
// First call: computes signature
Method method = clazz.getMethod("add", int.class, int.class);
String sig1 = TypeSignatureUtil.getMethodSignature(method);  // ~100μs

// Subsequent calls: uses cached value
String sig2 = TypeSignatureUtil.getMethodSignature(method);  // ~1μs
```

### Compression Trade-offs

| Message Size | Compress? | Reason |
|--------------|-----------|--------|
| <1KB | No | Compression overhead > savings |
| 1-10KB | Maybe | Depends on data (text vs binary) |
| >10KB | Yes | Significant bandwidth savings |

Typical compression ratios for text data: 60-80% reduction

---

## Security Considerations

### Security Vulnerabilities Addressed

TcpRest protocol has evolved to address several injection attack vectors:

#### 1. Path Traversal Attack
```
Class: ../../evil/MaliciousClass
```
**Risk:** Attacker could access unauthorized classes using relative paths
**Mitigation:** Base64 encoding + validation with `ProtocolSecurity.isValidClassName()`

#### 2. Delimiter Injection
```java
ClassName: com.example.MyClass/evilMethod()
// Would break parsing: "com.example.MyClass/evilMethod()/realMethod(...)"
```
**Risk:** Injecting protocol delimiters (`/`, `|`, `:::`) to manipulate parsing
**Mitigation:** All variable content (class names, method names, parameters) are Base64-encoded

#### 3. Method Name Injection
```
Method: getData():::maliciousParam
```
**Risk:** Inject fake parameters by manipulating method names
**Mitigation:** Base64 encoding prevents delimiter injection

#### 4. Message Tampering
**Risk:** Messages modified in transit without detection
**Mitigation:** Optional CRC32 (fast) or HMAC-SHA256 (cryptographic) checksums

### Security Features

#### Base64 Encoding (Built-in)
All protocol components use **URL-safe Base64** encoding:
- Replaces `+` with `-` and `/` with `_`
- Omits padding `=` for cleaner format
- Prevents all delimiter injection attacks

#### Optional Security Enhancements (SecurityConfig)

TcpRest supports optional security features via `SecurityConfig`:

```java
import cn.huiwings.tcprest.security.SecurityConfig;

// No security (default)
SecurityConfig config = new SecurityConfig();

// With CRC32 checksum (detects accidental corruption)
SecurityConfig config = new SecurityConfig()
    .enableCRC32();

// With HMAC-SHA256 (cryptographic authentication)
SecurityConfig config = new SecurityConfig()
    .enableHMAC("my-secret-key-123");

// With class whitelist (restrict callable classes)
SecurityConfig config = new SecurityConfig()
    .enableClassWhitelist()
    .allowClass("com.example.SafeService")
    .allowClasses("com.example.Service1", "com.example.Service2");

// Combined security
SecurityConfig config = new SecurityConfig()
    .enableHMAC("secret")
    .enableClassWhitelist()
    .allowClass("com.example.Service");
```

#### Checksum Format

When checksums are enabled, protocol messages include a `CHK:` suffix:

**V1 with checksum:**
```
0|{{base64_meta}}|{{base64_params}}|CHK:a1b2c3d4
```

**V2 with checksum:**
```
V2|0|{{base64_meta}}|{{base64_params}}|CHK:def789
```

#### Server-Side Security Configuration

```java
import cn.huiwings.tcprest.server.SingleThreadTcpRestServer;
import cn.huiwings.tcprest.security.SecurityConfig;

// Create server
TcpRestServer server = new SingleThreadTcpRestServer(8080);

// Configure security
SecurityConfig securityConfig = new SecurityConfig()
    .enableHMAC("shared-secret-key")
    .enableClassWhitelist()
    .allowClass("com.example.PublicAPI")
    .allowClass("com.example.UserService");

server.setSecurityConfig(securityConfig);
server.addResource(PublicAPI.class);
server.addResource(UserService.class);
server.up();
```

#### Client-Side Security Configuration

```java
import cn.huiwings.tcprest.client.TcpRestClientFactory;
import cn.huiwings.tcprest.security.SecurityConfig;

// Create client factory
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
);

// Configure security (must match server)
SecurityConfig securityConfig = new SecurityConfig()
    .enableHMAC("shared-secret-key");

factory.setSecurityConfig(securityConfig);

MyService client = (MyService) factory.getInstance();
String result = client.getData();
```

### Security Analysis

| Attack Vector | Mitigation | Implementation |
|---------------|------------|----------------|
| Path Traversal (`../../EvilClass`) | Base64 encoding + validation | `ProtocolSecurity.isValidClassName()` |
| Delimiter Injection (`Class/evil()/method`) | Base64 encoding | All components encoded |
| Method Name Injection (`method:::param`) | Base64 encoding | Metadata fully encoded |
| Message Tampering | Optional checksums | CRC32 or HMAC-SHA256 |
| Unauthorized Class Access | Optional whitelist | `SecurityConfig.isClassAllowed()` |
| Arbitrary Method Invocation | Resource registration | Only registered classes callable |
| Private Method Access | Reflection filtering | Only public methods accessible |
| Exception Information Leakage | Sanitization | Developer responsibility |
| Denial of Service (zip bomb) | Size limits | `CompressionConfig.setMaxSize()` |

### Performance Impact

**Encoding Overhead:**
- Base64 encoding: ~33% size increase (3 bytes → 4 bytes)
- CRC32 checksum: ~8 bytes hex (~2% overhead for typical messages)
- HMAC-SHA256: ~64 bytes hex (~5-10% overhead)

**Computational Overhead:**
- Base64 encode/decode: <1μs per component (JDK intrinsic)
- CRC32 calculation: <1μs per message
- HMAC-SHA256 calculation: <10μs per message

**Total overhead: <5% for most workloads**

### Security Best Practices

#### 1. Enable Checksums in Production

```java
// Development (no checksum for easier debugging)
SecurityConfig devConfig = new SecurityConfig();

// Production (HMAC for security)
SecurityConfig prodConfig = new SecurityConfig()
    .enableHMAC(System.getenv("TCPREST_HMAC_SECRET"));
```

#### 2. Use Class Whitelist for Public APIs

```java
SecurityConfig config = new SecurityConfig()
    .enableClassWhitelist()
    .allowClass("com.company.publicapi.UserService")
    .allowClass("com.company.publicapi.OrderService");
    // Do NOT whitelist internal/admin classes
```

#### 3. Rotate HMAC Secrets Regularly

```java
// Use environment variable or secure config
String secret = System.getenv("TCPREST_SECRET");
if (secret == null || secret.isEmpty()) {
    throw new IllegalStateException("TCPREST_SECRET must be set");
}
SecurityConfig config = new SecurityConfig().enableHMAC(secret);
```

#### 4. Sanitize Exception Messages

```java
// Bad: Leaks sensitive info
throw new Exception("Database password: " + pwd);

// Good: Generic error message
throw new Exception("Database connection failed");
```

#### 5. Log Security Events

```java
try {
    // Process request
} catch (SecurityException e) {
    logger.error("Security violation detected: " + e.getMessage());
    // Consider: Alert security team, block IP after repeated violations
}
```

#### 6. Enforce Compression Size Limits

```java
CompressionConfig config = new CompressionConfig();
config.setMaxSize(10 * 1024 * 1024);  // 10MB limit prevents zip bombs
```

---

## Future Protocol Versions

### Potential V3 Features

- **Binary protocol option**: Reduce Base64 overhead
- **Streaming support**: Large file transfers
- **Multiplexing**: Multiple requests over one connection
- **Authentication headers**: Built-in auth support
- **Schema validation**: JSON Schema or Protocol Buffers

### Versioning Strategy

- All new versions will use prefix-based detection (e.g., `V3|...`)
- `ProtocolVersion.AUTO` will support all versions
- Deprecated versions will be supported for minimum 2 major releases

---

## Appendix: Base64 Encoding Reference

TcpRest uses standard Base64 (RFC 4648) for parameter encoding.

**Encoding Examples:**

| Input | Base64 Output |
|-------|---------------|
| `5` | `NQ==` |
| `Hello` | `SGVsbG8=` |
| `[1, 2, 3]` | `WzEsIDIsIDNd` |
| `true` | `dHJ1ZQ==` |
| `3.14` | `My4xNA==` |

**Why Base64?**
- Safe for text-based protocols (no control characters)
- Language-agnostic encoding/decoding
- Simple to debug (easily decoded with command-line tools)

**Command-line Decoding:**
```bash
# Decode a parameter
echo "SGVsbG8=" | base64 -d
# Output: Hello
```

---

## References

- **JVM Type Signatures:** [Java VM Specification §4.3](https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-4.html#jvms-4.3)
- **Base64 Encoding:** [RFC 4648](https://datatracker.ietf.org/doc/html/rfc4648)
- **Gzip Compression:** [RFC 1952](https://datatracker.ietf.org/doc/html/rfc1952)

---

**Document Version:** 2.0
**Last Updated:** 2026-02-19
**TcpRest Version:** 2.0+ (V2-only, V1 removed)
