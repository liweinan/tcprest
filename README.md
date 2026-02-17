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

### Server Implementations

TcpRest provides three server implementations:

| Server | Module | Best For | SSL Support |
|--------|--------|----------|-------------|
| `SingleThreadTcpRestServer` | tcprest-core | Low traffic, simple deployment | ✅ Yes |
| `NioTcpRestServer` | tcprest-core | Medium traffic, non-blocking I/O | ❌ No |
| `NettyTcpRestServer` | tcprest-netty | High traffic, production systems | ✅ Yes |

**Example:**
```java
// High-performance Netty server
TcpRestServer server = new NettyTcpRestServer(8001);
server.addSingletonResource(new MyServiceImpl());
server.up();
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

### Custom Data Types with Mappers

Handle custom types by registering mappers:

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
