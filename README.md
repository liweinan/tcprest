# TcpRest

[![CI](https://github.com/liweinan/tcprest/actions/workflows/ci.yml/badge.svg)](https://github.com/liweinan/tcprest/actions/workflows/ci.yml)
[![CodeQL](https://github.com/liweinan/tcprest/actions/workflows/codeql.yml/badge.svg)](https://github.com/liweinan/tcprest/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java Version](https://img.shields.io/badge/Java-11%2B-orange)](https://www.oracle.com/java/)

A lightweight, zero-dependency RPC framework that transforms POJOs into network-accessible services over TCP.

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
server.up();
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

That's it! TcpRest handles all serialization, networking, and deserialization automatically.

## Installation

### Maven Dependencies

**For most use cases** (zero dependencies):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**For high-performance Netty server** (optional):
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-netty</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## Key Features

### Zero Dependencies
The `tcprest-core` module has **zero runtime dependencies** - only JDK built-in APIs. This minimizes dependency conflicts and reduces security vulnerabilities.

### Protocol v2 with Method Overloading

Use Protocol v2 to support method overloading and proper exception handling:

```java
// Service with overloaded methods
public interface Calculator {
    int add(int a, int b);           // Integer addition
    double add(double a, double b);   // Double addition
    String add(String a, String b);   // String concatenation
}

// Client (opt-in to v2)
TcpRestClientFactory factory = new TcpRestClientFactory(
    Calculator.class, "localhost", 8001
)
    .withProtocolV2();  // Enable Protocol v2

Calculator calc = factory.getClient();

calc.add(5, 3);         // Calls int add(int, int) → 8
calc.add(2.5, 3.5);     // Calls double add(double, double) → 6.0
calc.add("Hello", "!"); // Calls String add(String, String) → "Hello!"
```

**Server Configuration:**
```java
TcpRestServer server = new SingleThreadTcpRestServer(8001);
server.setProtocolVersion(ProtocolVersion.AUTO);  // Default: accept both v1 and v2
server.addSingletonResource(new CalculatorImpl());
server.up();
```

### Exception Handling (Protocol v2)

With Protocol v2, exceptions are properly propagated to the client:

```java
// Server-side service
public class UserService {
    public void validateAge(int age) {
        if (age < 0) {
            throw new ValidationException("Age must be non-negative");
        }
    }
}

// Client receives the exception
try {
    userService.validateAge(-1);
} catch (RuntimeException e) {
    // Exception message: "ValidationException: Age must be non-negative"
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
SSLParam sslParam = new SSLParam();
sslParam.setKeyStorePath("classpath:server_ks");
sslParam.setKeyStoreKeyPass("password");

// Secure server on localhost only
TcpRestServer server = new SingleThreadTcpRestServer(8443, "127.0.0.1", sslParam);
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
| `SingleThreadTcpRestServer` | tcprest-core | Low traffic, simple deployment | ✅ Yes | ✅ Yes | ✅ Yes |
| `NioTcpRestServer` | tcprest-core | Medium traffic, non-blocking I/O | ❌ No | ✅ Yes | ✅ Yes |
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
import cn.huiwings.tcprest.ssl.SSLParam;

SSLParam sslParam = new SSLParam();
sslParam.setKeyStorePath("classpath:server_ks");
sslParam.setKeyStoreKeyPass("password");
sslParam.setTrustStorePath("classpath:server_ks");
sslParam.setNeedClientAuth(true);  // Optional: mutual TLS

TcpRestServer server = new NettyTcpRestServer(8443, sslParam);
server.addSingletonResource(new MyServiceImpl());
server.up();
```

**With Bind Address (Security Best Practice):**
```java
// Bind to specific IP for security
TcpRestServer server = new NettyTcpRestServer(8001, "127.0.0.1");

// Or combine with SSL
TcpRestServer server = new NettyTcpRestServer(8443, "192.168.1.100", sslParam);
```

**With Protocol v2:**
```java
import cn.huiwings.tcprest.protocol.ProtocolVersion;

TcpRestServer server = new NettyTcpRestServer(8001);
server.setProtocolVersion(ProtocolVersion.V2);  // Enable method overloading and exceptions
server.addSingletonResource(new MyServiceImpl());
server.up();
```

**Complete Production Example:**
```java
// Production-ready setup: Netty + SSL + localhost binding + Protocol v2
SSLParam sslParam = new SSLParam();
sslParam.setKeyStorePath("classpath:server_ks");
sslParam.setKeyStoreKeyPass("password");
sslParam.setTrustStorePath("classpath:server_ks");

TcpRestServer server = new NettyTcpRestServer(8443, "127.0.0.1", sslParam);
server.setProtocolVersion(ProtocolVersion.V2);
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
factory.getProtocolConfig().setVersion(ProtocolVersion.V2);
UserService client = factory.getClient();
```

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

### Complex Service Example

Here's a realistic example demonstrating advanced features with Protocol v2:

```java
/**
 * Shopping cart service with method overloading and exception handling.
 */
public interface ShoppingCartService {
    // Cart operations
    int createCart(String customerId);
    boolean clearCart(int cartId);

    // Method overloading - different signatures
    boolean addProduct(int cartId, String name, double price, int quantity);
    boolean addProduct(int cartId, String name, double price);  // quantity=1

    // More overloading
    boolean updateQuantity(int cartId, String product, int newQuantity);
    boolean updateQuantity(int cartId, String product, int delta, boolean increment);

    // Calculations
    double getTotal(int cartId);
    int getItemCount(int cartId);
    double applyDiscount(int cartId, double percent);
}

/**
 * Implementation with validation and business rules.
 */
public class ShoppingCartServiceImpl implements ShoppingCartService {
    private static final double MAX_CART_VALUE = 10000.0;
    private final Map<Integer, Cart> carts = new ConcurrentHashMap<>();

    @Override
    public boolean addProduct(int cartId, String name, double price, int qty) {
        // Validation
        if (price <= 0) {
            throw new ValidationException("Price must be positive");
        }
        if (qty <= 0) {
            throw new ValidationException("Quantity must be positive");
        }

        Cart cart = getCart(cartId);  // Throws if not found

        // Business rule check
        if (cart.getTotal() + (price * qty) > MAX_CART_VALUE) {
            throw new BusinessException("Cart value exceeds maximum: " + MAX_CART_VALUE);
        }

        cart.addProduct(name, price, qty);
        return true;
    }

    @Override
    public boolean addProduct(int cartId, String name, double price) {
        return addProduct(cartId, name, price, 1);  // Default quantity
    }

    // ... other methods
}
```

**Server Setup:**
```java
TcpRestServer server = new NettyTcpRestServer(8001);
server.setProtocolVersion(ProtocolVersion.V2);  // Required for overloading
server.addSingletonResource(new ShoppingCartServiceImpl());
server.up();
```

**Client Usage:**
```java
TcpRestClientFactory factory = new TcpRestClientFactory(
    ShoppingCartService.class, "localhost", 8001
).withProtocolV2();

ShoppingCartService cart = factory.getClient();

// Create cart and add products
int cartId = cart.createCart("customer123");
cart.addProduct(cartId, "laptop", 1200.0, 1);     // Full signature
cart.addProduct(cartId, "mouse", 25.0);           // Default quantity=1

// Calculate total
double total = cart.getTotal(cartId);  // 1225.0

// Apply discount
double discounted = cart.applyDiscount(cartId, 10.0);  // 1102.5

// Exception handling works automatically
try {
    cart.addProduct(cartId, "bad", -10.0, 1);
} catch (RuntimeException e) {
    // Server exception is propagated to client
    System.out.println(e.getMessage());  // "ValidationException: Price must be positive"
}
```

**See Full Example:** [`ProtocolV2IntegrationTest.java`](tcprest-core/src/test/java/cn/huiwings/tcprest/test/integration/ProtocolV2IntegrationTest.java) for complete working code with tests.

**Pro Tip:** In this example, if your `Cart` and `Product` classes implement `Serializable`, you don't need any custom mappers - TcpRest will handle serialization automatically, including proper handling of `transient` fields.

### Automatic Serialization (Recommended)

**TcpRest automatically handles any class implementing `Serializable` - no mapper needed!**

```java
import java.io.Serializable;

// Simply implement Serializable
public class User implements Serializable {
    private int id;
    private String name;
    private transient String password;  // Excluded from serialization

    // Getters/setters...
}

// Works automatically - no mapper registration required!
public interface UserService {
    User getUser(int id);
    List<User> getAllUsers();  // Collections work too
}
```

**What's supported automatically:**
- ✅ Any class implementing `Serializable`
- ✅ Collections (`List`, `Map`, `Set`) - they're already Serializable
- ✅ Arrays of any type
- ✅ `transient` fields (automatically excluded)
- ✅ Nested Serializable objects
- ⚠️ Generic types (runtime type erasure applies, but works via Serializable)

**When to use custom mappers:**
- More efficient string representation (e.g., CSV format)
- Human-readable wire format
- Non-Serializable classes you can't modify

### Custom Data Types with Mappers

For more control, implement custom mappers:

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
        return p.getName() + "," + p.getAge();
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

## Migration to Protocol v2

Protocol v2 adds support for method overloading and exception handling. Migration is straightforward:

**Phase 1: Update Server** (backward compatible)
```java
// Server accepts both v1 and v2 clients (default)
TcpRestServer server = new SingleThreadTcpRestServer(8001);
server.setProtocolVersion(ProtocolVersion.AUTO);  // or omit, AUTO is default
```

**Phase 2: Update Clients** (gradual)
```java
// Opt-in to v2 per client
TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8001
)
    .withProtocolV2();  // Add this line
```

**Phase 3: V2-Only** (optional, after all clients upgraded)
```java
server.setProtocolVersion(ProtocolVersion.V2);  // Reject v1 clients
```

**Rollback:** Remove `.withProtocolV2()` to revert clients to v1.

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

# Verify zero dependencies in core
mvn dependency:tree -pl tcprest-core
```

## Examples

See the `src/test/java` directories for comprehensive examples:
- **Basic usage**: `cn.huiwings.tcprest.test.smoke.*`
- **Protocol v2**: `cn.huiwings.tcprest.test.integration.ProtocolV2IntegrationTest`
- **Backward compatibility**: `cn.huiwings.tcprest.test.integration.BackwardCompatibilityTest`
- **Compression**: `cn.huiwings.tcprest.test.compression.*`
- **SSL**: `cn.huiwings.tcprest.test.ssl.*`

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
```

#### Security Features

| Feature | Description | Use Case |
|---------|-------------|----------|
| **Full Encoding** | Base64-encodes all protocol components | Prevents all injection attacks |
| **CRC32 Checksum** | Fast integrity verification | Detect accidental corruption |
| **HMAC-SHA256** | Cryptographic message authentication | Prevent malicious tampering |
| **Class Whitelist** | Restrict accessible classes | Public API security |

#### Protection Against Attacks

✅ **Path Traversal** (`../../EvilClass`) - Base64 encoding prevents  
✅ **Delimiter Injection** (`Class/method()/evil`) - Structure protected  
✅ **Method Injection** (`method:::badParam`) - Cannot inject  
✅ **Message Tampering** - HMAC detects modifications  
✅ **Unauthorized Access** - Whitelist restricts classes

**📖 Full Documentation:** See [SECURITY-PROTOCOL.md](SECURITY-PROTOCOL.md) for complete security guide.

**⚠️ Breaking Change:** The security-enhanced protocol is NOT backward compatible. Migration guide available in security documentation.

