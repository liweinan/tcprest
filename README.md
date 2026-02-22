# TcpRest

[![CI](https://github.com/liweinan/tcprest/actions/workflows/ci.yml/badge.svg)](https://github.com/liweinan/tcprest/actions/workflows/ci.yml)
[![CodeQL](https://github.com/liweinan/tcprest/actions/workflows/codeql.yml/badge.svg)](https://github.com/liweinan/tcprest/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange)](https://www.oracle.com/java/)

A lightweight, zero-dependency RPC framework that transforms POJOs into network-accessible services over TCP.

## ⚡ What's New in v2.0 (2026-02-19)

**Exception System Revolution:**
- 🎯 **Intelligent exception reconstruction**: Preserves exact exception types across network
- 🔄 **Smart fallback**: RemoteBusinessException/RemoteServerException when classes missing
- 🧹 **Simplified hierarchy**: 5 core exceptions (was 8), all unchecked
- 📊 **Semantic categories**: Clear distinction between business/server/protocol errors
- ✅ **Full E2E testing**: Comprehensive exception propagation tests

**Major Simplification - V2-Only Architecture:**
- 🗑️ **V1 protocol removed**: Cleaner codebase, reduced complexity
- 📦 **Simplified API**: ProtocolCodec, RequestParser (renamed from Converter/Extractor)
- 🏗️ **Architecture cleanup**: ProtocolRouter merged into AbstractTcpRestServer
- ⚡ **Reduced footprint**: 1000+ lines of code removed

**V2 Protocol Features:**
- ✨ **Method overloading**: Full support with type signatures
- 🧠 **Intelligent mappers**: 4-tier system with auto-serialization for `Serializable` objects
- 📊 **Collection interfaces**: List, Map, Set, Queue, **Deque**, Collection — zero configuration!
- 📐 **Array support**: Primitive/String arrays via `Arrays.toString()`; object arrays (e.g. `Person[]`) via Java serialization; safety limits (max size/depth) to prevent DoS
- 🎯 **Exception propagation**: Full error details with intelligent type reconstruction
- 📦 **Clean wire format**: JSON-style arrays, compact markers (`~` for null)

## Quick Start

### 1. Define Your Service Interface

```java
public interface HelloWorld {
    String helloWorld();
    int add(int a, int b);
}
```

### 2. Create Server Implementation

```java
public class HelloWorldImpl implements HelloWorld {
    @Override
    public String helloWorld() {
        return "Hello, world!";
    }

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
```

### 3. Start the Server

```java
TcpRestServer server = new SingleThreadTcpRestServer(8001);
server.addSingletonResource(new HelloWorldImpl());
server.up();  // server.getStatus() is then TcpRestServerStatus.RUNNING
```

### 4. Create a Client and Call Methods

```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    HelloWorld.class, "localhost", 8001
);
HelloWorld client = factory.getClient();

String greeting = client.helloWorld();    // "Hello, world!"
int sum = client.add(10, 20);              // 30
```

Only **interfaces** may be registered; registering a concrete class throws `IllegalArgumentException`. The factory supports **single-interface** (one interface `Class`, then `getClient()`) and **multi-interface** (multiple interface classes, then `getClient(Class<T>)` per type):

```java
// One factory, multiple service interfaces (varargs)
TcpRestClientFactory factory = new TcpRestClientFactory(
    "localhost", 8001, Calculator.class, UserService.class
);
Calculator calc = factory.getClient(Calculator.class);
UserService users = factory.getClient(UserService.class);
```

That's it! TcpRest handles all serialization, networking, and deserialization automatically.

**API terminology:** The server registers **resources** (implementation classes or singleton instances via `addResource` / `addSingletonResource`). The client registers **interfaces** (contract types only; constructor parameters are `interfaceClass` / `interfaceClasses`). This keeps server = implementation, client = contract.

## Installation

### Maven Dependencies

TcpRest is organized into focused modules - choose what you need:

**1. Client-only applications** (zero dependencies):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-commons</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**2. SingleThread server** (SSL supported, low-medium concurrency):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-singlethread</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**3. NIO server** (medium-high concurrency, no SSL):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-nio</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**4. Netty server** (high concurrency + SSL + production-ready):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-netty</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**5. PGP/GPG signature** (optional, wire format `SIG:GPG:base64`):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-pgp</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```
Requires Bouncy Castle (transitive). Register once (e.g. class-load `cn.huiwings.tcprest.pgp.PgpSignatureHandler`), then use `SecurityConfig.enableCustomSignature("GPG", pgpPrivateKey, pgpPublicKey)` on both server and client. See [Security (GPG)](#gpg-signature-optional) below.

### Server Comparison

| Feature | SingleThread | NIO | Netty |
|---------|-------------|-----|-------|
| **Concurrency** | Low-Medium | Medium-High | Very High |
| **SSL/TLS** | ✅ Yes | ❌ No | ✅ Yes |
| **Async I/O** | ❌ Blocking | ✅ Non-blocking | ✅ Non-blocking |
| **Dependencies** | Zero* | Zero* | Netty 4.1.x |
| **Best For** | Development, Low traffic | Moderate traffic | Production, High traffic |

*Through transitive dependency on `tcprest-commons` (which has zero runtime dependencies)

### UDP transport (Netty module)

The **tcprest-netty** module also provides **UDP** transport: one datagram = one request, one datagram = one response. Same Protocol V2 on the wire. Use when you need low-latency, fire-and-forget style, or non-TCP networks.

| Component | Class | Notes |
|-----------|--------|-------|
| Server | `NettyUdpRestServer` | `new NettyUdpRestServer(port)` then `addResource` / `up()` |
| Client | `NettyUdpRestClientFactory` | `new NettyUdpRestClientFactory(MyService.class, host, port).getClient()`; call `shutdown()` when done |

**Limitations:** No SSL/DTLS. Request and response must fit in a single UDP packet (default max 1472 bytes). Oversized packets are dropped.

```java
// Server
NettyUdpRestServer server = new NettyUdpRestServer(9090);
server.addResource(HelloWorldResource.class);
server.up();

// Client (same interface as TCP)
NettyUdpRestClientFactory factory = new NettyUdpRestClientFactory(HelloWorld.class, "localhost", 9090);
HelloWorld client = factory.getClient();
assertEquals(client.helloWorld(), "Hello, world!");
factory.shutdown();
```

## Key Features

### Zero Dependencies
The `tcprest-commons` module has **zero runtime dependencies** - only JDK built-in APIs. This minimizes dependency conflicts and reduces security vulnerabilities. Server modules (`tcprest-singlethread`, `tcprest-nio`) inherit this zero-dependency principle through `tcprest-commons`.

### Method Overloading Support

TcpRest supports method overloading using type signatures:

```java
// Service with overloaded methods
public interface Calculator {
    int add(int a, int b);           // Integer addition
    double add(double a, double b);   // Double addition
    String add(String a, String b);   // String concatenation
}

// Client (V2 is default, no configuration needed)
TcpRestClientFactory factory = new TcpRestClientFactory(
    Calculator.class, "localhost", 8001
);

Calculator calc = factory.getClient();

calc.add(5, 3);         // Calls int add(int, int) → 8
calc.add(2.5, 3.5);     // Calls double add(double, double) → 6.0
calc.add("Hello", "!"); // Calls String add(String, String) → "Hello!"
```

### Exception Handling

TcpRest provides intelligent exception propagation that preserves exception types and semantics across the network boundary.

#### Exception Reconstruction

When a server throws an exception, TcpRest attempts to recreate the exact same exception type on the client:

```java
// Server-side service
public class UserService {
    public User getUser(int id) {
        if (id < 0) {
            throw new IllegalArgumentException("User ID must be positive");
        }
        // ...
    }
}

// Client receives the EXACT same exception type
try {
    User user = userService.getUser(-1);
} catch (IllegalArgumentException e) {
    // Caught as IllegalArgumentException - exact type preserved!
    assertEquals("User ID must be positive", e.getMessage());
}
```

**How it works:**
1. Server encodes exception with full class name: `java.lang.IllegalArgumentException: User ID must be positive`
2. Client attempts to load and instantiate the exception class via reflection
3. If successful → client receives the original exception type
4. If class not available → intelligent fallback (see below)

#### Intelligent Exception Fallback

When the client doesn't have the exception class (e.g., custom server-side exceptions), TcpRest uses semantic fallback wrappers:

**Business Exception Fallback:**
```java
// Server has custom exception (client doesn't)
public class OrderValidationException extends BusinessException {
    public OrderValidationException(String message) {
        super(message);
    }
}

// Server throws
throw new OrderValidationException("Order amount exceeds limit");

// Client receives RemoteBusinessException
try {
    orderService.placeOrder(order);
} catch (RemoteBusinessException e) {
    // Can still handle appropriately!
    System.out.println("Business error: " + e.getRemoteExceptionType());
    // Output: "com.example.OrderValidationException"

    assertTrue(e.isBusinessException());
    // Can retry with corrected input
}
```

**Server Exception Fallback:**
```java
// Server has custom exception (client doesn't)
public class DatabasePoolException extends RuntimeException {
    // ...
}

// Server throws
throw new DatabasePoolException("Connection pool exhausted");

// Client receives RemoteServerException
try {
    userService.getUser(123);
} catch (RemoteServerException e) {
    // Identifies as server-side error!
    System.err.println("Server error: " + e.getRemoteExceptionType());
    // Output: "com.example.DatabasePoolException"

    assertTrue(e.isServerError());
    // Should log/alert, not retry
}
```

#### Exception Categories

TcpRest classifies exceptions into clear categories:

| Category | Exception Type | Meaning | Client Handling |
|----------|---------------|---------|-----------------|
| **Business Errors** | `BusinessException` | Expected application logic errors | Retry with corrected input |
| **Business Fallback** | `RemoteBusinessException` | Server business exception (client missing class) | Handle as business error |
| **Server Errors** | Standard exceptions (NPE, etc.) | Unexpected server-side failures | Log/alert, don't retry |
| **Server Fallback** | `RemoteServerException` | Server exception (client missing class) | Handle as server error |
| **Protocol Errors** | `ProtocolException` | Protocol format/parsing errors | Fix protocol mismatch |
| **Security Errors** | `SecurityException` | Security violations (checksum, whitelist) | Check security config |
| **Timeout Errors** | `TimeoutException` | Client-side timeout | Increase timeout or optimize server |

#### Best Practices

**✅ Use BusinessException for expected errors:**
```java
public class OrderService {
    public void validateOrder(Order order) {
        if (order.getAmount() > limit) {
            // Extends BusinessException - client knows it's a business rule
            throw new ValidationException("Order exceeds limit");
        }
    }
}
```

**✅ Client can distinguish error types:**
```java
try {
    orderService.placeOrder(order);
} catch (RemoteBusinessException e) {
    // Business error - user can fix
    showUserError("Please correct: " + e.getMessage());
} catch (RemoteServerException e) {
    // Server error - ops team issue
    logger.error("Server failure", e);
    showUserError("Service temporarily unavailable");
}
```

**✅ Standard exceptions propagate correctly:**
```java
// NullPointerException, IllegalArgumentException, etc.
// are standard Java exceptions - always available on both sides
try {
    service.process(null);
} catch (NullPointerException e) {
    // Exact type preserved!
}
```

### Data Compression

Reduce bandwidth usage with automatic GZIP compression:

**Server:**
```java
TcpRestServer server = new SingleThreadTcpRestServer(8001);
server.enableCompression();  // Enable with defaults
server.up();
```

**Client:**
```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8001
)
    .withCompression();  // Enable compression

MyService client = factory.getClient();
```

**Custom configuration:**
```java
CompressionConfig config = new CompressionConfig(
    true,   // enabled
    1024,   // threshold: only compress if message > 1KB
    9       // level: 1=fastest, 9=best compression
);

server.setCompressionConfig(config);
// or
factory.withCompression(config);
```

Compression is fully backward-compatible - compressed and uncompressed clients/servers can communicate seamlessly.

### SSL/TLS Support

Secure your communication with SSL:

**Server:**
```java
SSLParam serverSSL = new SSLParam();
serverSSL.setKeyStorePath("classpath:server_ks");
serverSSL.setKeyStoreKeyPass("password");
serverSSL.setTrustStorePath("classpath:server_ks");
serverSSL.setNeedClientAuth(true);  // Optional: require client cert

TcpRestServer server = new SingleThreadTcpRestServer(8443, serverSSL);
server.addSingletonResource(new MyServiceImpl());
server.up();
```

**Client:**
```java
SSLParam clientSSL = new SSLParam();
clientSSL.setKeyStorePath("classpath:client_ks");
clientSSL.setKeyStoreKeyPass("password");
clientSSL.setTrustStorePath("classpath:client_ks");

TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8443, null, clientSSL
);
MyService client = factory.getClient();
```

### Network Binding

Control which network interfaces your server listens on for security and multi-homing scenarios:

**Bind to specific IP address (recommended for production):**
```java
// Only accept connections on localhost (more secure)
TcpRestServer server = new SingleThreadTcpRestServer(8001, "127.0.0.1");

// Bind to specific internal network interface
TcpRestServer server = new NioTcpRestServer(8001, "192.168.1.100");

// IPv6 localhost
TcpRestServer server = new NettyTcpRestServer(8001, "::1");
```

**Bind to all interfaces (default):**
```java
// Accepts connections on all network interfaces (0.0.0.0)
TcpRestServer server = new SingleThreadTcpRestServer(8001);
// Equivalent to:
TcpRestServer server = new SingleThreadTcpRestServer(8001, null);
```

**Combine with SSL:**
```java
SSLParam sslParams = new SSLParam();
sslParams.setKeyStorePath("classpath:server_ks");
sslParams.setKeyStoreKeyPass("password");

// Secure server on localhost only
TcpRestServer server = new SingleThreadTcpRestServer(8443, "127.0.0.1", sslParams);
server.up();
```

**Security Best Practices:**
- **Development**: Bind to `127.0.0.1` to prevent external access
- **Production**: Bind to specific internal IPs instead of `0.0.0.0`
- **Public services**: Use SSL/TLS with specific IP binding

### Server Implementations

TcpRest provides three server implementations:

| Server | Module | Best For | SSL Support | IPv6 Support | Serializable Auto-Mapper |
|--------|--------|----------|-------------|--------------|--------------------------|
| `SingleThreadTcpRestServer` | tcprest-singlethread | Low traffic, simple deployment | ✅ Yes | ✅ Yes | ✅ Yes |
| `NioTcpRestServer` | tcprest-nio | Medium traffic, non-blocking I/O | ❌ No | ✅ Yes | ✅ Yes |
| `NettyTcpRestServer` | tcprest-netty | High traffic, production systems | ✅ Yes | ✅ Yes | ✅ Yes |

**Notes:**
- **IPv6 Support**: All servers support IPv6 addresses (e.g., `::1` for localhost, `::` for all interfaces)
- **Serializable Auto-Mapper**: Classes implementing `java.io.Serializable` don't need custom mappers - they work automatically
- **Transient Support**: Fields marked `transient` are automatically excluded from serialization

### Netty Server Usage (Recommended for Production)

The Netty server provides the best performance and SSL support for production deployments.

**Basic Setup:**
```java
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;

// Create high-performance Netty server
TcpRestServer server = new NettyTcpRestServer(8001);
server.addSingletonResource(new MyServiceImpl());
server.up();
```

**With SSL/TLS:**
```java
import cn.huiwings.tcprest.ssl.SSLParams;

SSLParam sslParams = new SSLParam();
sslParams.setKeyStorePath("classpath:server_ks");
sslParams.setKeyStoreKeyPass("password");
sslParams.setTrustStorePath("classpath:server_ks");
sslParams.setNeedClientAuth(true);  // Optional: mutual TLS

TcpRestServer server = new NettyTcpRestServer(8443, sslParams);
server.addSingletonResource(new MyServiceImpl());
server.up();
```

**With Bind Address (Security Best Practice):**
```java
// Bind to specific IP for security
TcpRestServer server = new NettyTcpRestServer(8001, "127.0.0.1");

// Or combine with SSL
TcpRestServer server = new NettyTcpRestServer(8443, "192.168.1.100", sslParams);
```

**Complete Production Example:**
```java
// Production-ready setup: Netty + SSL + localhost binding
SSLParam sslParams = new SSLParam();
sslParams.setKeyStorePath("classpath:server_ks");
sslParams.setKeyStoreKeyPass("password");
sslParams.setTrustStorePath("classpath:server_ks");

TcpRestServer server = new NettyTcpRestServer(8443, "127.0.0.1", sslParams);
server.addSingletonResource(new UserServiceImpl());
server.up();

// Client connection
SSLParam clientSSL = new SSLParam();
clientSSL.setKeyStorePath("classpath:client_ks");
clientSSL.setKeyStoreKeyPass("password");
clientSSL.setTrustStorePath("classpath:client_ks");

TcpRestClientFactory factory = new TcpRestClientFactory(
    UserService.class, "127.0.0.1", 8443, null, clientSSL
);
UserService client = factory.getClient();
```

## Performance

TcpRest (especially the Netty implementation) offers significant performance advantages over traditional HTTP REST in many scenarios.

### Protocol Overhead Comparison

**Traditional HTTP REST:**
```http
POST /api/calculator/add HTTP/1.1
Host: localhost:8080
Content-Type: application/json
Content-Length: 25
...
```
Overhead: **~200-300 bytes** of HTTP headers + JSON payload

**TcpRest Protocol:**
```
0|Y24uaHVpd2luZ3MudGNwcmVzdC5DYWNGY3VsYXRvci9hZGQ|MTAsIDIw
```
Overhead: **~50-100 bytes** (compression flag + Base64 metadata + params)

**Result: 60-80% protocol overhead reduction**

### Performance Benefits

| Aspect | HTTP REST | TcpRest Netty | Improvement |
|--------|-----------|---------------|-------------|
| **Protocol Overhead** | 200-300 bytes | 50-100 bytes | 60-80% reduction |
| **Serialization** | JSON text | Binary/Custom mappers | 50-70% smaller |
| **Compression** | Usually disabled | Optional GZIP (96% for repetitive data) | 80-95% reduction |
| **Connection Reuse** | HTTP Keep-Alive | Long-lived TCP | Zero handshake overhead |
| **Concurrency (1000+ connections)** | ~1000 threads | ~10-20 threads (EventLoop) | 10-50x better |
| **Latency (simple RPC)** | 3-6ms | 0.6-0.9ms | 3-10x faster |

### Netty Server Advantages

The `NettyTcpRestServer` leverages Netty's high-performance features:

- **Zero-Copy I/O**: Reduces memory copies
- **Event-Driven Architecture**: Boss/Worker thread pool model
- **NIO Selector**: Single thread handles thousands of connections
- **Direct Buffers**: Reduces JVM heap pressure

### When TcpRest is Faster

✅ **Best scenarios:**
- **Microservice internal communication** - High frequency RPC calls
- **High concurrency** (10k+ concurrent connections) - Netty's EventLoop excels
- **Low latency requirements** (<5ms) - Minimal protocol overhead
- **Large volume of small requests** - Connection pooling and reuse

❌ **When to use HTTP REST instead:**
- Public-facing APIs (HTTP standard ecosystem)
- Cross-language/platform calls (REST + JSON more universal)
- Need for HTTP caching/CDN
- Load balancing/API gateway integration

### Compression Performance

From benchmark tests (see `CompressionBenchmarkTest.java`):

```
Data Type          | Compression Ratio
-------------------|------------------
Repetitive Text    | 96% reduction
JSON/XML           | 88-90% reduction
General Text       | 85-95% reduction
Overhead           | <1ms per operation
```

### Performance Tuning Tips

```java
// 1. Enable compression for bandwidth-heavy scenarios
server.enableCompression();

// 2. Use singleton resources (avoid instantiation overhead)
server.addSingletonResource(new MyServiceImpl());

// 3. Implement Serializable for automatic binary serialization (V2 feature)
public class MyData implements Serializable { ... }

// Note: Protocol V2 is already default - no configuration needed!
```

**Summary:** For controlled internal environments with high concurrency and low latency requirements, TcpRest can deliver **2-10x performance improvement** over traditional HTTP REST frameworks.

## Common Use Cases

### Singleton vs Per-Request Resources

**Singleton (recommended for stateless services):**
```java
server.addSingletonResource(new MyServiceImpl());
// Same instance handles all requests
```

**Per-Request (for stateful services):**
```java
server.addResource(MyServiceImpl.class);
// New instance created for each request
```

### V2 Features Example (Best Practice: Simple Design)

Here's a clean example demonstrating V2 features using **simple, interface-based design**:

```java
import java.io.Serializable;

/**
 * Simple DTO - just implement Serializable for zero-config auto-serialization.
 */
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String email;
    private transient String password;  // Excluded from serialization

    public UserInfo(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    // Getters/setters...
}

/**
 * Simple service interface - focus on clear method signatures.
 *
 * ✅ Best Practice: Use simple parameters and DTOs instead of complex objects.
 */
public interface UserService {
    // Basic CRUD with simple parameters
    UserInfo getUser(int userId);
    boolean updateUser(int userId, String name, String email);

    // Method overloading (V2 feature)
    UserInfo createUser(String name, String email);
    UserInfo createUser(String name, String email, String role);

    // Collections work automatically (V2 feature)
    List<UserInfo> getAllUsers();
    Map<String, UserInfo> getUsersByRole(String role);

    // Exception handling (V2 feature)
    void validateEmail(String email);  // Throws if invalid
}

/**
 * Simple implementation - focus on business logic, not complex data structures.
 */
public class UserServiceImpl implements UserService {
    private final Map<Integer, UserInfo> users = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);

    @Override
    public UserInfo createUser(String name, String email) {
        return createUser(name, email, "user");  // Default role
    }

    @Override
    public UserInfo createUser(String name, String email, String role) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }

        int id = idCounter.getAndIncrement();
        UserInfo user = new UserInfo(id, name, email);
        users.put(id, user);
        return user;
    }

    @Override
    public void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }

    @Override
    public List<UserInfo> getAllUsers() {
        return new ArrayList<>(users.values());  // Collections work automatically!
    }

    @Override
    public Map<String, UserInfo> getUsersByRole(String role) {
        // Map interface automatically supported in V2
        return users.values().stream()
            .collect(Collectors.toMap(u -> u.getName(), u -> u));
    }

    // ... other methods
}
```

**Server Setup:**
```java
TcpRestServer server = new NettyTcpRestServer(8001);
server.addSingletonResource(new UserServiceImpl());
server.up();
```

**Client Usage:**
```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    UserService.class, "localhost", 8001
);
UserService service = factory.getClient();

// Method overloading works automatically
UserInfo user1 = service.createUser("Alice", "alice@example.com");           // Default role
UserInfo user2 = service.createUser("Bob", "bob@example.com", "admin");      // Custom role

// Collections work automatically (no mapper needed!)
List<UserInfo> allUsers = service.getAllUsers();
Map<String, UserInfo> admins = service.getUsersByRole("admin");

// Exception handling works automatically
try {
    service.validateEmail("invalid");
} catch (RuntimeException e) {
    System.out.println(e.getMessage());  // "IllegalArgumentException: Invalid email format"
}

// Complex object handling (auto-serialization)
UserInfo retrieved = service.getUser(user1.getId());
System.out.println("User: " + retrieved.getName());  // All fields preserved
```

**Why This is Best Practice:**

✅ **Simple DTOs** - `UserInfo` is flat and focused
✅ **Clear interface** - Methods have clear, simple parameters
✅ **Zero configuration** - Just implement `Serializable` on DTOs
✅ **Collections work** - List, Map, Set automatically supported
✅ **Easy to test** - Simple objects, simple mocking
✅ **Easy to debug** - Clear data flow, no complex object graphs

❌ **Avoid:**
- Deep inheritance hierarchies
- Complex nested objects
- Circular references
- Large object graphs

**See Full Example:** [`ProtocolV2IntegrationTest.java`](tcprest-singlethread/src/test/java/cn/huiwings/tcprest/test/integration/ProtocolV2IntegrationTest.java) for complete working code with tests.

### Intelligent Mapper System (Zero-Configuration Support)

**TcpRest features a 4-tier intelligent mapper system - collections, DTOs, and most classes work automatically with zero configuration!**

#### Priority 1: User-Defined Mappers (Highest)
Custom mappers for fine-grained control (e.g., JSON serialization with Gson):

```java
// Register custom Gson mapper (for public APIs or custom formats)
server.addMapper(User.class.getCanonicalName(), new GsonUserMapper());

Map<String, Mapper> mappers = new HashMap<>();
mappers.put(User.class.getCanonicalName(), new GsonUserMapper());
TcpRestClientFactory factory = new TcpRestClientFactory(
    UserService.class, "localhost", 8001, mappers
);
```

#### Priority 2: Collection Interfaces (High) **Zero Config**
Built-in support for `List`, `Map`, `Set`, `Queue`, `Deque`, `Collection` - **no mapper needed!**

```java
// Collection interfaces work automatically in method signatures
public interface DataService {
    List<String> getItems();              // ✅ Works automatically
    Map<String, Integer> getScores();     // ✅ Works automatically
    Set<String> getUniqueNames();         // ✅ Works automatically
}

// Client usage - no mapper needed
DataService service = factory.getClient();
List<String> items = service.getItems();        // ArrayList → List → ArrayList
Map<String, Integer> scores = service.getScores();  // HashMap → Map → HashMap

// All Serializable implementations supported
List<String> myList = new ArrayList<>();     // ✅
List<String> myList = new LinkedList<>();    // ✅
Map<String, Integer> myMap = new HashMap<>();   // ✅
Map<String, Integer> myMap = new TreeMap<>();   // ✅
```

**Supported interfaces (MapperHelper.DEFAULT_MAPPERS):**
- ✅ `java.util.List` - ArrayList, LinkedList, Vector, etc.
- ✅ `java.util.Map` - HashMap, TreeMap, LinkedHashMap, etc.
- ✅ `java.util.Set` - HashSet, TreeSet, LinkedHashSet, etc.
- ✅ `java.util.Queue` - LinkedList, PriorityQueue, etc.
- ✅ `java.util.Deque` - ArrayDeque, LinkedList (added in DEFAULT_MAPPERS)
- ✅ `java.util.Collection` - parent interface

**Type preservation:** Concrete types are preserved (ArrayList stays ArrayList, not just List).

#### Priority 3: Auto-Serialization (Medium) ⭐ **Recommended**
Any class implementing `Serializable` works automatically - **no mapper needed!**

```java
import java.io.Serializable;

// Simply implement Serializable
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private transient String password;  // Excluded from serialization

    // Getters/setters...
}

// Works automatically - no mapper registration required!
public interface UserService {
    User getUser(int id);
    User saveUser(User user);
    List<User> getAllUsers();  // Collections + Serializable = ✅✅
}

// Server setup - no mapper needed
TcpRestServer server = new SingleThreadTcpRestServer(8001);
server.addSingletonResource(new UserServiceImpl());
server.up();

// Client setup - no mapper needed
TcpRestClientFactory factory = new TcpRestClientFactory(
    UserService.class, "localhost", 8001
);
UserService client = factory.getClient();

User user = client.getUser(123);  // Just works! 🎉
List<User> users = client.getAllUsers();  // Collections + DTOs = ✅
```

**What's supported automatically:**
- ✅ Any class implementing `Serializable` (DTOs, entities, domain objects)
- ✅ Collection interfaces (List, Map, Set, Queue, Deque, Collection)
- ✅ **Arrays**: primitive/`String[]` via `Arrays.toString()`; object arrays (e.g. `User[]`) via Java serialization, with size/depth limits to prevent DoS
- ✅ `transient` fields (automatically excluded)
- ✅ Nested Serializable objects (entire object graph)
- ✅ Class inheritance (exact types preserved: Car → Car, not Vehicle)

#### Priority 4: Built-in Conversion (Lowest)
Primitives, wrappers, Strings, and arrays - always supported.

#### When to Use Each Approach

| Approach | Parser priority | Use Case | Configuration |
|----------|-----------------|----------|----------------|
| **Built-in** | P1 | Primitives, strings, primitive/String arrays | Always available |
| **Custom Mapper** | P3 | Public APIs, JSON/XML, cross-language, exact generics | Register mapper on client & server |
| **Collection Interfaces** | P4 | List, Map, Set, Queue, Deque parameters | Zero config - works automatically! |
| **Auto-Serialization** | P5 | Internal microservices, DTOs you control | Just implement `Serializable` - zero config! |

**Best Practices:**

✅ **Recommended approach for internal services:**
1. Use collection interfaces (List, Map, Set) in method signatures
2. Implement `Serializable` on your DTOs
3. Keep DTOs simple and flat (avoid deep nesting)
4. Zero configuration needed!

❌ **When to use custom mappers:**
- Public APIs requiring JSON/XML format
- Cross-language compatibility
- Specific generic type preservation (e.g., `List<User>` with exact type)
- Human-readable wire format for debugging

**Quick Example - Best Practice:**
```java
// ✅ Perfect: Collection interfaces + Simple DTOs
public interface UserService {
    List<UserInfo> getAllUsers();           // Collection interface
    Map<String, UserInfo> getUsersByRole(String role);  // Collection interface
}

public class UserInfo implements Serializable {  // Simple DTO
    private int id;
    private String name;
    private String email;
    // Simple, flat structure
}

// Zero configuration - everything works automatically!
```

## Best Practices for TcpRest

### 1. Design Philosophy: Keep It Simple

**✅ Recommended: Simple, Interface-Based Design**

```java
// Good: Clear, simple interface with focused methods
public interface UserService {
    UserInfo getUser(int userId);
    boolean updateUser(int userId, String name, String email);
    List<UserInfo> getAllUsers();
}

// Good: Simple, flat DTO
public class UserInfo implements Serializable {
    private int id;
    private String name;
    private String email;
    // Simple structure, easy to understand and test
}
```

**❌ Avoid: Complex Object Hierarchies**

```java
// Bad: Deep inheritance
public class Car extends Vehicle extends MovableObject extends PhysicalEntity { }

// Bad: Complex nested objects
public class Order {
    private Customer customer;           // nested
    private Address shippingAddress;     // nested
    private Address billingAddress;      // nested
    private List<OrderItem> items;       // nested collection
    private Map<String, Discount> discounts;  // nested map
    private Payment payment;             // nested
}
```

### 2. DTO Design Guidelines

**✅ Do:**
- Keep DTOs flat and focused on data
- Implement `Serializable` with `serialVersionUID`
- Use `transient` for sensitive fields
- Follow single responsibility principle

**❌ Don't:**
- Create deep inheritance hierarchies
- Include business logic in DTOs
- Use circular references
- Nest objects more than 2 levels deep

**Example:**

```java
// ✅ Good: Simple, focused DTO
public class UserInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String email;
    private String city;  // Flatten nested Address.city

    private transient String password;  // Excluded from serialization
}

// ❌ Bad: Complex nested structure
public class User implements Serializable {
    private int id;
    private PersonalInfo personalInfo;  // Nested
    private Address address;            // Nested
    private Company company;            // Nested
    private List<Skill> skills;         // Nested collection
    // Too many nested levels!
}
```

### 3. Service Interface Design

**✅ Use collection interfaces in method signatures:**

```java
public interface DataService {
    List<Item> getItems();           // ✅ Interface type
    Map<String, Score> getScores();  // ✅ Interface type
    Set<String> getNames();          // ✅ Interface type
}
```

**✅ Keep methods focused and simple:**

```java
// Good: Simple, clear methods
public interface UserService {
    UserInfo getUser(int id);
    boolean updateUser(int id, String name, String email);
    List<UserInfo> searchUsers(String query);
}
```

**❌ Avoid complex method signatures:**

```java
// Bad: Too many parameters
public boolean updateCompleteUserProfile(
    int id, String name, String email, String phone, String address,
    String city, String country, String zipCode, List<String> interests
);

// Better: Use a simple DTO
public boolean updateUserProfile(int id, UserProfile profile);
```

### 4. When to Use What

| Scenario | Recommendation | Why |
|----------|---------------|-----|
| **Internal microservices** | Collection interfaces + Serializable DTOs | Zero config, fast, type-safe |
| **Public APIs** | Custom JSON mappers (Gson/Jackson) | Human-readable, cross-language |
| **Simple data transfer** | Flat DTOs with primitives | Easy to test and debug |
| **Complex data** | Break into multiple simple methods | Better separation of concerns |
| **High performance** | Binary serialization (Serializable) | Faster than JSON |
| **Cross-language** | Custom JSON mappers | Universal format |

### 5. Common Anti-Patterns to Avoid

| Anti-Pattern | Why It's Bad | Better Alternative |
|--------------|--------------|-------------------|
| **God Objects** | Hard to maintain, test | Break into focused DTOs |
| **Deep Nesting** | Serialization overhead | Flatten structure |
| **No serialVersionUID** | Version conflicts | Always define it |
| **Business Logic in DTOs** | Violates SRP | Keep DTOs as data containers |
| **Ignoring transient** | Sends sensitive data | Mark passwords/secrets transient |
| **Large collections** | Memory/performance issues | Pagination or streaming |

### 6. Testing Best Practices

```java
// Good: Easy to test with simple objects
@Test
public void testUserService() {
    UserInfo user = new UserInfo(1, "Alice", "alice@example.com");
    boolean result = service.updateUser(user.getId(), "Alice Smith", user.getEmail());
    assertTrue(result);
}

// Bad: Hard to test with complex nested objects
@Test
public void testComplexOrder() {
    Order order = new Order(
        new Customer(new PersonalInfo(...), new Address(...)),
        new ShippingInfo(new Address(...), new Carrier(...)),
        Arrays.asList(new OrderItem(new Product(...), new Pricing(...)))
    );
    // Too complex to set up!
}
```

### 7. Performance Tips

✅ **Enable compression for bandwidth-heavy scenarios:**
```java
server.enableCompression();
factory.withCompression();
```

✅ **Use singleton resources when possible:**
```java
server.addSingletonResource(new MyServiceImpl());  // One instance for all requests
```

✅ **Implement Serializable for automatic binary serialization:**
```java
public class MyData implements Serializable { ... }  // Faster than custom JSON
```

### Summary: The TcpRest Way

1. **Keep it simple** - Flat DTOs, clear interfaces
2. **Use built-in features** - Collection interfaces, auto-serialization
3. **Avoid complexity** - No deep nesting, no god objects
4. **Think interface-first** - Design clear service contracts
5. **Test easily** - Simple objects = simple tests

**Remember:** TcpRest is designed for **high-performance internal microservices**. For this use case, simplicity and zero-configuration support give you the best developer experience and maintainability.

### Custom Mappers (Advanced Usage)

For more control over serialization format (e.g., human-readable JSON instead of binary), implement custom mappers. **Custom mappers have highest priority** and override auto-serialization.

**Example: CSV Format Mapper**
```java
public class PersonMapper implements Mapper {
    @Override
    public Object stringToObject(String s) {
        String[] parts = s.split(",");
        return new Person(parts[0], Integer.parseInt(parts[1]));
    }

    @Override
    public String objectToString(Object o) {
        Person p = (Person) o;
        return p.getName() + "," + p.getAge();  // CSV format
    }
}

// Register on server
server.addMapper(Person.class.getCanonicalName(), new PersonMapper());

// Register on client
Map<String, Mapper> mappers = new HashMap<>();
mappers.put(Person.class.getCanonicalName(), new PersonMapper());

TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8001, mappers
);
```

**Example: Gson/JSON Mapper** (See `V2MapperDemoTest.java` for complete working example)
```java
import com.google.gson.Gson;

public class GsonUserMapper implements Mapper {
    private final Gson gson = new Gson();

    @Override
    public String objectToString(Object object) {
        return gson.toJson(object);  // Produces: {"name":"Alice","age":25}
    }

    @Override
    public Object stringToObject(String param) {
        return gson.fromJson(param, User.class);
    }
}

// Register on both server and client
server.addMapper(User.class.getCanonicalName(), new GsonUserMapper());
mappers.put(User.class.getCanonicalName(), new GsonUserMapper());
```

**Custom Mapper Benefits:**
- Human-readable wire format (JSON, XML, CSV)
- Efficient string representation
- Cross-language compatibility (if you implement clients in other languages)
- Fine-grained control over what gets serialized

**When NOT to use custom mappers:**
- Simple DTOs in Java-only microservices → Use auto-serialization instead
- Classes that already implement `Serializable` and don't need special format → Zero config!

### Timeout Configuration

Set custom timeouts for specific methods:

```java
public interface MyService {
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    String longRunningOperation();
}
```

### Proper Shutdown

Always shutdown servers gracefully:

```java
try {
    server.up();
    // ... server running ...
} finally {
    server.down();  // Releases ports and resources
}
```

## Documentation

- **[PROTOCOL.md](PROTOCOL.md)** - Wire protocol specification, format details, and compatibility
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Technical design, implementation details, and internals
- **[CLAUDE.md](CLAUDE.md)** - Development guidelines and coding standards

## Building from Source

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Verify zero dependencies in commons
mvn dependency:tree -pl tcprest-commons
```

## Examples

See the test directories for comprehensive examples:

**Commons (protocol, codecs, mappers):**
- `tcprest-commons/src/test/java/cn/huiwings/tcprest/`

**SingleThread server (integration tests, SSL):**
- **Integration tests**: `tcprest-singlethread/src/test/java/.../integration/ProtocolV2IntegrationTest`
- **Compression**: `tcprest-singlethread/src/test/java/.../compression/*`
- **SSL**: `tcprest-singlethread/src/test/java/.../ssl/*`

**NIO server:**
- See `tcprest-nio/src/main/java/.../example/NioServerDemo.java`

**Netty server:**
- **E2E (arrays)**: `tcprest-netty/.../integration/NettyArrayE2ETest` — int[]/String[]/object array over Netty
- **Integration (array + Deque)**: `tcprest-netty/.../integration/ArrayAndDequeIntegrationTest`
- See `tcprest-netty/src/test/java/cn/huiwings/tcprest/test/`

## Requirements

- **Java 11+**
- **Maven 3.6+** (for building)

## License

Apache License 2.0 - See [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please follow the guidelines in [CLAUDE.md](CLAUDE.md).

## Support

- **Issues**: Report bugs and request features via [GitHub Issues](https://github.com/liweinan/tcprest/issues)
- **Documentation**: See [PROTOCOL.md](PROTOCOL.md) and [ARCHITECTURE.md](ARCHITECTURE.md)

## Security (NEW - 2026-02-18)

### Security-Enhanced Protocol

TcpRest now includes comprehensive security features to protect against injection attacks and message tampering.

#### Quick Security Setup

```java
import cn.huiwings.tcprest.security.SecurityConfig;

// Basic security with CRC32 checksum
SecurityConfig securityConfig = new SecurityConfig().enableCRC32();

// Production security with HMAC
SecurityConfig securityConfig = new SecurityConfig()
    .enableHMAC("your-secret-key");

// With class whitelist (recommended for public APIs)
SecurityConfig securityConfig = new SecurityConfig()
    .enableHMAC("your-secret-key")
    .enableClassWhitelist()
    .allowClass("com.example.PublicAPI");

// Optional: origin signature (RSA-SHA256). CHK = integrity, SIG = who sent it.
// Server: enableSignature(serverPrivateKey, clientPublicKey);
// Client: enableSignature(clientPrivateKey, serverPublicKey);
SecurityConfig withSig = new SecurityConfig()
    .enableCRC32()
    .enableSignature(myPrivateKey, peerPublicKey);
```

**CHK vs SIG:** CHK (checksum) is **integrity only** (CRC32/HMAC). SIG (signature) is **origin authentication** (e.g. RSA-SHA256 or GPG). Both can be used together: wire format is `content|CHK:value|SIG:value`.

#### GPG signature (optional)

To use **GPG/OpenPGP** signatures (wire format `SIG:GPG:base64`), add the optional **tcprest-pgp** module and Bouncy Castle. Ensure the GPG handler is loaded (e.g. reference `cn.huiwings.tcprest.pgp.PgpSignatureHandler` or call `PgpSignatureHandler.register()`), then configure custom signature with PGP key objects:

```java
// Server: sign with server PGP private key, verify with client PGP public key
SecurityConfig serverConfig = new SecurityConfig()
    .enableCRC32()
    .enableCustomSignature("GPG", serverPgpPrivateKey, clientPgpPublicKey);
// Client: sign with client PGP private key, verify with server PGP public key
SecurityConfig clientConfig = new SecurityConfig()
    .enableCRC32()
    .enableCustomSignature("GPG", clientPgpPrivateKey, serverPgpPublicKey);
```

Key types are Bouncy Castle `PGPPrivateKey` and `PGPPublicKey` (e.g. from key rings or in-memory generation).

#### Security Features

| Feature | Description | Use Case |
|---------|-------------|----------|
| **Full Encoding** | Base64-encodes all protocol components | Prevents all injection attacks |
| **CHK (CRC32)** | Fast integrity verification | Detect accidental corruption |
| **CHK (HMAC-SHA256)** | Symmetric integrity/auth | Prevent tampering (shared secret) |
| **SIG (RSA-SHA256)** | Asymmetric origin signature (JDK) | Prove who sent the message |
| **SIG (GPG)** | OpenPGP signature (tcprest-pgp, Bouncy Castle) | Same as above, `SIG:GPG:base64` |
| **Class Whitelist** | Restrict accessible classes | Public API security |

#### Protection Against Attacks

✅ **Path Traversal** (`../../EvilClass`) - Base64 encoding prevents  
✅ **Delimiter Injection** (`Class/method()/evil`) - Structure protected  
✅ **Method Injection** (`method:::badParam`) - Cannot inject  
✅ **Message Tampering** - HMAC detects modifications  
✅ **Unauthorized Access** - Whitelist restricts classes

**📖 Full Documentation:** See [SECURITY-PROTOCOL.md](SECURITY-PROTOCOL.md) for complete security guide.

