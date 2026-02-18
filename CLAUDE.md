# TcpRest Development Guidelines for Claude

This document outlines core design principles and constraints that must be maintained during future development of TcpRest.

## Core Design Principles

### 1. Zero-Dependency Commons Module (CRITICAL)

**Rule:** The `tcprest-commons` module MUST have ZERO runtime dependencies.

**Requirements:**
- ✅ Only JDK built-in packages may be used (`java.*`, `javax.*`)
- ✅ Test dependencies (e.g., TestNG, SLF4J-Simple) are allowed in `<scope>test</scope>` only
- ❌ NO external libraries in compile or runtime scope
- ❌ NO Apache Commons, Guava, Jackson, or any third-party libraries

**Rationale:**
- Minimizes dependency conflicts for users
- Reduces attack surface and CVE exposure
- Ensures lightweight deployable artifact
- Simplifies dependency management

**Verification:**
```bash
# This command should show ZERO compile/runtime dependencies
mvn dependency:tree -pl tcprest-commons
```

**Allowed JDK packages for common tasks:**
- Compression: `java.util.zip.GZIPInputStream/GZIPOutputStream`
- Base64: `java.util.Base64`
- SSL/TLS: `javax.net.ssl.*`
- Networking: `java.net.*`, `java.nio.*`
- Serialization: `java.io.Serializable`
- Collections: `java.util.*`
- Concurrency: `java.util.concurrent.*`

### 2. Modular Architecture with Separation of Concerns

**Rule:** External dependencies belong in separate modules with clear naming. Server implementations are isolated in dedicated modules.

**Current structure:**
```
tcprest-parent/
├── tcprest-commons/       # Zero dependencies - client, protocol, utilities
├── tcprest-singlethread/  # Depends on: tcprest-commons (blocking I/O server)
├── tcprest-nio/           # Depends on: tcprest-commons (NIO server)
└── tcprest-netty/         # Depends on: tcprest-commons + Netty (high-perf server)
```

**Benefits:**
- `tcprest-commons` remains minimal and zero-dependency
- Server implementations are cleanly separated
- Users include only what they need (client-only apps use commons only)
- Each module has focused tests for its specific functionality

**Future extensions:**
```
tcprest-jackson/           # Optional JSON support
tcprest-metrics/           # Optional metrics (Micrometer)
tcprest-spring/            # Optional Spring integration
```

### 3. Backward Compatibility

**Rule:** All protocol changes MUST be backward compatible.

**Requirements:**
- New protocol features must include version/prefix markers
- Old clients must work with new servers and vice versa
- Default behavior should match legacy behavior

**Example:**
```java
// Compression feature uses prefix for backward compatibility
"0|" + data  // Uncompressed (works with old clients)
"1|" + data  // Compressed (new feature)
```

### 4. Modular Feature Design

**Rule:** New features should be optional and configurable.

**Pattern:**
```java
// Feature disabled by default
private CompressionConfig config = new CompressionConfig(); // disabled

// Explicit opt-in
server.enableCompression();
server.setCompressionConfig(new CompressionConfig(true, 1024, 9));
```

**Anti-pattern:**
```java
// DON'T: Feature always enabled
private CompressionConfig config = new CompressionConfig(true);
```

### 5. SSL/TLS Support

**Rule:** Server implementations SHOULD support SSL/TLS when technically feasible using JDK built-in SSL.

**Current SSL Support Status:**

| Component | SSL Support | Implementation |
|-----------|-------------|----------------|
| SingleThreadTcpRestServer | ✅ Yes | `javax.net.ssl.SSLServerSocketFactory` |
| NioTcpRestServer | ❌ No | Technical limitation (see below) |
| NettyTcpRestServer | ✅ Yes | Netty SSL handler |
| DefaultTcpRestClient | ✅ Yes | `javax.net.ssl.SSLSocketFactory` |

**NioTcpRestServer SSL Limitation:**

Java NIO's `SocketChannel` doesn't support SSL directly. Implementing SSL with NIO requires:
- Using `SSLEngine` for encryption/decryption
- Manual buffer management for encrypted/decrypted data
- Complex handshake state machine
- Proper handling of SSL buffer overflows/underflows

This adds ~500+ lines of complex code and makes the implementation error-prone.

**Recommendation:** For SSL with NIO performance, use `NettyTcpRestServer` which has battle-tested SSL support.

**SSL Implementation Requirements (for supported servers):**
- Use `javax.net.ssl.*` (JDK built-in, zero dependencies)
- Support both plain and SSL sockets
- SSL should be optional (configured via `SSLParam`)
- Test coverage for SSL scenarios
- Support both server and client authentication

**SSL Configuration Example:**
```java
// Server side
SSLParam serverParam = new SSLParam();
serverParam.setKeyStorePath("classpath:server_ks");
serverParam.setKeyStoreKeyPass("password");
serverParam.setTrustStorePath("classpath:server_ks");
serverParam.setNeedClientAuth(true); // Optional: require client cert

TcpRestServer server = new SingleThreadTcpRestServer(8443, serverParam);
server.up();

// Client side
SSLParam clientParam = new SSLParam();
clientParam.setKeyStorePath("classpath:client_ks");
clientParam.setKeyStoreKeyPass("password");
clientParam.setTrustStorePath("classpath:client_ks");
clientParam.setNeedClientAuth(true);

TcpRestClientFactory factory = new TcpRestClientFactory(
    MyService.class, "localhost", 8443, null, clientParam
);
```

**SSL Test Requirements:**
- Basic SSL handshake test
- Mutual authentication (client cert) test
- SSL with compression test (if applicable)
- Proper cleanup and port release test

### 6. Proper Resource Management

**Rule:** All resources (sockets, threads, streams) MUST be properly closed.

**Requirements:**
- Servers must support graceful shutdown within timeout (5 seconds)
- Use try-with-resources for AutoCloseable resources
- Interrupt threads before closing resources
- Release ports for restart

**Pattern:**
```java
public void down() {
    status = TcpRestServerStatus.CLOSING;

    // 1. Interrupt threads
    if (workerThread != null) {
        workerThread.interrupt();
    }

    // 2. Close server socket
    if (serverSocket != null && !serverSocket.isClosed()) {
        try {
            serverSocket.close();
        } catch (IOException e) {
            logger.error("Error closing socket", e);
        }
    }

    // 3. Wait for termination with timeout
    if (workerThread != null) {
        try {
            workerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 7. Thread Safety

**Rule:** Shared mutable state MUST be properly synchronized.

**Requirements:**
- Use `synchronized` blocks for shared collections
- Use `volatile` for visibility across threads
- Document thread-safety guarantees in JavaDoc
- Prefer immutable objects where possible

### 8. Logging

**Rule:** Use abstracted logger interface, not specific logging framework.

**Requirements:**
- Use `cn.huiwings.tcprest.logger.Logger` interface
- NO direct dependency on Log4j, SLF4J, Logback in core
- Provide simple implementations (SystemOutLogger, NullLogger)

### 9. Testing

**Rule:** All features must have comprehensive test coverage.

**Requirements:**
- Unit tests for individual components
- Integration tests for client-server communication
- Shutdown tests (verify proper cleanup)
- Performance/benchmark tests for features like compression
- SSL tests for secure communication

**Test structure:**
```
src/test/java/cn/huiwings/tcprest/test/
├── compression/          # Compression tests
├── shutdown/             # Shutdown behavior tests
├── errorhandling/        # Error handling tests
├── ssl/                  # SSL/TLS tests
└── smoke/                # End-to-end smoke tests
```

#### 9.1 TestNG @Factory Pattern Guidelines (CRITICAL)

**Problem:** Using `@Factory` to test multiple server implementations can cause test failures due to improper lifecycle management.

**Rules for @Factory tests:**

1. **Use @BeforeClass/@AfterClass, NOT @BeforeMethod/@AfterMethod**

   ❌ **WRONG - Causes multiple restarts:**
   ```java
   @BeforeMethod  // BAD: Restarts server for EVERY test method
   public void startServer() {
       server.up();
   }

   @AfterMethod   // BAD: Stops server after EVERY test method
   public void stopServer() {
       server.down();
   }
   ```

   ✅ **CORRECT - One startup per instance:**
   ```java
   @BeforeClass   // GOOD: Starts server once per @Factory instance
   public void startServer() throws Exception {
       tcpRestServer.up();
       // Delay for async servers (NioTcpRestServer, NettyTcpRestServer)
       Thread.sleep(500);
   }

   @AfterClass    // GOOD: Stops server once per @Factory instance
   public void stopServer() throws Exception {
       tcpRestServer.down();
       // Wait for port release
       Thread.sleep(300);
   }
   ```

2. **Add startup/shutdown delays for async servers**

   Async servers (NioTcpRestServer, NettyTcpRestServer) start worker threads that may not be immediately ready:

   ```java
   @BeforeClass
   public void startServer() throws Exception {
       tcpRestServer.up();
       Thread.sleep(500);  // CRITICAL: Wait for async server to be fully ready
   }

   @AfterClass
   public void stopServer() throws Exception {
       tcpRestServer.down();
       Thread.sleep(300);  // CRITICAL: Wait for port to be released
   }
   ```

3. **Use testng.xml for strict sequential execution**

   Create `src/test/resources/testng.xml`:
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

   Configure surefire to use it:
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

4. **Avoid port conflicts across modules**

   Each module should use a different port range:

   ```java
   // tcprest-commons/PortGenerator.java
   private static final AtomicInteger counter = new AtomicInteger(8000);

   // tcprest-singlethread/PortGenerator.java
   private static final AtomicInteger counter = new AtomicInteger(14000);

   // tcprest-nio/PortGenerator.java
   private static final AtomicInteger counter = new AtomicInteger(16000);

   // tcprest-netty/PortGenerator.java
   private static final AtomicInteger counter = new AtomicInteger(20000);
   ```

   **Use AtomicInteger, NOT Random** to ensure predictable, non-colliding ports:

   ❌ **WRONG:**
   ```java
   public static int get() {
       return Math.abs((new Random()).nextInt()) % 10000 + 8000;  // Can collide!
   }
   ```

   ✅ **CORRECT:**
   ```java
   private static final AtomicInteger counter = new AtomicInteger(8000);

   public static int get() {
       return counter.getAndIncrement();  // Guaranteed unique
   }
   ```

5. **Example: Proper @Factory test class**

   ```java
   public class MapperSmokeTest {
       protected TcpRestServer tcpRestServer;

       public MapperSmokeTest(TcpRestServer tcpRestServer) {
           this.tcpRestServer = tcpRestServer;
       }

       @Factory
       public static Object[] create() throws Exception {
           List result = new ArrayList();
           result.add(new MapperSmokeTest(new SingleThreadTcpRestServer(PortGenerator.get())));
           result.add(new MapperSmokeTest(new NioTcpRestServer(PortGenerator.get())));
           result.add(new MapperSmokeTest(new NettyTcpRestServer(PortGenerator.get())));
           return result.toArray();
       }

       @BeforeClass  // NOT @BeforeMethod!
       public void startTcpRestServer() throws Exception {
           tcpRestServer.up();
           Thread.sleep(500);  // Wait for async startup
       }

       @AfterClass   // NOT @AfterMethod!
       public void stopTcpRestServer() throws Exception {
           tcpRestServer.down();
           Thread.sleep(300);  // Wait for port release
       }

       @Test
       public void testFeature() {
           // Test implementation
       }
   }
   ```

#### 9.2 Common Test Failures and Solutions

| Symptom | Root Cause | Solution |
|---------|-----------|----------|
| "Connection refused" on some runs | Server not fully started | Add `Thread.sleep(500)` after `server.up()` |
| "Address already in use" | Port not released from previous test | Add `Thread.sleep(300)` after `server.down()` |
| Random test failures (Run 1, 3 fail; Run 2 passes) | @Factory instances executing concurrently | Use `@BeforeClass/@AfterClass` + `testng.xml` with `group-by-instances="true"` |
| Tests pass individually, fail together | Test concurrency issues | Configure surefire with `<parallel>none</parallel>` |
| Port conflicts between modules | Same port range in different modules | Use different base ports (8000 for core, 20000 for netty) |

### 10. Documentation

**Rule:** All public APIs must be documented.

**Requirements:**
- JavaDoc for all public classes, methods, and interfaces
- README.md for user-facing documentation
- ARCHITECTURE.md for design documentation
- Code examples in documentation
- Migration guides for breaking changes

## Pre-Commit Checklist

Before committing changes, verify:

- [ ] `tcprest-commons` has zero runtime dependencies
- [ ] All tests pass: `mvn test`
- [ ] Build succeeds: `mvn clean install`
- [ ] No deprecated APIs introduced
- [ ] Backward compatibility maintained
- [ ] Documentation updated (README, ARCHITECTURE, JavaDoc)
- [ ] Proper resource cleanup (no leaks)
- [ ] Thread-safety considered
- [ ] SSL support verified if touching network code

## Dependency Verification Commands

```bash
# Verify zero dependencies in commons
mvn dependency:tree -pl tcprest-commons | grep -v test

# Should output only:
# cn.huiwings:tcprest-commons:jar:1.0-SNAPSHOT
# (no compile/runtime dependencies)

# Verify singlethread module has only commons dependency
mvn dependency:tree -pl tcprest-singlethread | grep -E "tcprest-commons"

# Should show:
# +- cn.huiwings:tcprest-commons:jar:1.0-SNAPSHOT:compile

# Verify nio module has only commons dependency
mvn dependency:tree -pl tcprest-nio | grep -E "tcprest-commons"

# Should show:
# +- cn.huiwings:tcprest-commons:jar:1.0-SNAPSHOT:compile

# Verify Netty module has correct dependencies
mvn dependency:tree -pl tcprest-netty | grep -E "tcprest-commons|netty"

# Should show:
# +- cn.huiwings:tcprest-commons:jar:1.0-SNAPSHOT:compile
# +- io.netty:netty-all:jar:4.1.131.Final:compile
```

## Common Patterns

### Adding a New Feature

1. **Design Phase:**
   - Ensure feature can be implemented with JDK built-in APIs
   - If requires external dependency, create new module
   - Design with backward compatibility in mind
   - Make feature optional (disabled by default)

2. **Implementation:**
   - Add configuration class (e.g., `FeatureConfig`)
   - Add utility/helper classes
   - Integrate into server and client
   - Add enable/disable/configure methods

3. **Testing:**
   - Unit tests for utilities
   - Integration tests for client-server
   - Backward compatibility tests
   - Performance benchmarks if applicable

4. **Documentation:**
   - Update README.md with usage examples
   - Update ARCHITECTURE.md with design details
   - Add JavaDoc to all public APIs

### Example: Adding New Server Implementation

```java
// Must extend AbstractTcpRestServer
public class MyNewServer extends AbstractTcpRestServer {

    // Must support SSL via constructor
    public MyNewServer(int port, SSLParam sslParam) throws Exception {
        // Implementation
    }

    // Must implement lifecycle methods
    public void up() { }
    public void up(boolean setDaemon) { }
    public void down() {
        // MUST: Proper cleanup within 5 seconds
    }

    // Must implement port getter
    public int getServerPort() { }
}
```

## Anti-Patterns to Avoid

❌ **Don't add dependencies to commons:**
```xml
<!-- BAD: Adding dependency to tcprest-commons -->
<dependency>
    <groupId>com.google.guava</groupId>
    <artifactId>guava</artifactId>
</dependency>
```

✅ **Do create separate module:**
```xml
<!-- GOOD: Create tcprest-guava module -->
<modules>
    <module>tcprest-commons</module>
    <module>tcprest-singlethread</module>
    <module>tcprest-nio</module>
    <module>tcprest-netty</module>
    <module>tcprest-guava</module>
</modules>
```

❌ **Don't break backward compatibility:**
```java
// BAD: Changing protocol without compatibility layer
return compressedData; // Old clients will fail!
```

✅ **Do add version/prefix markers:**
```java
// GOOD: Prefix allows detection
return "1|" + compressedData; // New format
return "0|" + data;            // Old format (fallback)
```

❌ **Don't leak resources:**
```java
// BAD: Server socket never closed
public void down() {
    status = TcpRestServerStatus.CLOSING;
}
```

✅ **Do cleanup properly:**
```java
// GOOD: Close socket and wait for thread
public void down() {
    status = TcpRestServerStatus.CLOSING;
    serverSocket.close();
    serverThread.join(5000);
}
```

## Version History

- 2026-02-18: Major module refactoring - split tcprest-core into 4 focused modules (tcprest-commons, tcprest-singlethread, tcprest-nio, tcprest-netty)
- 2026-02-17: Initial version documenting zero-dependency principle and core guidelines
