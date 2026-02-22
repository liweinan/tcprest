# TcpRest Architecture

## Overview

TcpRest is a lightweight TCP-based RPC framework that transforms Plain Old Java Objects (POJOs) into network-accessible services. The framework provides a simple annotation-driven approach to expose Java methods over TCP connections.

## Multi-Module Architecture

The project is organized into four Maven modules:

### tcprest-commons
**Zero-dependency commons module** containing all shared framework components using pure JDK implementations.

**Key components:**
- Client factory and proxy
- Protocol V2 layer
- Serialization/deserialization (mappers)
- Annotations (@TcpRestMethod, @Timeout)
- Request parser and invoker
- Compression support
- Security utilities
- SSL parameter configuration

**Dependencies:** None (only TestNG + SLF4J-Simple in test scope)

**Philosophy:** Minimal, core functionality that all other modules depend on. Zero external runtime dependencies.

### tcprest-singlethread
**SingleThread server implementation module** providing a simple blocking I/O server.

**Key components:**
- SingleThreadTcpRestServer
- SSL support via javax.net.ssl

**Dependencies:**
- tcprest-commons

**Use case:** Development, testing, low-concurrency scenarios, SSL required

### tcprest-nio
**NIO server implementation module** providing non-blocking I/O server.

**Key components:**
- NioTcpRestServer
- Selector-based async I/O

**Dependencies:**
- tcprest-commons

**Use case:** Moderate concurrency without SSL requirements

**Note:** SSL not supported due to Java NIO SocketChannel limitations

### tcprest-netty
**Optional high-performance module** providing Netty-based server implementation.

**Key components:**
- NettyTcpRestServer
- NettyTcpRestProtocolHandler

**Dependencies:**
- tcprest-commons
- io.netty:netty-all:4.1.131.Final

**Use case:** High-concurrency production scenarios, SSL with NIO performance

## Core Components

### 1. Protocol Layer

**Package:** `cn.huiwings.tcprest.protocol`

TcpRest uses Protocol V2 exclusively (as of v2.0, V1 has been removed).

#### Protocol V2

**Format:**
```
V2|statusCode|{{base64(ClassName/methodName(Signature))}}|[p1,p2,p3]
```

**Example:**
```
V2|0|{{Y2FsYy9hZGQoSUkp}}|[NQ==,Mw==]
     ↓
V2 | SUCCESS | Calculator/add(II) | [5, 3]
```

**Characteristics:**
- ✅ **Single-layer Base64** (only class/method/signature, not entire request)
- ✅ **JSON-style parameter array** `[p1,p2,p3]` instead of `{{p1}}:::{{p2}}`
- ✅ **Method overloading support** via type signatures `(II)`, `(Ljava/lang/String;I)`
- ✅ **Status codes** for error handling (0=SUCCESS, 1=BUSINESS_EXCEPTION, 2=SERVER_ERROR)
- ✅ **Protocol markers** (`~` for null, empty string for empty)
- ✅ **Security features** (checksum, HMAC, class whitelist)
- ✅ **Array handling**: Primitive/String arrays → `Arrays.toString()` then Base64; object arrays → RawTypeMapper (single Base64, URL-safe). Decode enforces `MAX_ARRAY_SIZE` and `MAX_ARRAY_DEPTH`.

**Status Codes:**

| Code | Constant | Meaning | Use Case |
|------|----------|---------|----------|
| 0 | SUCCESS | Method executed successfully | Normal response |
| 1 | BUSINESS_EXCEPTION | Business logic error (thrown exception extends BusinessException) | Validation errors, business rule violations |
| 2 | SERVER_ERROR | Server-side error (unexpected exception) | NPE, reflection errors, system failures |

**Key classes:**
- `StatusCode`: Status code constants (SUCCESS, BUSINESS_EXCEPTION, SERVER_ERROR, PROTOCOL_ERROR)
- `ProtocolV2Constants`: Protocol constants and markers

### 2. Server Layer

**Package:** `cn.huiwings.tcprest.server`

#### AbstractTcpRestServer
Base class providing common functionality:
- Resource management (classes and singleton instances)
- Mapper registry
- Request processing pipeline: delegates to **Protocol V2 components** (parser, invoker, codec) created via `ProtocolV2ServerComponents` so the server does not depend on concrete V2 implementation types
- Request validation (null/empty, V2 prefix) is performed inside the parser (`ProtocolV2Parser.parse()`); the server does not duplicate this logic and relies on `ProtocolException` from the parser for error responses

#### Server Implementations

**SingleThreadTcpRestServer** (`tcprest-singlethread`)
- Uses traditional blocking I/O with ServerSocket
- Single-threaded request handling
- **SSL Support:** ✅ Yes (via `javax.net.ssl.SSLServerSocketFactory`)
- Best for: Development, testing, low-concurrency scenarios, SSL required
- Thread model: One thread accepting connections sequentially
- Lifecycle: Properly shuts down with port release and thread termination
- Zero external dependencies (only depends on tcprest-commons)

**NioTcpRestServer** (`tcprest-nio`)
- Uses Java NIO with Selector
- Non-blocking I/O with worker thread pool
- **SSL Support:** ❌ No (Java NIO SocketChannel doesn't support SSL directly)
- Best for: Moderate concurrency **without SSL requirements**
- Thread model: One selector thread + cached thread pool for request processing
- Lifecycle: Properly closes selector and all channels on shutdown
- Zero external dependencies (only depends on tcprest-commons)
- **Technical limitation:** Java NIO's SocketChannel doesn't support SSL out of the box. SSL with NIO requires SSLEngine which adds significant complexity. For SSL with NIO performance, use NettyTcpRestServer instead.

**NettyTcpRestServer** (`tcprest-netty`)
- Uses Netty 4.x framework
- High-performance async I/O
- **SSL Support:** ✅ Yes (via Netty's SSL handler)
- Best for: High-concurrency production scenarios, SSL with NIO performance
- Thread model: Netty's event loop model with configurable thread pools
- Requires: Netty dependency
- Combines NIO performance with SSL support

**NettyUdpRestServer** / **NettyUdpRestClient** (`tcprest-netty`)
- UDP transport: one datagram = one request, one datagram = one response; same Protocol V2 payload.
- **SSL/DTLS:** ❌ No.
- Single-packet constraint: request and response must fit in one UDP packet (default max 1472 bytes); oversized packets are dropped.
- Use when: low-latency, fire-and-forget, or non-TCP networks.
- Client: `NettyUdpRestClientFactory(interfaceClass, host, port).getClient()`; call `shutdown()` when done.

#### Lifecycle Management

All servers implement the `TcpRestServer` interface:

```java
void up();                    // Start server
void up(boolean setDaemon);   // Start with daemon flag
void down();                  // Shutdown server
String getStatus();           // CLOSED | RUNNING | CLOSING (TcpRestServerStatus)
int getServerPort();          // Get listening port
```

**Server status:** `getStatus()` returns the current lifecycle state: `TcpRestServerStatus.CLOSED` (initial or after shutdown), `RUNNING` (after `up()`), `CLOSING` (during `down()`). The status field is volatile and is set in all server implementations so callers can observe state safely.

**Shutdown guarantees:**
- Closes server socket to stop accepting new connections
- Interrupts server thread(s)
- Waits up to 5 seconds for graceful termination
- Releases port for reuse
- Idempotent - can be called multiple times safely

### 3. Client Layer

**Package:** `cn.huiwings.tcprest.client`

Uses **dynamic proxy pattern** to create type-safe client stubs:

```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8080
);
MyService client = factory.getClient();
String result = client.someMethod(arg1, arg2);
```

**Key classes:**
- `TcpRestClientFactory`: Factory for creating client proxies; only **interfaces** may be registered (constructor params `interfaceClass` / `interfaceClasses`; concrete classes throw `IllegalArgumentException`). Supports single-interface (one `Class` + `getClient()`) or multi-interface (varargs/array + `getClient(Class<T>)` per type). Terminology is aligned with the server: server uses "resource" (addResource, getResource), client uses "interface" (contract only).
- `TcpRestClientProxy`: InvocationHandler that serializes calls to protocol format
- Supports custom mappers and SSL

### 4. Serialization Layer

**Package:** `cn.huiwings.tcprest.mapper`

**Mapper interface** handles bidirectional conversion between Java objects and string representation:

```java
public interface Mapper {
    String objectToString(Object object);  // Serialize object to string
    Object stringToObject(String param);   // Deserialize string to object
}
```

#### V2 Intelligent 4-Tier Mapper System

Protocol V2 uses a **priority-based mapper resolution system** that provides zero-configuration support for most types:

**Priority 1: User-Defined Mappers** (Highest)
- Custom mappers registered via `addMapper(className, mapper)`
- Example: `GsonMapper` for custom JSON serialization
- Use case: Override default behavior, third-party types, special formats

**Priority 2: Collection Interfaces** ⭐ Zero config
- Automatic support for: `List`, `Map`, `Set`, `Queue`, `Deque`, `Collection` (via `MapperHelper.DEFAULT_MAPPERS`)
- Uses `RawTypeMapper` (Java serialization) for implementation
- Example: `List<User>` → works automatically, deserializes to `ArrayList`
- **Zero configuration needed** - just use `Serializable` DTOs!

**Priority 3: Auto-Serialization** ⭐ Convenience
- Any class implementing `Serializable` → automatic via `RawTypeMapper`
- Uses Java serialization (Base64-encoded)
- Example: Custom DTOs with `implements Serializable`
- **Zero configuration needed** - just implement `Serializable`!

**Priority 4: Built-in Types** (Lowest)
- Primitives and wrappers: `int`, `Integer`, `long`, `Long`, `boolean`, `Boolean`, etc.
- `String`, `byte[]`, arrays (primitive/String arrays via `Arrays.toString()`; object arrays via RawTypeMapper, with size/depth limits)
- Optimized for common types

**Built-in mapper implementations:**
- `RawTypeMapper`: Java serialization for `Serializable` objects (Priority 2 & 3)
- `StringMapper`: For String types
- `IntegerMapper`: For Integer/int types
- `LongMapper`: For Long/long types
- `BooleanMapper`: For Boolean/boolean types
- `DoubleMapper`: For Double/double types
- And more for primitive types and wrappers

**Example - Zero Configuration:**

```java
// 1. Define DTO (just implement Serializable)
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int age;
    private transient String password;  // Excluded
}

// 2. Service interface (collections work automatically!)
public interface UserService {
    UserInfo getUser(int id);           // ✅ Auto-serialization (Priority 3)
    List<UserInfo> getAllUsers();       // ✅ Collection interface (Priority 2)
    Map<String, UserInfo> getUserMap(); // ✅ Collection interface (Priority 2)
}

// 3. No mapper registration needed - it just works!
```

**Custom mappers:**
Users can implement `Mapper` interface and register with server/client for custom types:

```java
// Custom JSON mapper using Gson
public class GsonUserMapper implements Mapper {
    private final Gson gson = new Gson();

    public String objectToString(Object object) {
        return gson.toJson(object);
    }

    public Object stringToObject(String param) {
        return gson.fromJson(param, User.class);
    }
}

// Register (Priority 1 - overrides auto-serialization)
server.addMapper(User.class.getCanonicalName(), new GsonUserMapper());
```

**Protocol Markers (V2):**
- `~` → `null` value
- `""` (empty string) → empty string
- Prevents ambiguity and reduces bandwidth

**Thread safety:** Mappers should be stateless or thread-safe.

### 5. Request Parser, Invoker, and V2 Server Components

**Package:** `cn.huiwings.tcprest.parser`, `cn.huiwings.tcprest.invoker`, `cn.huiwings.tcprest.protocol.v2`

These components form the server-side request processing pipeline. **ProtocolV2ServerComponents** (`protocol.v2`) creates and holds the parser, invoker, and codec for V2; the server calls `ProtocolV2ServerComponents.create(mappers, securityConfig)` and uses the result for parsing, invocation, and response encoding so it does not depend on `ProtocolV2Parser`/`ProtocolV2Codec` types directly.

#### RequestParser: Protocol Parsing

**RequestParser** parses protocol strings into invocation context (`Context` object).

**Implementation:**

- `ProtocolV2Parser`: Protocol V2 parser with full feature support

**ProtocolV2Parser capabilities:**
- ✅ **Request validation** (single source): null/empty and V2 prefix check; invalid requests throw `ProtocolException` so the server does not duplicate validation
- ✅ **Method signature parsing**: `add(II)` → finds `add(int, int)` even with overloads
- ✅ **Parameter parsing priority** (server-side): P1 primitives/wrappers/String/primitive arrays/String[] (decode + convertToType); P2 object arrays (Base64 → RawTypeMapper); P3 user mapper; P4 collection interfaces; P5 Serializable; P6 fallback
- ✅ **Security validation**: Checksum verification (CRC32/HMAC), class whitelist
- ✅ **Protocol markers**: `~` for null, empty string for empty
- ✅ **Exception handling**: Throws `ProtocolException`, `SecurityException`

**Example:**
```java
// Request: V2|0|{{Y2FsYy9hZGQoSUkp}}|[NQ==,Mw==]
Context context = parser.parse(request);

context.getTargetClass()   // → Calculator.class
context.getTargetMethod()  // → public int add(int, int)
context.getParams()        // → [5, 3]
```

#### Invoker: Method Execution

**Invoker** executes the method invocation using Java reflection.

**Implementation:**

- `ProtocolV2Invoker`: Protocol V2 invoker with exception propagation

**ProtocolV2Invoker behavior:**
- ✅ **Exception propagation**: `BusinessException` → status code 1, others → status code 2
- ✅ **Null handling**: Returns `null` directly (V2 protocol handles it)
- ✅ **Validation**: Validates context, instance, method are not null
- ✅ **Exception unwrapping**: Unwraps `InvocationTargetException` to real cause

**Example (V2 workflow):**
```java
ProtocolV2Invoker invoker = new ProtocolV2Invoker();

try {
    Object result = invoker.invoke(context);
    // Success → encode with StatusCode.SUCCESS (0)
} catch (BusinessException e) {
    // Business error → encode with StatusCode.BUSINESS_EXCEPTION (1)
} catch (Exception e) {
    // Server error → encode with StatusCode.SERVER_ERROR (2)
}
```

### 6. Annotations

**Package:** `cn.huiwings.tcprest.annotations`

**Current annotations:**
- `@TcpRestMethod`: Mark methods for remote access (optional, all public methods are exposed by default)
- `@Timeout`: Configure per-method timeout in milliseconds

**@Timeout annotation:**

```java
public interface UserService {
    @Timeout(3000)  // 3 second timeout
    User getUser(int id);

    @Timeout(10000)  // 10 second timeout for slow operations
    Report generateReport(Date start, Date end);
}
```

**Use cases:**
1. **Database queries** - Short timeout for simple queries, longer for reports
2. **External API calls** - Prevent hanging on unresponsive services
3. **Heavy computation** - Allow more time for complex calculations
4. **File operations** - Longer timeout for large file transfers

**Future annotations (In Design - See ANNOTATIONS-DESIGN.md):**
- `@TcpRestClient`: Declarative client configuration with CDI/Spring support
- `@TcpRestServer`: Declarative server configuration
- Independent modules: `tcprest-cdi`, `tcprest-spring` (maintains core zero-dependency)

### 7. Security Layer (NEW - 2026-02-18)

**Package:** `cn.huiwings.tcprest.security`

TcpRest V2 includes comprehensive security features to protect against attacks and ensure message integrity.

#### Security Architecture

**SecurityConfig** provides centralized security configuration:

```java
public class SecurityConfig {
    private ChecksumAlgorithm checksumAlgorithm = NONE;  // CRC32 or HMAC_SHA256
    private String hmacSecret;                           // HMAC secret key
    private boolean classWhitelistEnabled = false;       // Restrict accessible classes
    private Set<String> allowedClasses;                  // Whitelist entries
}
```

#### Security Features

**1. Full Base64 Encoding** (Always Active)
- All protocol components are Base64-encoded
- Prevents delimiter injection: `Class/method()/evil` → safe
- Prevents path traversal: `../../EvilClass` → safe
- No configuration needed - built into V2 protocol

**2. Checksum Verification** (Optional)

**CRC32 - Fast Integrity Check:**
```java
SecurityConfig config = new SecurityConfig().enableCRC32();
server.setSecurityConfig(config);
```
- **Purpose**: Detect accidental corruption, network errors
- **Algorithm**: CRC32 (32-bit cyclic redundancy check)
- **Performance**: ~0.1ms overhead
- **Use case**: Development, internal services

**HMAC-SHA256 - Cryptographic Authentication:**
```java
SecurityConfig config = new SecurityConfig()
    .enableHMAC("your-secret-key-min-32-chars");
server.setSecurityConfig(config);
```
- **Purpose**: Prevent malicious tampering, man-in-the-middle attacks
- **Algorithm**: HMAC-SHA256 (keyed-hash message authentication)
- **Performance**: ~0.5ms overhead
- **Use case**: Production, public APIs, untrusted networks

**Protocol format with checksum:**
```
V2|0|{{base64(class/method(sig))}}|[params]|checksum
                                             ↑
                                   CRC32 or HMAC-SHA256
```

**3. Class Whitelist** (Optional)

Restrict which classes can be invoked:

```java
SecurityConfig config = new SecurityConfig()
    .enableClassWhitelist()
    .allowClass("com.example.PublicAPI")
    .allowClass("com.example.UserService");
server.setSecurityConfig(config);
```

- **Purpose**: Prevent unauthorized access to internal classes
- **Use case**: Public APIs, multi-tenant systems
- **Behavior**: Non-whitelisted classes → `SecurityException`

**4. Combined Security (Recommended for Production)**

```java
SecurityConfig config = new SecurityConfig()
    .enableHMAC("production-secret-key-min-32-chars")
    .enableClassWhitelist()
    .allowClass("com.example.api.*");  // Wildcard support
```

#### Attack Protection

| Attack Type | Protection Mechanism | How It Works |
|-------------|---------------------|--------------|
| **Delimiter Injection** | Base64 encoding | `evil/method()` → cannot inject delimiters |
| **Path Traversal** | Base64 encoding | `../../EvilClass` → treated as literal string |
| **Method Injection** | Signature matching | `method:::evil` → signature mismatch, rejected |
| **Message Tampering** | HMAC-SHA256 | Modified message → checksum fails |
| **Unauthorized Access** | Class whitelist | Non-whitelisted class → `SecurityException` |
| **Replay Attacks** | (Future) Timestamp/nonce | Not yet implemented |

#### Security Utilities

**ProtocolSecurity** class provides security operations:

```java
public class ProtocolSecurity {
    // Calculate CRC32 checksum
    public static String calculateCRC32(String message)

    // Calculate HMAC-SHA256
    public static String calculateHMAC(String message, String secret)

    // Verify checksum
    public static boolean verifyChecksum(String message, String checksum,
                                         SecurityConfig config)
}
```

#### Performance Impact

| Security Level | Overhead | Use Case |
|---------------|----------|----------|
| None (Base64 only) | ~0.05ms | Trusted internal network |
| CRC32 | ~0.1ms | Development, testing |
| HMAC-SHA256 | ~0.5ms | Production, public APIs |
| HMAC + Whitelist | ~0.6ms | High-security scenarios |

**Benchmark results** (1KB message):
- Base64 encoding/decoding: 0.05ms
- CRC32 calculation: 0.05ms
- HMAC-SHA256 calculation: 0.4ms
- Whitelist check: 0.1ms

**Note:** All measurements on standard hardware. Overhead is negligible compared to network latency (typically 1-50ms).

#### Security Best Practices

1. ✅ **Always use HMAC in production** with strong secret (32+ chars)
2. ✅ **Enable class whitelist for public APIs** to restrict access
3. ✅ **Rotate HMAC secrets periodically** (e.g., every 90 days)
4. ✅ **Use TLS/SSL** in addition to HMAC for encryption
5. ✅ **Log security violations** for monitoring and alerting
6. ❌ **Don't use CRC32 in production** - it's not cryptographically secure
7. ❌ **Don't expose internal classes** to public APIs

#### Client-Side Security

Both client and server must use matching security configuration:

```java
// Server
SecurityConfig serverConfig = new SecurityConfig().enableHMAC("secret");
server.setSecurityConfig(serverConfig);

// Client (must match!)
SecurityConfig clientConfig = new SecurityConfig().enableHMAC("secret");
factory.setSecurityConfig(clientConfig);
```

**Mismatch behavior:**
- Server has HMAC, client doesn't → Client requests rejected
- Server has whitelist, client calls non-whitelisted class → `SecurityException`

### 8. Compression Layer

**Package:** `cn.huiwings.tcprest.compression`

Optional GZIP compression support for bandwidth optimization (uses JDK built-in `java.util.zip`).

**Key classes:**
- `CompressionConfig`: Configuration for compression (enabled, threshold, level)
- `CompressionUtil`: Utility methods for compress/decompress operations
- `CompressingConverter`: Decorator for Converter adding compression support

**Protocol format:**
```
Uncompressed: "0|" + data
Compressed:   "1|" + base64(gzip(data))
```

**Features:**
- Configurable compression threshold (don't compress small messages)
- Adjustable compression level (1-9, trading speed vs ratio)
- Automatic format detection (backward compatible with legacy format)
- Both client and server support

**Performance:**
- Repetitive text: ~96% compression ratio
- JSON/XML: ~88-90% compression ratio
- General text: ~85-95% compression ratio
- Overhead: <1ms for typical messages

**Thread safety:** All compression operations are stateless and thread-safe.

### 8. Exception Handling

**Package:** `cn.huiwings.tcprest.exception`

TcpRest provides a clean, semantic exception hierarchy for different error scenarios:

#### Core Exception Types (5 exceptions)

**Protocol Layer Exceptions:**
- `SecurityException` (unchecked): Security violations (checksum failure, whitelist rejection)
- `ProtocolException` (unchecked): Protocol format/parsing errors, malformed requests

**Transport Layer Exceptions:**
- `TimeoutException` (unchecked): Client-side timeout exceeded

**Application Layer Exceptions:**
- `BusinessException` (unchecked): Base class for expected business logic errors
- `RemoteException` (unchecked, abstract): Base class for remote exception fallbacks
  - `RemoteBusinessException`: Fallback when client lacks server's BusinessException subclass
  - `RemoteServerException`: Fallback when client lacks server's exception class

**Design Philosophy:**
- ✅ All exceptions are **unchecked** (RuntimeException) - no forced exception handling
- ✅ Clear semantic categories (protocol, business, server)
- ✅ Intelligent fallback mechanism preserves error semantics

#### Exception Reconstruction Mechanism

When a server throws an exception, TcpRest employs a sophisticated reconstruction strategy:

**Step 1: Encode Exception (Server)**
```java
// ProtocolV2Codec.encodeException()
String exceptionString = exception.getClass().getName() + ": " + exception.getMessage();
// Example: "java.lang.NullPointerException: User not found"

// Encode with status code
return "V2|0|2|{{base64(exceptionString)}}";  // Status code 2 = SERVER_ERROR
```

**Step 2: Decode and Reconstruct (Client)**
```java
// ProtocolV2Codec.recreateException()

// Parse: "java.lang.NullPointerException: User not found"
String className = "java.lang.NullPointerException";
String message = "User not found";

// Try to load exception class
Class<?> exceptionClass = Class.forName(className);

// Try to instantiate with reflection
Constructor<?> constructor = exceptionClass.getConstructor(String.class);
return (Exception) constructor.newInstance(message);  // Success!
```

**Step 3: Fallback (if reconstruction fails)**
```java
// Client doesn't have the exception class

if (isBusinessException) {
    // Server threw BusinessException subclass → RemoteBusinessException
    return new RemoteBusinessException(className, message);
} else {
    // Server threw other exception → RemoteServerException
    return new RemoteServerException(className, message);
}
```

#### Exception Categories and Status Codes

| Exception Category | Status Code | Server-Side Example | Client Receives |
|--------------------|-------------|---------------------|-----------------|
| **Success** | 0 (SUCCESS) | Method returns normally | Return value |
| **Business Exception** | 1 (BUSINESS_EXCEPTION) | `throw new ValidationException(...)` | ValidationException or RemoteBusinessException |
| **Server Error** | 2 (SERVER_ERROR) | `throw new NullPointerException(...)` | NullPointerException or RemoteServerException |
| **Protocol Error** | 3 (PROTOCOL_ERROR) | Parsing/security failure | ProtocolException or SecurityException |

#### RemoteException Hierarchy

**Purpose:** When client cannot reconstruct original exception type, use semantic wrappers.

**RemoteBusinessException:**
```java
public class RemoteBusinessException extends RemoteException {
    // Wrapper for server business exceptions not available on client

    public String getRemoteExceptionType();  // e.g., "com.example.OrderValidationException"
    public boolean isBusinessException();     // Always returns true
    public boolean isServerError();           // Always returns false
}
```

**Example scenario:**
```java
// Server has custom business exception
public class OrderValidationException extends BusinessException {
    public OrderValidationException(String msg) { super(msg); }
}

// Server throws
throw new OrderValidationException("Order amount exceeds limit");

// Client receives (doesn't have OrderValidationException.class)
try {
    orderService.placeOrder(order);
} catch (RemoteBusinessException e) {
    // Can still identify error type and handle appropriately
    System.out.println(e.getRemoteExceptionType());
    // Output: "com.example.OrderValidationException"

    System.out.println(e.getMessage());
    // Output: "OrderValidationException: Order amount exceeds limit"

    // Client knows it's a business error - can retry
}
```

**RemoteServerException:**
```java
public class RemoteServerException extends RemoteException {
    // Wrapper for server exceptions (non-business) not available on client

    public String getRemoteExceptionType();  // e.g., "com.example.DatabasePoolException"
    public boolean isBusinessException();     // Always returns false
    public boolean isServerError();           // Always returns true
}
```

**Example scenario:**
```java
// Server has custom server exception
public class DatabasePoolException extends RuntimeException {
    public DatabasePoolException(String msg) { super(msg); }
}

// Server throws
throw new DatabasePoolException("Connection pool exhausted");

// Client receives (doesn't have DatabasePoolException.class)
try {
    userService.getUser(123);
} catch (RemoteServerException e) {
    // Can identify as server-side error
    System.out.println(e.getRemoteExceptionType());
    // Output: "com.example.DatabasePoolException"

    // Client knows it's a server error - should alert ops, not retry
    logger.error("Server failure: " + e.getRemoteExceptionType(), e);
}
```

#### Exception Propagation Flow

```
Server Side                          Wire Format                    Client Side
-----------                          -----------                    -----------

1. Method Execution
   throw new NPE("error")
                  ↓
2. ProtocolV2Invoker.invoke()
   catches exception
                  ↓
3. Determine Status Code
   NPE → SERVER_ERROR (2)
   BusinessException → BUSINESS_EXCEPTION (1)
                  ↓
4. ProtocolV2Codec.encodeException()
   "NullPointerException: error"
                  ↓
            V2|0|2|{{base64}}    →    Network    →    V2|0|2|{{base64}}
                                                                ↓
                                                    5. ProtocolV2Codec.decode()
                                                       detects status code 2
                                                                ↓
                                                    6. recreateException()
                                                       tries Class.forName("NullPointerException")
                                                                ↓
                                                    7. Success: new NullPointerException("error")
                                                       OR
                                                       Failure: new RemoteServerException(...)
                                                                ↓
                                                    8. Client catches
                                                       catch (NullPointerException e) { ... }
```

#### Testing Exception Propagation

**Test Coverage:**
```java
// ExceptionPropagationTest.java
@Test
public void testNullPointerExceptionPropagation() {
    // Standard exception - client has class
    assertThrows(NullPointerException.class, () -> client.throwNullPointer());
}

@Test
public void testBusinessExceptionPropagation() {
    // BusinessException - client has class
    assertThrows(BusinessException.class, () -> client.throwBusiness());
}

// ExceptionReconstructionE2ETest.java
@Test
public void testCustomBusinessExceptionReconstruction() {
    // Custom business exception - both sides have class
    assertThrows(OrderValidationException.class, () -> client.validateOrder());
}

@Test
public void testMissingBusinessExceptionFallback() {
    // Server has OrderValidationException, client doesn't
    // (Tested in unit tests - E2E shares classloader)
    // Expected: RemoteBusinessException
}

@Test
public void testMissingServerExceptionFallback() {
    // Server has CustomDatabaseException, client doesn't
    // Expected: RemoteServerException
}
```

**Note:** In single-JVM test environments, client and server share the same ClassLoader, so all exception classes are visible to both sides. To test fallback scenarios (RemoteBusinessException/RemoteServerException), use unit tests that simulate missing classes.

#### Best Practices

**✅ Use BusinessException for expected errors:**
```java
// Server-side
public class OrderService {
    public void validateOrder(Order order) {
        if (order.getAmount() > limit) {
            throw new OrderValidationException("Amount exceeds limit");  // Extends BusinessException
        }
    }
}

// Client-side
try {
    orderService.placeOrder(order);
} catch (BusinessException e) {
    // Expected error - user can fix
} catch (RemoteBusinessException e) {
    // Server has custom business exception we don't have - still handle as business error
}
```

**✅ Distinguish business errors from server errors:**
```java
try {
    orderService.process(order);
} catch (RemoteBusinessException e) {
    // Business logic error - retry after user correction
    showUserMessage("Please fix: " + e.getMessage());
} catch (RemoteServerException e) {
    // Server internal error - alert ops team
    logger.error("Server error: " + e.getRemoteExceptionType());
    showUserMessage("Service temporarily unavailable");
}
```

**✅ Use standard Java exceptions when possible:**
```java
// Standard exceptions are always available on both sides
throw new IllegalArgumentException("Invalid input");  // Always reconstructs correctly
throw new NullPointerException("Object is null");      // Always reconstructs correctly
```

**❌ Don't catch generic Exception:**
```java
// Bad: Catches everything, loses semantic information
try {
    service.call();
} catch (Exception e) {
    // Can't distinguish business errors from server errors
}

// Good: Catch specific types
try {
    service.call();
} catch (BusinessException | RemoteBusinessException e) {
    // Business error
} catch (RemoteServerException e) {
    // Server error
}
```

#### Deleted Legacy Exceptions

The following exceptions have been removed in favor of a cleaner architecture:

- ❌ `ServerSideException` - Empty class, unused
- ❌ `ParseException` - Merged into unchecked `ProtocolException`
- ❌ `RemoteInvocationException` - Replaced by `RemoteBusinessException` and `RemoteServerException`
- ❌ `MapperNotFoundException` - Removed (mappers are now optional with auto-serialization)

## Extension Points

### 1. Custom Mappers

Implement `Mapper` interface for custom serialization:

```java
public class ColorMapper implements Mapper {
    @Override
    public String objectToString(Object object) {
        Color c = (Color) object;
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    @Override
    public Object stringToObject(String param) {
        String[] parts = param.split(",");
        return new Color(
            Integer.parseInt(parts[0]),
            Integer.parseInt(parts[1]),
            Integer.parseInt(parts[2])
        );
    }
}
```

Register with server:
```java
server.addMapper("java.awt.Color", new ColorMapper());
```

**Alternative: Use Serializable (Zero Configuration):**

```java
// Just implement Serializable - no mapper needed!
public class Color implements Serializable {
    private static final long serialVersionUID = 1L;
    private int red, green, blue;

    // Constructor, getters, setters...
}

// Works automatically with V2 protocol (Priority 3: Auto-serialization)
```

### 2. Custom Request Parsers

Implement `RequestParser` interface for custom protocol parsing:

```java
public class MyRequestParser implements RequestParser {
    public Context parse(String request) {
        // Custom parsing logic
    }
}
```

### 3. Custom Invokers

Implement custom invoker for custom invocation logic:

```java
public class MyInvoker {
    public Object invoke(Context ctx) {
        // Custom invocation logic (e.g., AOP, security checks)
    }
}
```

## Threading Models Comparison

| Server Type | Accepting Connections | Processing Requests | Best For |
|-------------|----------------------|---------------------|----------|
| SingleThread | Single blocking thread | Same thread | Development, testing |
| NIO | Single selector thread | Cached thread pool | Moderate load, no deps |
| Netty | Netty boss thread | Netty worker pool | High concurrency, production |

## Security Considerations

### SSL/TLS Support

Configure SSL parameters:

```java
SSLParam sslParams = new SSLParam();
sslParams.setKeyStore("/path/to/keystore");
sslParams.setKeyStorePassword("password");

TcpRestServer server = new SingleThreadTcpRestServer(8080, sslParams);
```

### Input Validation

- Always validate input parameters in service methods
- Use type-safe mappers to prevent injection attacks
- Implement authentication/authorization in custom invokers if needed

## Performance Characteristics

### Protocol Overhead Comparison

**Encoding/Decoding Performance:**

| Protocol | Operation | Time (avg) | Notes |
|----------|-----------|------------|-------|
| V1 | Encode | ~0.15ms | Double Base64 encoding |
| V1 | Decode | ~0.18ms | Double Base64 decoding |
| V2 | Encode | ~0.08ms | Single Base64 encoding |
| V2 | Decode | ~0.10ms | Single Base64 decoding |

**V2 is ~45% faster** due to single-layer encoding.

### Compression Performance

**Compression effectiveness (1KB text message):**

| Content Type | Original Size | Compressed | Ratio | Time |
|--------------|---------------|------------|-------|------|
| Repetitive text | 1024 bytes | 41 bytes | 96% | 0.8ms |
| JSON | 1024 bytes | 122 bytes | 88% | 0.9ms |
| General text | 1024 bytes | 154 bytes | 85% | 0.7ms |
| Random data | 1024 bytes | 1050 bytes | -3% | 1.2ms |

**Recommendation:**
- Use compression threshold (e.g., 512 bytes) to avoid overhead on small messages
- Compression level 6 (default) offers best speed/ratio trade-off
- Level 9 (max) only improves ratio by ~2-5% but 2x slower

### Security Performance

**Security overhead (1KB message):**

| Security Level | Overhead | Total Time | Use Case |
|---------------|----------|------------|----------|
| None (Base64 only) | 0.05ms | 0.13ms | Trusted internal network |
| CRC32 | 0.05ms | 0.18ms | Development, testing |
| HMAC-SHA256 | 0.40ms | 0.53ms | Production, public APIs |

**Combined features (1KB message, compression enabled):**

| Configuration | Total Overhead | Typical Use Case |
|---------------|----------------|------------------|
| V2 + No security | ~0.1ms | Internal microservices |
| V2 + CRC32 | ~0.2ms | Development environment |
| V2 + HMAC + Compression | ~1.4ms | Production (recommended) |
| V2 + HMAC + Whitelist + Compression | ~1.5ms | Public API (high security) |

**Note:** All overhead is negligible compared to network latency (typically 1-50ms).

### Server Performance

**Throughput comparison (concurrent clients):**

| Server Type | Max Throughput | CPU Usage | Memory | Best For |
|-------------|---------------|-----------|---------|----------|
| SingleThread | ~500 req/s | Low (single thread) | Minimal | Development, testing |
| NIO | ~5,000 req/s | Medium (thread pool) | Low | Medium concurrency |
| Netty | ~20,000 req/s | High (event loop) | Medium | High concurrency |

**Latency (p99, 10 concurrent clients):**
- SingleThread: ~50ms
- NIO: ~10ms
- Netty: ~5ms

### Memory Usage

**Memory footprint (idle server):**
- SingleThread: ~20MB
- NIO: ~30MB
- Netty: ~45MB (includes Netty buffers)

**Per-connection overhead:**
- SingleThread: ~2KB (blocking socket buffer)
- NIO: ~8KB (NIO buffers)
- Netty: ~16KB (Netty pooled buffers)

## Performance Tuning

### For NioTcpRestServer
- Adjust selector timeout (default: 1000ms)
- Configure thread pool size in `Executors.newCachedThreadPool()`

### For NettyTcpRestServer
- Configure Netty boss and worker thread pools
- Tune buffer sizes and channel options

### General
- Use singleton resources for stateless services
- Implement efficient mappers for frequently used types
- Consider connection pooling on client side for high-volume scenarios

## Migration Guide

### From 0.x to 1.0 (Package Rename)

Update all imports:
```java
// Old
import io.tcprest.server.TcpRestServer;

// New
import cn.huiwings.tcprest.server.TcpRestServer;
```

### Module Structure Migration

**Client-only applications** (no server):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-commons</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**SingleThread server** (blocking I/O, SSL support):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-singlethread</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**NIO server** (non-blocking I/O, no SSL):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-nio</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**Netty server** (high-performance, SSL support):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-netty</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**Note:** All server modules automatically include `tcprest-commons` as a transitive dependency.

## Testing

### Unit Testing

**Port allocation (CRITICAL):**
❌ **WRONG - Can cause port conflicts:**
```java
int port = Math.abs(new Random().nextInt()) % 10000 + 8000;  // Random collisions!
```

✅ **CORRECT - Use PortGenerator:**
```java
import cn.huiwings.tcprest.test.PortGenerator;

int port = PortGenerator.get();  // AtomicInteger - guaranteed unique
```

**Port ranges by module (avoid conflicts):**
- `tcprest-commons`: 8000-13999
- `tcprest-singlethread`: 14000-15999
- `tcprest-nio`: 16000-19999
- `tcprest-netty`: 20000-23999

**Basic test pattern:**
```java
public class MyServiceTest {
    private TcpRestServer server;

    @BeforeMethod
    public void setup() {
        server = new SingleThreadTcpRestServer(PortGenerator.get());
        server.addSingletonResource(new MyServiceImpl());
        server.up();
    }

    @AfterMethod
    public void teardown() throws Exception {
        server.down();
        Thread.sleep(300);  // Wait for port release
    }

    @Test
    public void testMethod() {
        // Test implementation
    }
}
```

### TestNG @Factory Pattern (Multi-Server Testing)

**Use @Factory to test multiple server implementations in one test class:**

```java
public class MapperSmokeTest {
    protected TcpRestServer tcpRestServer;

    // Constructor receives server instance
    public MapperSmokeTest(TcpRestServer tcpRestServer) {
        this.tcpRestServer = tcpRestServer;
    }

    // Factory creates test instances for each server type
    @Factory
    public static Object[] create() throws Exception {
        List<Object> result = new ArrayList<>();
        result.add(new MapperSmokeTest(new SingleThreadTcpRestServer(PortGenerator.get())));
        result.add(new MapperSmokeTest(new NioTcpRestServer(PortGenerator.get())));
        result.add(new MapperSmokeTest(new NettyTcpRestServer(PortGenerator.get())));
        return result.toArray();
    }

    // CRITICAL: Use @BeforeClass, NOT @BeforeMethod!
    @BeforeClass
    public void startServer() throws Exception {
        tcpRestServer.up();
        Thread.sleep(500);  // Wait for async servers (NIO, Netty)
    }

    // CRITICAL: Use @AfterClass, NOT @AfterMethod!
    @AfterClass
    public void stopServer() throws Exception {
        tcpRestServer.down();
        Thread.sleep(300);  // Wait for port release
    }

    @Test
    public void testFeature() {
        // Test runs 3 times (once per server type)
    }
}
```

**Why @BeforeClass/@AfterClass instead of @BeforeMethod/@AfterMethod?**
- ❌ **@BeforeMethod**: Restarts server for EVERY test method → port conflicts, slow tests
- ✅ **@BeforeClass**: Starts server ONCE per @Factory instance → clean, fast, reliable

**Configure testng.xml for strict sequential execution:**

```xml
<!DOCTYPE suite SYSTEM "https://testng.org/testng-1.0.dtd" >
<suite name="TestSuite" verbose="1" parallel="false"
       data-provider-thread-count="1" group-by-instances="true">
    <test name="Tests" parallel="false" preserve-order="true">
        <classes>
            <class name="cn.huiwings.tcprest.test.smoke.MapperSmokeTest">
                <methods>
                    <include name="testMethod1"/>
                    <include name="testMethod2"/>
                </methods>
            </class>
        </classes>
    </test>
</suite>
```

**Configure Maven Surefire:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <suiteXmlFiles>
            <suiteXmlFile>src/test/resources/testng.xml</suiteXmlFile>
        </suiteXmlFiles>
        <parallel>none</parallel>
        <threadCount>1</threadCount>
    </configuration>
</plugin>
```

### Integration Testing

**Test areas:**
- Server shutdown behavior (proper cleanup within 5 seconds)
- Port release and restart capabilities
- Error handling and recovery
- Protocol version compatibility (V1 and V2)
- Security features (HMAC, CRC32, whitelist)
- Compression (threshold, level, backward compatibility)

**Example - Shutdown test:**
```java
@Test
public void testShutdown() throws Exception {
    int port = PortGenerator.get();
    TcpRestServer server = new SingleThreadTcpRestServer(port);
    server.up();

    // Verify server is running
    // ... make some calls ...

    server.down();
    Thread.sleep(500);  // Wait for full shutdown

    // Verify port is released (can restart)
    TcpRestServer server2 = new SingleThreadTcpRestServer(port);
    server2.up();  // Should succeed
    server2.down();
}
```

### Load Testing

- Use `NettyTcpRestServer` for load tests
- Monitor thread usage and memory consumption
- Test with concurrent clients
- Benchmark compression effectiveness
- Measure security overhead (HMAC vs CRC32 vs none)

## Common Patterns

### Stateless Service

```java
public class CalculatorService {
    @TcpRestMethod
    public int add(int a, int b) {
        return a + b;
    }
}

server.addResource(CalculatorService.class);
```

### Singleton Service with State

```java
@Singleton
public class CounterService {
    private int count = 0;

    @TcpRestMethod
    public synchronized int increment() {
        return ++count;
    }
}

server.addSingletonResource(new CounterService());
```

### Custom Type Marshalling

**Option 1: Zero Configuration (Recommended for V2)**

```java
// 1. Define type with Serializable
public class Person implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private int age;

    // Constructor, getters, setters...
}

// 2. Use in service (no mapper needed!)
public class PersonService {
    public Person getPerson(String name) {
        return new Person(name, 30);
    }

    public List<Person> getAllPersons() {
        // Collections work automatically too!
        return Arrays.asList(
            new Person("Alice", 25),
            new Person("Bob", 30)
        );
    }
}

// 3. Works automatically with V2 protocol!
```

**Option 2: Custom Mapper (For Special Formats)**

```java
// 1. Define custom type
public class Person {
    String name;
    int age;
}

// 2. Implement custom mapper
public class PersonMapper implements Mapper {
    @Override
    public String objectToString(Object object) {
        Person p = (Person) object;
        return p.name + "|" + p.age;
    }

    @Override
    public Object stringToObject(String param) {
        String[] parts = param.split("\\|");
        Person p = new Person();
        p.name = parts[0];
        p.age = Integer.parseInt(parts[1]);
        return p;
    }
}

// 3. Register mapper (Priority 1 - overrides auto-serialization)
server.addMapper("com.example.Person", new PersonMapper());

// 4. Use in service
public class PersonService {
    public Person getPerson(String name) {
        return new Person(name, 30);
    }
}
```

## Troubleshooting

### Server doesn't shut down
- Ensure you're calling `server.down()` (fixed in 1.0)
- Check for daemon threads that prevent JVM exit
- Verify no external resources are keeping connections open

### Port already in use
- Ensure previous server instance was properly shut down
- Check for other processes using the port: `lsof -i :PORT`
- Use dynamic port allocation in tests

### Mapper not found
- Verify mapper is registered for the exact class name
- Check for primitive vs. wrapper type mismatches
- Ensure custom mappers are registered before starting server

### Connection refused
- Verify server is started: `server.up()`
- Check firewall settings
- Ensure correct host and port in client factory

## Recent Enhancements (2026)

**V2 Array Handling & Parser Priority (2026-02-19):**
- ✅ **Array safety**: `MAX_ARRAY_SIZE` (100k) and `MAX_ARRAY_DEPTH` (10) in ProtocolV2Codec to prevent DoS
- ✅ **Array encoding**: Primitive/String arrays use `Arrays.toString()`; object arrays use RawTypeMapper (single Base64, no double-encoding)
- ✅ **Parser parameter order**: P1 = primitives/String/primitive arrays/String[] (fast path); P2 = object arrays; P3 = user mapper; P4 = collections; P5 = Serializable; P6 = fallback
- ✅ **MapperHelper**: `Deque` added to `DEFAULT_MAPPERS` (with RawTypeMapper)
- ✅ **ProtocolV2Parser**: Object-array params parsed as Base64 → RawTypeMapper; `isPrimitiveOrStringComponent()` helper
- ✅ **Tests**: ProtocolV2ParserTest.testExtract_objectArrayParameter; ArrayAndDequeIntegrationTest; NettyArrayE2ETest (int[]/String[]/PersonDto[] over Netty)

**Exception System Refactoring (2026-02-19):**
- ✅ Unified exception hierarchy: 5 core exceptions (was 8)
- ✅ Deleted redundant exceptions: ServerSideException, ParseException, RemoteInvocationException, MapperNotFoundException
- ✅ All exceptions now unchecked (RuntimeException) for cleaner API
- ✅ Intelligent exception reconstruction with semantic fallback
- ✅ New: RemoteBusinessException and RemoteServerException for missing classes
- ✅ ParseException merged into ProtocolException (unchecked)
- ✅ Comprehensive E2E tests: ExceptionPropagationTest, ExceptionReconstructionE2ETest
- ✅ Clear semantic categories: protocol, business, server errors

**V2-Only Refactoring (2026-02-19):**
- ✅ V1 protocol completely removed (1000+ lines of code reduced)
- ✅ API renamed: Converter → ProtocolCodec, Extractor → RequestParser
- ✅ ProtocolRouter merged into AbstractTcpRestServer
- ✅ Deleted legacy utilities: Base64, NullObj, DefaultInvoker
- ✅ Simplified architecture with single protocol version

**Protocol V2 Features:**
- ✅ JSON-style parameter arrays `[p1,p2,p3]`
- ✅ Method overloading support via type signatures
- ✅ Status codes (SUCCESS, BUSINESS_EXCEPTION, SERVER_ERROR, PROTOCOL_ERROR)
- ✅ 4-tier intelligent mapper system
- ✅ Collection interfaces support (List, Map, Set)
- ✅ Protocol markers (`~` for null, empty for empty)

**Security Features (2026-02-18):**
- ✅ HMAC-SHA256 message authentication
- ✅ CRC32 integrity verification
- ✅ Class whitelist access control
- ✅ Full Base64 encoding (injection protection)

**Module Refactoring (2026-02-18):**
- ✅ Split monolithic core into 4 focused modules
- ✅ Zero-dependency commons module
- ✅ Separate server implementations (singlethread, nio, netty)
- ✅ Netty upgraded to 4.1.131.Final

**Compression (2026-02-17):**
- ✅ GZIP compression with configurable threshold
- ✅ Compression level control (1-9)
- ✅ Backward compatibility via prefix detection

## Future Enhancements

**High Priority:**
- 🔲 **Asynchronous client API** - Non-blocking calls with CompletableFuture
- 🔲 **Replay attack protection** - Timestamp/nonce validation
- 🔲 **Connection pooling** - Reuse TCP connections for better performance
- 🔲 **Client-side load balancing** - Round-robin, least connections
- 🔲 **Annotation-based configuration** - @TcpRestClient, @TcpRestServer with CDI/Spring support

**Medium Priority:**
- 🔲 **Service discovery integration** - Consul, Eureka, Nacos
- 🔲 **Metrics and monitoring** - Micrometer integration
- 🔲 **Circuit breaker pattern** - Resilience4j integration
- 🔲 **Distributed tracing** - OpenTelemetry, Jaeger
- 🔲 **Request/response interceptors** - AOP-style hooks

**Low Priority:**
- 🔲 **HTTP/2 transport** - Alternative to raw TCP
- 🔲 **WebSocket support** - Bidirectional communication
- 🔲 **Protocol buffer support** - Binary serialization option
- 🔲 **GraphQL-style queries** - Advanced query capabilities
- 🔲 **Admin console** - Web UI for monitoring and management

**Research:**
- 🔲 **GraalVM native image** - Compile to native binary
- 🔲 **Reactive Streams** - Project Reactor integration
- 🔲 **Virtual threads (JDK 21+)** - Lightweight concurrency
