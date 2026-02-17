# TcpRest Architecture

## Overview

TcpRest is a lightweight TCP-based RPC framework that transforms Plain Old Java Objects (POJOs) into network-accessible services. The framework provides a simple annotation-driven approach to expose Java methods over TCP connections.

## Multi-Module Architecture

The project is organized into two Maven modules:

### tcprest-core
**Zero-dependency core module** containing all essential framework functionality using pure JDK implementations.

**Key components:**
- Server implementations (SingleThread, NIO)
- Client factory and proxy
- Protocol layer
- Serialization/deserialization (mappers)
- Annotations
- Extractors and invokers

**Dependencies:** None (only TestNG in test scope)

### tcprest-netty
**Optional high-performance module** providing Netty-based server implementation.

**Key components:**
- NettyTcpRestServer
- NettyTcpRestProtocolHandler

**Dependencies:**
- tcprest-core
- io.netty:netty:3.10.6.Final

## Core Components

### 1. Protocol Layer

**Package:** `cn.huiwings.tcprest.protocol`

The protocol layer defines the wire format for method invocations:

```
ClassName/methodName(param1<sep>param2<sep>...)
```

**Key classes:**
- `TcpRestProtocol`: Defines protocol constants and parsing rules
- Protocol separators: `/` for class/method, custom separator for parameters

### 2. Server Layer

**Package:** `cn.huiwings.tcprest.server`

#### AbstractTcpRestServer
Base class providing common functionality:
- Resource management (classes and singleton instances)
- Mapper registry
- Request processing pipeline
- Extractor and invoker integration

#### Server Implementations

**SingleThreadTcpRestServer** (`tcprest-core`)
- Uses traditional blocking I/O with ServerSocket
- Single-threaded request handling
- Best for: Development, testing, low-concurrency scenarios
- Thread model: One thread accepting connections sequentially
- Lifecycle: Properly shuts down with port release and thread termination

**NioTcpRestServer** (`tcprest-core`)
- Uses Java NIO with Selector
- Non-blocking I/O with worker thread pool
- Best for: Moderate concurrency without external dependencies
- Thread model: One selector thread + cached thread pool for request processing
- Lifecycle: Properly closes selector and all channels on shutdown

**NettyTcpRestServer** (`tcprest-netty`)
- Uses Netty 3.x framework
- High-performance async I/O
- Best for: High-concurrency production scenarios
- Thread model: Netty's event loop model with configurable thread pools
- Requires: Netty dependency

#### Lifecycle Management

All servers implement the `TcpRestServer` interface:

```java
void up();                    // Start server
void up(boolean setDaemon);   // Start with daemon flag
void down();                  // Shutdown server
int getServerPort();          // Get listening port
```

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
MyService client = factory.produceProxy();
String result = client.someMethod(arg1, arg2);
```

**Key classes:**
- `TcpRestClientFactory`: Factory for creating client proxies
- `TcpRestClientProxy`: InvocationHandler that serializes calls to protocol format
- Supports custom mappers and SSL

### 4. Serialization Layer

**Package:** `cn.huiwings.tcprest.mapper`

**Mapper interface** handles conversion between Java objects and string representation:

```java
public interface Mapper {
    String encode(Object o);
    Object decode(String s, Class returnType);
}
```

**Built-in mappers:**
- `StringMapper`: For String types
- `IntegerMapper`: For Integer/int types
- `LongMapper`: For Long/long types
- `BooleanMapper`: For Boolean/boolean types
- `DoubleMapper`: For Double/double types
- And more for primitive types and wrappers

**Custom mappers:**
Users can implement `Mapper` interface and register with server/client for custom types.

**Thread safety:** Mappers should be stateless or thread-safe.

### 5. Extractor and Invoker

**Package:** `cn.huiwings.tcprest.extractor`, `cn.huiwings.tcprest.invoker`

**Extractor** parses protocol strings into invocation context:
- `DefaultExtractor`: Parses class name, method name, and parameters
- Creates `Context` object with target class, method, and decoded parameters

**Invoker** executes the method invocation:
- `DefaultInvoker`: Uses reflection to invoke target method
- Handles both class instantiation and singleton resources

### 6. Annotations

**Package:** `cn.huiwings.tcprest.annotations`

- `@TcpRestMethod`: Mark methods for remote access
- `@Singleton`: Mark resources as singleton (one instance per server)

### 7. Compression Layer

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

Custom exceptions for framework errors:
- `ParseException`: Protocol parsing errors
- `MapperNotFoundException`: No mapper found for type
- `TcpRestException`: Base exception class

## Extension Points

### 1. Custom Mappers

Implement `Mapper` interface for custom serialization:

```java
public class ColorMapper implements Mapper {
    public String encode(Object o) {
        Color c = (Color) o;
        return c.getRed() + "," + c.getGreen() + "," + c.getBlue();
    }

    public Object decode(String s, Class returnType) {
        String[] parts = s.split(",");
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

### 2. Custom Extractors

Implement `Extractor` interface for custom protocol parsing:

```java
public class MyExtractor implements Extractor {
    public Context extract(String request) {
        // Custom parsing logic
    }
}
```

### 3. Custom Invokers

Implement `Invoker` interface for custom invocation logic:

```java
public class MyInvoker implements Invoker {
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
SSLParam sslParam = new SSLParam();
sslParam.setKeyStore("/path/to/keystore");
sslParam.setKeyStorePassword("password");

TcpRestServer server = new SingleThreadTcpRestServer(8080, sslParam);
```

### Input Validation

- Always validate input parameters in service methods
- Use type-safe mappers to prevent injection attacks
- Implement authentication/authorization in custom invokers if needed

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

### From Single Module to Multi-Module

**If you only need core features:**
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-core</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

**If you need Netty server:**
```xml
<dependency>
    <groupId>cn.huiwings</groupId>
    <artifactId>tcprest-netty</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

Note: tcprest-netty automatically includes tcprest-core as a transitive dependency.

## Testing

### Unit Testing
- Use `SingleThreadTcpRestServer` for simple test scenarios
- Call `server.down()` in @AfterMethod to ensure proper cleanup
- Use random ports to avoid conflicts: `Math.abs(new Random().nextInt()) % 10000 + 8000`

### Integration Testing
- Test server shutdown behavior
- Verify port release and restart capabilities
- Test error handling and recovery

### Load Testing
- Use `NettyTcpRestServer` for load tests
- Monitor thread usage and memory consumption
- Test with concurrent clients

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

```java
// 1. Define custom type
public class Person {
    String name;
    int age;
}

// 2. Implement mapper
public class PersonMapper implements Mapper {
    public String encode(Object o) {
        Person p = (Person) o;
        return p.name + "|" + p.age;
    }

    public Object decode(String s, Class returnType) {
        String[] parts = s.split("\\|");
        Person p = new Person();
        p.name = parts[0];
        p.age = Integer.parseInt(parts[1]);
        return p;
    }
}

// 3. Register mapper
server.addMapper("com.example.Person", new PersonMapper());

// 4. Use in service
public class PersonService {
    @TcpRestMethod
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

## Future Enhancements

Potential areas for improvement:
- Asynchronous client API
- Upgrade to Netty 4.x
- HTTP/2 or WebSocket support
- Service discovery integration
- Metrics and monitoring hooks
- Circuit breaker pattern
- Distributed tracing support
