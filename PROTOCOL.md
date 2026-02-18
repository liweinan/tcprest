# TcpRest Protocol Specification

This document explains the TcpRest wire protocol formats for both Protocol v1 (legacy) and Protocol v2 (current).

## Design Philosophy

TcpRest uses a simple, text-based protocol that is:
- **Human-readable**: Debugging with `tcpdump` or `wireshark` is straightforward
- **Language-agnostic**: Any language can implement a client/server
- **Backward-compatible**: New protocol versions work with old clients
- **Efficient**: Base64 encoding with optional compression

---

## Protocol v1 (Legacy)

### Request Format

```
[COMPRESSION]|ClassName/methodName(PARAMS)
```

**Components:**
- `COMPRESSION`: `0` (uncompressed) or `1` (gzip)
- `ClassName`: Fully qualified class name (e.g., `com.example.Calculator`)
- `methodName`: Method name (e.g., `add`)
- `PARAMS`: Base64-encoded parameters separated by `:::`

**Example:**
```
0|com.example.Calculator/add({{NQ==}}:::{{Mw==}})
```

Decoded: Call `Calculator.add` with params `5` and `3`

### Response Format

```
[COMPRESSION]|{{BASE64_RESULT}}
```

**Example:**
```
0|{{OA==}}
```

Decoded: Result is `8`

### Limitations

1. **No method overloading support**: Only method name is sent, not parameter types
   - If a class has `add(int, int)` and `add(double, double)`, only the first match is called
   - The extractor stops at the first name match (breaking other overloads)

2. **No exception propagation**: Exceptions are swallowed and return `NullObj`
   - Server catches `InvocationTargetException` and returns null instead of error details
   - Clients cannot distinguish between null result and server error

3. **No status codes**: Success and failure look identical
   - Empty response could mean null, error, or network issue

---

## Protocol v2 (Current)

Protocol v2 solves v1's limitations by adding **method signatures**, **status codes**, and **intelligent mapper support**.

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
  - Special markers: `NULL` for null, `EMPTY` for empty string

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
- `null` → `NULL` marker (special marker, not Base64-encoded)
- Empty string `""` → `EMPTY` marker (to distinguish from absent parameter)
- Primitives → `toString()` then Base64
- Arrays → `Arrays.toString()` format then Base64 (e.g., `[1, 2, 3]`)
- Objects → `toString()` then Base64

**Examples:**

| Java Value | Encoded in Array | Decoded |
|------------|------------------|---------|
| `5` | `NQ==` | `"5"` |
| `null` | `NULL` | `null` |
| `""` | `EMPTY` | `""` |
| `"hello"` | `aGVsbG8=` | `"hello"` |
| `new int[]{1, 2, 3}` | `WzEsIDIsIDNd` | `"[1, 2, 3]"` |

**Full Request Example:**
```
V2|0|{{Y24uZXhhbXBsZS5UZXN0L21ldGhvZChMamF2YS9sYW5nL1N0cmluZztJTGphdmEvbGFuZy9TdHJpbmc7KQ==}}|[aGVsbG8=,NULL,d29ybGQ=]
```
Calls: `Test.method(String, int, String)` with params `("hello", null, "world")`

### V2 Intelligent Mapper Support

Protocol V2 includes an intelligent 3-tier type mapping system that automatically handles complex objects without requiring manual mapper implementation for every class.

#### Mapper Priority System

When encoding/decoding parameters and return values, V2 uses the following priority:

**Priority 1: User-Defined Mappers (Highest)**
- Custom mappers registered via `server.addMapper()` or client `mappers` parameter
- Provides fine-grained control over serialization format
- Example: Using Gson for JSON serialization

**Priority 2: Automatic Serialization (Medium)**
- Any class implementing `java.io.Serializable` is automatically handled
- Uses Java's built-in serialization via `RawTypeMapper`
- No mapper registration required
- Supports `transient` fields (automatically excluded)

**Priority 3: Built-in Conversion (Lowest)**
- Primitives (`int`, `double`, `boolean`, etc.)
- Primitive wrappers (`Integer`, `Double`, `Boolean`, etc.)
- Strings
- Arrays (primitive and object arrays)

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
server.addMapper(User.class.getName(), new GsonUserMapper());

// Client side: Register custom mapper
Map<String, Mapper> mappers = new HashMap<>();
mappers.put(User.class.getName(), new GsonUserMapper());

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
| **Auto-Serialization** | Internal microservices, DTOs you control | Zero configuration, handles complex objects, supports transient | Binary format, larger size, Java-only |
| **Custom Mapper** | Public APIs, cross-language, human-readable wire format | Full control, readable format, efficient | Requires manual implementation |
| **Built-in** | Primitives, strings, simple types | Fast, minimal overhead | Limited to basic types |

#### Best Practices

1. **For new DTOs**: Implement `Serializable` first (zero effort)
   ```java
   public class MyDTO implements Serializable {
       private static final long serialVersionUID = 1L;
       // Fields...
   }
   ```

2. **For public APIs**: Use custom mappers with JSON
   ```java
   server.addMapper(MyDTO.class.getName(), new GsonMapper());
   ```

3. **For sensitive data**: Mark fields `transient` or use custom mapper
   ```java
   private transient String password;  // Not serialized
   ```

4. **For nested objects**: All nested objects must be Serializable or have mappers
   ```java
   public class Order implements Serializable {
       private User user;        // User must also be Serializable
       private Product product;  // Product must also be Serializable
   }
   ```

#### Mapper Resolution Algorithm

```java
// Encoding (client → server)
String encodeParam(Object param, Map<String, Mapper> mappers) {
    // 1. Check user-defined mapper
    if (mappers != null) {
        Mapper mapper = mappers.get(param.getClass().getName());
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
        Mapper mapper = mappers.get(paramType.getName());
        if (mapper != null) {
            return mapper.stringToObject(unbase64(paramStr));
        }
    }

    // 2. Check if Serializable (auto-deserialization)
    if (Serializable.class.isAssignableFrom(paramType) && ...) {
        RawTypeMapper rawMapper = new RawTypeMapper();
        return rawMapper.stringToObject(paramStr);
    }

    // 3. Use built-in conversion
    return convertPrimitive(unbase64(paramStr), paramType);
}
```

#### Complete Working Example

See test files for complete examples:
- **Auto-serialization**: `tcprest-singlethread/src/test/java/.../v2mapper/V2MapperDemoTest.java#testAutoSerialization`
- **Custom Gson mapper**: `tcprest-singlethread/src/test/java/.../v2mapper/V2MapperDemoTest.java#testCustomMapperWithGson`
- **Mixed types**: `tcprest-singlethread/src/test/java/.../v2mapper/V2MapperDemoTest.java#testMixedTypes`

---

## Version Detection & Compatibility

### Auto-Detection

The server automatically detects protocol version by checking the request prefix:

```java
if (request.startsWith("V2|")) {
    return processV2Request(request);
} else {
    return processV1Request(request);  // Default to v1
}
```

No handshake or negotiation is needed - the protocol version is self-describing.

### Server Modes

Servers can be configured with three protocol modes:

| Mode | Accepts V1 | Accepts V2 | Use Case |
|------|------------|------------|----------|
| `ProtocolVersion.V1` | ✅ | ❌ | Legacy systems only |
| `ProtocolVersion.V2` | ❌ | ✅ | New deployments |
| `ProtocolVersion.AUTO` | ✅ | ✅ | **Default** - mixed environments |

```java
// Server configuration
TcpRestServer server = new SingleThreadTcpRestServer(8080);
server.setProtocolVersion(ProtocolVersion.AUTO);  // Default: accept both
```

### Client Configuration

**As of version 2.0 (2026-02-19):** Clients default to V2 protocol.

```java
// V2 client (default - recommended)
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
);
// Uses ProtocolVersion.V2 by default

// V1 client (legacy - for backward compatibility)
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
);
factory.getProtocolConfig().setVersion(ProtocolVersion.V1);
```

### Compatibility Matrix

| Client | Server Mode | Result | Notes |
|--------|-------------|--------|-------|
| V1 | V1 only | ✅ Works | Legacy mode |
| V1 | AUTO | ✅ Works | Server accepts v1 requests |
| V1 | V2 only | ❌ Protocol error | Server rejects v1 format |
| V2 | V1 only | ❌ Protocol error | Server doesn't understand V2| prefix |
| V2 | AUTO | ✅ Works | Server accepts v2 requests |
| V2 | V2 only | ✅ Works | Recommended for new systems |

### Migration Strategy

**Current State (Version 2.0+):**
- Clients default to V2 protocol
- Servers default to AUTO mode (accept both V1 and V2)

**For New Projects:**
- Use defaults (V2 client + AUTO server)
- No configuration needed

**For Upgrading from Version 1.x:**

**Phase 1: Update Dependencies**
- Upgrade tcprest libraries to 2.0+
- Servers automatically use `ProtocolVersion.AUTO` (accepts both V1 and V2)

**Phase 2: Identify Legacy Clients**
- New clients will use V2 by default
- Identify any old clients still using V1 protocol

**Phase 3: Update Legacy Clients**
- Option A: Upgrade to tcprest 2.0+ (uses V2 by default)
- Option B: Keep old version but explicitly set V1:
  ```java
  factory.getProtocolConfig().setVersion(ProtocolVersion.V1);
  ```

**Phase 4: (Optional) V2-Only Mode**
- Once all clients upgraded to V2, switch servers to `ProtocolVersion.V2`
- Reject legacy V1 clients

**Rollback:**
- If issues occur, explicitly set clients to V1:
  ```java
  factory.getProtocolConfig().setVersion(ProtocolVersion.V1);
  ```

---

## Exception Handling

### V1 Exception Handling

**Server Behavior:**
```java
try {
    Object result = method.invoke(instance, params);
    return encodeResult(result);
} catch (InvocationTargetException e) {
    return encodeResult(NullObj);  // Swallows exception!
}
```

**Client Receives:** Null value (cannot distinguish from legitimate null)

### V2 Exception Handling

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
**TcpRest Version:** 2.0+ (V2 protocol simplified, default changed to V2)
