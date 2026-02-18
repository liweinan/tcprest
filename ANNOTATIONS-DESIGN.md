# TcpRest Annotations Support Design

> **Design Date:** 2026-02-19
> **Status:** Proposal
> **Author:** TcpRest Team

## Overview

Add declarative annotation support for TcpRest to simplify client and server configuration, following the principle of **zero dependencies in core modules**.

## Architecture Principles

### 1. Zero-Dependency Core

```
tcprest-commons (0 dependencies)
  └── annotations package (annotation definitions only, no processing logic)

tcprest-cdi (depends on: tcprest-commons + CDI API)
  └── CDI-specific annotation processors

tcprest-spring (depends on: tcprest-commons + Spring Framework)
  └── Spring-specific annotation processors
```

### 2. Module Structure

```
tcprest-parent/
├── tcprest-commons/           # Annotation definitions only
│   └── src/main/java/
│       └── cn/huiwings/tcprest/annotations/
│           ├── Timeout.java             # ✅ Already exists
│           ├── TcpRestClient.java       # NEW
│           └── TcpRestServer.java       # NEW
│
├── tcprest-cdi/              # NEW MODULE - CDI support
│   ├── pom.xml               # Dependencies: tcprest-commons + jakarta.enterprise.cdi-api
│   └── src/main/java/
│       └── cn/huiwings/tcprest/cdi/
│           ├── TcpRestClientProducer.java
│           ├── TcpRestServerManager.java
│           └── TcpRestExtension.java
│
└── tcprest-spring/           # NEW MODULE - Spring support
    ├── pom.xml               # Dependencies: tcprest-commons + spring-context
    └── src/main/java/
        └── cn/huiwings/tcprest/spring/
            ├── TcpRestClientFactoryBean.java
            ├── TcpRestServerConfiguration.java
            └── EnableTcpRest.java
```

## Annotation Specifications

### 1. @TcpRestClient Annotation

**Location:** `tcprest-commons` (definition only)

```java
package cn.huiwings.tcprest.annotations;

import java.lang.annotation.*;

/**
 * Declares an interface as a TcpRest client with automatic proxy creation.
 *
 * <p><b>CDI Usage:</b></p>
 * <pre>
 * &#64;TcpRestClient(host = "localhost", port = 8001)
 * public interface UserService {
 *     User getUser(int id);
 * }
 *
 * // Injection
 * &#64;Inject
 * UserService userService;
 * </pre>
 *
 * <p><b>Spring Usage:</b></p>
 * <pre>
 * &#64;TcpRestClient(host = "localhost", port = 8001)
 * public interface UserService {
 *     User getUser(int id);
 * }
 *
 * // Injection
 * &#64;Autowired
 * UserService userService;
 * </pre>
 *
 * <p><b>With Security (HMAC):</b></p>
 * <pre>
 * &#64;TcpRestClient(
 *     host = "${tcprest.host}",
 *     port = 8001,
 *     checksumAlgorithm = "HMAC_SHA256",
 *     hmacSecret = "${tcprest.security.secret}"
 * )
 * public interface SecureService {
 *     SensitiveData getData(String id);
 * }
 * </pre>
 *
 * <p><b>With SSL/TLS:</b></p>
 * <pre>
 * &#64;TcpRestClient(
 *     host = "api.example.com",
 *     port = 8443,
 *     ssl = true,
 *     sslKeyStore = "classpath:client.jks",
 *     sslPassword = "${ssl.password}",
 *     sslTrustStore = "classpath:truststore.jks",
 *     sslTrustStorePassword = "${ssl.truststore.password}"
 * )
 * public interface SslService {
 *     Response secureCall(Request req);
 * }
 * </pre>
 *
 * @since 2.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TcpRestClient {
    /**
     * Server host address.
     * Supports property placeholders: ${tcprest.host}
     */
    String host() default "localhost";

    /**
     * Server port.
     * Supports property placeholders: ${tcprest.port}
     */
    int port() default 8001;

    /**
     * Protocol version (V1, V2, AUTO).
     */
    String protocol() default "V2";

    /**
     * Enable compression.
     */
    boolean compression() default false;

    /**
     * Default timeout in seconds for all methods (0 = no timeout).
     * Can be overridden by method-level @Timeout.
     */
    int timeout() default 0;

    /**
     * Enable SSL/TLS.
     */
    boolean ssl() default false;

    /**
     * Path to SSL keystore (for client certificate in mutual TLS).
     * Supports property placeholders: ${tcprest.ssl.keystore}
     */
    String sslKeyStore() default "";

    /**
     * SSL keystore password.
     * Supports property placeholders: ${tcprest.ssl.password}
     */
    String sslPassword() default "";

    /**
     * Path to SSL truststore (for server certificate verification).
     * Supports property placeholders: ${tcprest.ssl.truststore}
     */
    String sslTrustStore() default "";

    /**
     * SSL truststore password.
     * Supports property placeholders: ${tcprest.ssl.truststore.password}
     */
    String sslTrustStorePassword() default "";

    /**
     * Checksum algorithm for message integrity (NONE, CRC32, HMAC_SHA256).
     * <ul>
     *   <li>NONE: No checksum (default)</li>
     *   <li>CRC32: Fast integrity check (development)</li>
     *   <li>HMAC_SHA256: Cryptographic authentication (production)</li>
     * </ul>
     */
    String checksumAlgorithm() default "NONE";

    /**
     * HMAC secret key (required when checksumAlgorithm = HMAC_SHA256).
     * Must be at least 32 characters for security.
     * Supports property placeholders: ${tcprest.security.hmac.secret}
     */
    String hmacSecret() default "";

    /**
     * Optional qualifier name for multiple clients of same interface.
     */
    String name() default "";
}
```

### 2. @TcpRestServer Annotation

**Location:** `tcprest-commons` (definition only)

```java
package cn.huiwings.tcprest.annotations;

import java.lang.annotation.*;

/**
 * Declares a class as a TcpRest server with automatic configuration.
 *
 * <p><b>CDI Usage (Simple - All Defaults):</b></p>
 * <pre>
 * // Uses defaults: SINGLE_THREAD, V2, no SSL, no checksum
 * &#64;TcpRestServer(
 *     port = 8001,
 *     resources = {UserServiceImpl.class, OrderServiceImpl.class}
 * )
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         SeContainer container = SeContainerInitializer.newInstance().initialize();
 *     }
 * }
 * </pre>
 *
 * <p><b>Spring Usage (Simple - All Defaults):</b></p>
 * <pre>
 * &#64;Configuration
 * &#64;TcpRestServer(port = 8001)  // Uses defaults: SINGLE_THREAD, V2
 * public class TcpRestConfig {
 *     &#64;Bean
 *     public UserServiceImpl userService() {
 *         return new UserServiceImpl();
 *     }
 * }
 * </pre>
 *
 * <p><b>High-Performance Production:</b></p>
 * <pre>
 * &#64;TcpRestServer(
 *     port = 8001,
 *     serverType = ServerType.NETTY,  // Override for high concurrency
 *     checksumAlgorithm = "HMAC_SHA256",
 *     hmacSecret = "${tcprest.server.security.secret}",
 *     enableClassWhitelist = true,
 *     allowedClasses = {"com.example.api.*"}
 * )
 * public class ProductionServerConfig { }
 * </pre>
 *
 * <p><b>With SSL/TLS (Production):</b></p>
 * <pre>
 * &#64;TcpRestServer(
 *     port = 8443,
 *     serverType = ServerType.NETTY,  // Override: SSL requires NETTY for performance
 *     ssl = true,
 *     sslKeyStore = "classpath:server.jks",
 *     sslPassword = "${ssl.password}",
 *     requireClientAuth = true,
 *     sslTrustStore = "classpath:truststore.jks",
 *     sslTrustStorePassword = "${ssl.truststore.password}",
 *     compression = true,
 *     checksumAlgorithm = "HMAC_SHA256",
 *     hmacSecret = "${tcprest.server.security.secret}"
 * )
 * public class ProductionServerConfig { }
 * </pre>
 *
 * <p><b>Note:</b> SSL is only supported with SINGLE_THREAD and NETTY server types.
 * NIO does not support SSL due to technical limitations.</p>
 *
 * @since 2.1.0
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TcpRestServer {
    /**
     * Server port.
     * Supports property placeholders: ${tcprest.server.port}
     */
    int port() default 8001;

    /**
     * Bind address (null = all interfaces, "127.0.0.1" = localhost only).
     * Supports property placeholders: ${tcprest.server.bind}
     */
    String bindAddress() default "";

    /**
     * Server implementation type.
     * <p>Default: SINGLE_THREAD (simple, zero dependencies, SSL supported)</p>
     * <p>Use NETTY for high-concurrency production scenarios.</p>
     */
    ServerType serverType() default ServerType.SINGLE_THREAD;

    /**
     * Protocol version (V1, V2, AUTO).
     * <p>Default: V2 (recommended, supports method overloading and status codes)</p>
     * <p>Use AUTO to support both V1 and V2 clients.</p>
     */
    String protocol() default "V2";

    /**
     * Enable compression.
     */
    boolean compression() default false;

    /**
     * Resource classes to register (CDI only - for Spring use @Bean).
     */
    Class<?>[] resources() default {};

    /**
     * Enable SSL/TLS.
     * <p><b>Compatibility:</b></p>
     * <ul>
     *   <li>SINGLE_THREAD: ✅ Supported</li>
     *   <li>NIO: ❌ Not supported (technical limitation)</li>
     *   <li>NETTY: ✅ Supported</li>
     * </ul>
     */
    boolean ssl() default false;

    /**
     * Path to SSL keystore (server certificate).
     * Supports property placeholders: ${tcprest.server.ssl.keystore}
     */
    String sslKeyStore() default "";

    /**
     * SSL keystore password.
     * Supports property placeholders: ${tcprest.server.ssl.password}
     */
    String sslPassword() default "";

    /**
     * Path to SSL truststore (for client certificate verification in mutual TLS).
     * Supports property placeholders: ${tcprest.server.ssl.truststore}
     */
    String sslTrustStore() default "";

    /**
     * SSL truststore password.
     * Supports property placeholders: ${tcprest.server.ssl.truststore.password}
     */
    String sslTrustStorePassword() default "";

    /**
     * Require client authentication (mutual TLS).
     */
    boolean requireClientAuth() default false;

    /**
     * Checksum algorithm for message integrity (NONE, CRC32, HMAC_SHA256).
     * <ul>
     *   <li>NONE: No checksum (default)</li>
     *   <li>CRC32: Fast integrity check (development)</li>
     *   <li>HMAC_SHA256: Cryptographic authentication (production)</li>
     * </ul>
     */
    String checksumAlgorithm() default "NONE";

    /**
     * HMAC secret key (required when checksumAlgorithm = HMAC_SHA256).
     * Must match client-side secret. Minimum 32 characters recommended.
     * Supports property placeholders: ${tcprest.server.security.hmac.secret}
     */
    String hmacSecret() default "";

    /**
     * Enable class whitelist for security.
     * When enabled, only classes in allowedClasses can be invoked.
     */
    boolean enableClassWhitelist() default false;

    /**
     * Allowed class patterns (supports wildcards).
     * Examples: "com.example.api.*", "com.example.UserService"
     * Supports property placeholders: ${tcprest.server.security.whitelist}
     */
    String[] allowedClasses() default {};

    /**
     * Server type enumeration.
     */
    enum ServerType {
        /**
         * Single-threaded blocking I/O server.
         * <p><b>Features:</b> SSL ✅, Low concurrency, Development/Testing</p>
         */
        SINGLE_THREAD,

        /**
         * NIO non-blocking I/O server.
         * <p><b>Features:</b> SSL ❌, Medium concurrency, No external dependencies</p>
         */
        NIO,

        /**
         * Netty high-performance server.
         * <p><b>Features:</b> SSL ✅, High concurrency, Production recommended</p>
         */
        NETTY
    }
}
```

### 3. @Timeout Annotation Enhancement

**Already exists** in `tcprest-commons`. Usage scenarios:

#### Scenario 1: Slow Operations
```java
public interface ReportService {
    @Timeout(second = 30)
    Report generateMonthlyReport();  // May take up to 30 seconds

    @Timeout(second = 5)
    Report getReportStatus(String id);  // Fast operation
}
```

#### Scenario 2: External API Calls
```java
public interface PaymentGateway {
    @Timeout(second = 10)
    PaymentResult processPayment(Payment payment);  // External API timeout

    String getTransactionId();  // No timeout (instant)
}
```

#### Scenario 3: Different Timeouts per Method
```java
public interface DataService {
    @Timeout(second = 1)
    CachedData getFast();  // Cache hit - should be instant

    @Timeout(second = 60)
    FullData getComplete();  // Database query - may be slow
}
```

#### Scenario 4: Combined with @TcpRestClient
```java
@TcpRestClient(
    host = "localhost",
    port = 8001,
    timeout = 5  // Default timeout for all methods
)
public interface UserService {
    User getUser(int id);  // Uses default 5 seconds

    @Timeout(second = 30)
    List<User> exportAllUsers();  // Override: 30 seconds
}
```

## CDI Support Module (`tcprest-cdi`)

### Dependencies (pom.xml)
```xml
<dependencies>
    <dependency>
        <groupId>cn.huiwings</groupId>
        <artifactId>tcprest-commons</artifactId>
        <version>${project.version}</version>
    </dependency>

    <dependency>
        <groupId>jakarta.enterprise</groupId>
        <artifactId>jakarta.enterprise.cdi-api</artifactId>
        <version>4.0.1</version>
        <scope>provided</scope>
    </dependency>

    <!-- Test dependency: Weld SE for testing -->
    <dependency>
        <groupId>org.jboss.weld.se</groupId>
        <artifactId>weld-se-core</artifactId>
        <version>5.1.0.Final</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### Implementation

#### 1. Client Producer
```java
package cn.huiwings.tcprest.cdi;

import cn.huiwings.tcprest.annotations.TcpRestClient;
import cn.huiwings.tcprest.client.TcpRestClientFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;

@ApplicationScoped
public class TcpRestClientProducer {

    @Produces
    @TcpRestClient
    public <T> T createClient(InjectionPoint injectionPoint) {
        TcpRestClient annotation = injectionPoint.getAnnotated()
            .getAnnotation(TcpRestClient.class);

        Class<T> clientInterface = (Class<T>) injectionPoint.getType();

        TcpRestClientFactory<T> factory = new TcpRestClientFactory<>(
            clientInterface,
            resolveProperty(annotation.host()),
            resolvePort(annotation.port())
        );

        // Configure protocol, compression, SSL, etc.
        configureFactory(factory, annotation);

        return factory.getClient();
    }

    private String resolveProperty(String value) {
        // Resolve ${...} placeholders
        // Implementation depends on CDI property resolution
        return value;
    }

    private void configureFactory(TcpRestClientFactory<?> factory,
                                   TcpRestClient annotation) {
        // Apply configuration from annotation
    }
}
```

#### 2. Server Manager
```java
package cn.huiwings.tcprest.cdi;

import cn.huiwings.tcprest.annotations.TcpRestServer;
import cn.huiwings.tcprest.server.TcpRestServer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.*;

@ApplicationScoped
public class TcpRestServerManager {

    private TcpRestServer server;

    public void startServer(@Observes @Initialized(ApplicationScoped.class) Object init,
                           BeanManager beanManager) {
        // Find @TcpRestServer annotated class
        // Create server instance
        // Register resources
        // Start server
    }

    public void stopServer(@Observes @BeforeDestroyed(ApplicationScoped.class) Object init) {
        if (server != null) {
            server.down();
        }
    }
}
```

### CDI Usage Example

```java
// 1. Define service interface
@TcpRestClient(host = "localhost", port = 8001, timeout = 5)
public interface UserService {
    User getUser(int id);

    @Timeout(second = 30)
    List<User> getAllUsers();
}

// 2. Application code
@ApplicationScoped
public class MyApp {

    @Inject
    @TcpRestClient
    UserService userService;

    public void doSomething() {
        User user = userService.getUser(123);
    }
}

// 3. Server configuration (simple - uses defaults)
@TcpRestServer(
    port = 8001,
    resources = {UserServiceImpl.class}
    // Defaults: SINGLE_THREAD, V2, no SSL, no checksum
)
public class ServerApp {
    public static void main(String[] args) {
        SeContainer container = SeContainerInitializer.newInstance().initialize();
        // Server starts automatically with SINGLE_THREAD
    }
}

// 3b. High-performance server (override for production)
@TcpRestServer(
    port = 8001,
    serverType = ServerType.NETTY,  // Override for high concurrency
    resources = {UserServiceImpl.class}
)
public class ProductionServerApp {
    public static void main(String[] args) {
        SeContainer container = SeContainerInitializer.newInstance().initialize();
        // Server starts with NETTY for better performance
    }
}
```

## Spring Support Module (`tcprest-spring`)

### Dependencies (pom.xml)
```xml
<dependencies>
    <dependency>
        <groupId>cn.huiwings</groupId>
        <artifactId>tcprest-commons</artifactId>
        <version>${project.version}</version>
    </dependency>

    <dependency>
        <groupId>org.springframework</groupId>
        <artifactId>spring-context</artifactId>
        <version>6.1.0</version>
        <scope>provided</scope>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
        <version>3.2.0</version>
        <scope>provided</scope>
        <optional>true</optional>
    </dependency>
</dependencies>
```

### Implementation

#### 1. Client Factory Bean
```java
package cn.huiwings.tcprest.spring;

import cn.huiwings.tcprest.annotations.TcpRestClient;
import cn.huiwings.tcprest.client.TcpRestClientFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;

public class TcpRestClientFactoryBean<T> implements FactoryBean<T>, EnvironmentAware {

    private final Class<T> clientInterface;
    private final TcpRestClient annotation;
    private Environment environment;

    @Override
    public T getObject() throws Exception {
        String host = environment.resolvePlaceholders(annotation.host());
        int port = resolvePort(annotation.port());

        TcpRestClientFactory<T> factory = new TcpRestClientFactory<>(
            clientInterface, host, port
        );

        // Configure from annotation
        return factory.getClient();
    }

    @Override
    public Class<?> getObjectType() {
        return clientInterface;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
```

#### 2. Server Configuration
```java
package cn.huiwings.tcprest.spring;

import cn.huiwings.tcprest.annotations.TcpRestServer;
import cn.huiwings.tcprest.server.NettyTcpRestServer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class TcpRestServerPostProcessor implements BeanPostProcessor, ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName)
            throws BeansException {

        TcpRestServer annotation = bean.getClass().getAnnotation(TcpRestServer.class);
        if (annotation != null) {
            // Create and configure server
            // Register beans as resources
            // Start server
        }

        return bean;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
```

#### 3. Auto-Configuration (Spring Boot)
```java
package cn.huiwings.tcprest.spring.boot;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(TcpRestClient.class)
public class TcpRestAutoConfiguration {
    // Auto-register processors and factories
}
```

### Spring Usage Example

```java
// 1. Define service interface
@TcpRestClient(
    host = "${tcprest.host}",  // Property placeholder
    port = 8001,
    timeout = 5
)
public interface UserService {
    User getUser(int id);
}

// 2. Application code
@Service
public class MyService {

    @Autowired
    UserService userService;  // Auto-injected

    public void doSomething() {
        User user = userService.getUser(123);
    }
}

// 3. Server configuration (simple - uses defaults)
@Configuration
@TcpRestServer(port = 8001)  // Defaults: SINGLE_THREAD, V2
public class TcpRestConfig {

    @Bean
    public UserServiceImpl userService() {
        return new UserServiceImpl();
    }
}

// 3b. For high-performance production, override server type
@Configuration
@TcpRestServer(port = 8001, serverType = ServerType.NETTY)
public class ProductionTcpRestConfig {

    @Bean
    public UserServiceImpl userService() {
        return new UserServiceImpl();
    }
}

// 4. application.properties
tcprest.host=localhost
tcprest.port=8001
```

## Server Implementation Compatibility

### Feature Support Matrix

Different server implementations have varying feature support. Choose the right server type based on your requirements:

| Feature | SINGLE_THREAD | NIO | NETTY |
|---------|---------------|-----|-------|
| **SSL/TLS** | ✅ Supported | ❌ Not supported | ✅ Supported |
| **Compression** | ✅ Supported | ✅ Supported | ✅ Supported |
| **Security (HMAC/CRC32)** | ✅ Supported | ✅ Supported | ✅ Supported |
| **Class Whitelist** | ✅ Supported | ✅ Supported | ✅ Supported |
| **Protocol V2** | ✅ Supported | ✅ Supported | ✅ Supported |
| **Max Throughput** | ~500 req/s | ~5,000 req/s | ~20,000 req/s |
| **Concurrency Model** | Single thread | Selector + pool | Event loop |
| **External Dependencies** | None | None | Netty 4.x |
| **Best For** | Dev/Testing | Medium load | Production |

### SSL/TLS Technical Limitation (NIO)

**Why NIO doesn't support SSL:**

Java NIO's `SocketChannel` doesn't support SSL/TLS out of the box. Implementing SSL with NIO requires:
- Using `SSLEngine` for manual encryption/decryption
- Complex buffer management for encrypted/decrypted data
- Handshake state machine implementation
- Proper handling of SSL buffer overflows/underflows

This adds ~500+ lines of complex code and is error-prone.

**Solution:** For SSL with NIO performance, use `NETTY` server type which has battle-tested SSL support.

### Compatibility Validation

The annotation processor should validate configuration at startup:

```java
// Example: Invalid configuration
@TcpRestServer(
    serverType = ServerType.NIO,
    ssl = true  // ❌ ERROR: NIO doesn't support SSL!
)
public class InvalidConfig { }
```

**Expected behavior:**
```
Exception in thread "main" cn.huiwings.tcprest.exception.ConfigurationException:
  SSL is not supported with NIO server. Please use SINGLE_THREAD or NETTY instead.

  Suggested fix:
    @TcpRestServer(serverType = ServerType.NETTY, ssl = true)
```

### Configuration Examples by Use Case

#### Development (Low Concurrency, No SSL)
```java
@TcpRestServer(
    port = 8001,
    serverType = ServerType.SINGLE_THREAD,  // Simple, easy debugging
    protocol = "V2"
)
```

#### Production (High Concurrency, No SSL)
```java
@TcpRestServer(
    port = 8001,
    serverType = ServerType.NIO,  // ✅ Good choice (no SSL needed)
    protocol = "AUTO",
    compression = true,
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "${tcprest.security.secret}"
)
```

#### Production (High Concurrency, SSL Required)
```java
@TcpRestServer(
    port = 8443,
    serverType = ServerType.NETTY,  // ✅ Only choice for SSL + performance
    protocol = "AUTO",
    ssl = true,
    sslKeyStore = "classpath:server.jks",
    sslPassword = "${ssl.password}",
    compression = true,
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "${tcprest.security.secret}",
    enableClassWhitelist = true,
    allowedClasses = {"com.example.api.*"}
)
```

#### Internal Microservices (Medium Concurrency)
```java
@TcpRestServer(
    port = 8001,
    serverType = ServerType.NIO,  // ✅ No deps, good performance
    bindAddress = "127.0.0.1",  // Localhost only
    protocol = "V2",
    checksumAlgorithm = "CRC32"  // Fast integrity check
)
```

#### Public API (High Security)
```java
@TcpRestServer(
    port = 8443,
    serverType = ServerType.NETTY,
    ssl = true,
    sslKeyStore = "classpath:server.jks",
    sslPassword = "${ssl.password}",
    requireClientAuth = true,  // Mutual TLS
    sslTrustStore = "classpath:truststore.jks",
    sslTrustStorePassword = "${ssl.truststore.password}",
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "${tcprest.security.secret}",
    enableClassWhitelist = true,
    allowedClasses = {
        "com.example.api.PublicUserService",
        "com.example.api.PublicOrderService"
    }
)
```

### Security Configuration Examples

#### Client with HMAC Authentication
```java
@TcpRestClient(
    host = "${tcprest.host}",
    port = 8001,
    protocol = "V2",
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "${tcprest.security.secret}",  // Must match server
    timeout = 5
)
public interface SecureUserService {
    User getUser(int id);
}
```

#### Client with Mutual TLS + HMAC (Maximum Security)
```java
@TcpRestClient(
    host = "${tcprest.host}",
    port = 8443,
    protocol = "V2",
    ssl = true,
    sslKeyStore = "classpath:client.jks",
    sslPassword = "${ssl.password}",
    sslTrustStore = "classpath:truststore.jks",
    sslTrustStorePassword = "${ssl.truststore.password}",
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "${tcprest.security.secret}",
    timeout = 10
)
public interface HighSecurityService {
    SensitiveData processSensitiveData(Request request);
}
```

#### Server with CRC32 (Development)
```java
@TcpRestServer(
    port = 8001,
    serverType = ServerType.SINGLE_THREAD,
    checksumAlgorithm = "CRC32",  // Fast, good for dev
    resources = {UserServiceImpl.class}
)
```

### Migration from Programmatic Configuration

**Before:**
```java
// Programmatic
SecurityConfig securityConfig = new SecurityConfig()
    .enableHMAC("secret-key-min-32-chars");

SSLParam sslParam = new SSLParam();
sslParam.setKeyStorePath("classpath:server.jks");
sslParam.setKeyStoreKeyPass("password");

NettyTcpRestServer server = new NettyTcpRestServer(8443, sslParam);
server.setSecurityConfig(securityConfig);
server.addSingletonResource(new UserServiceImpl());
server.up();
```

**After:**
```java
// Declarative
@TcpRestServer(
    port = 8443,
    serverType = ServerType.NETTY,
    ssl = true,
    sslKeyStore = "classpath:server.jks",
    sslPassword = "password",
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "secret-key-min-32-chars",
    resources = {UserServiceImpl.class}
)
public class ServerConfig { }
```

## Testing Strategy

### CDI Module Tests (using Weld SE)
```java
public class CdiClientTest {

    @Test
    public void testClientInjection() {
        SeContainer container = SeContainerInitializer.newInstance().initialize();

        MyApp app = container.select(MyApp.class).get();
        assertNotNull(app.userService);

        User user = app.userService.getUser(123);
        assertNotNull(user);
    }
}
```

### Spring Module Tests (using Spring Test)
```java
@SpringBootTest
public class SpringClientTest {

    @Autowired
    UserService userService;

    @Test
    public void testClientInjection() {
        assertNotNull(userService);
        User user = userService.getUser(123);
        assertNotNull(user);
    }
}
```

## Default Values and Rationale

### Design Philosophy: Start Simple, Scale Later

The default values are chosen to provide the **simplest, most reliable setup** for getting started:

| Annotation | Parameter | Default | Rationale |
|------------|-----------|---------|-----------|
| @TcpRestServer | serverType | SINGLE_THREAD | Zero dependencies, easy debugging, SSL supported |
| @TcpRestServer | protocol | V2 | Latest protocol with full features (not AUTO to avoid ambiguity) |
| @TcpRestServer | ssl | false | Security should be explicit opt-in |
| @TcpRestServer | checksumAlgorithm | NONE | Performance first, add security when needed |
| @TcpRestServer | compression | false | Avoid overhead for small messages |
| @TcpRestClient | protocol | V2 | Match server default |
| @TcpRestClient | ssl | false | Match server default |
| @TcpRestClient | checksumAlgorithm | NONE | Match server default |
| @TcpRestClient | timeout | 0 | No timeout (use @Timeout per method for fine control) |

### When to Override Defaults

**Use SINGLE_THREAD (default):**
- ✅ Development and testing
- ✅ Low-traffic internal services (<500 req/s)
- ✅ When SSL is required but you don't want Netty dependency
- ✅ Simple deployments with minimal dependencies

**Override to NIO:**
```java
@TcpRestServer(
    port = 8001,
    serverType = ServerType.NIO  // Override: Medium concurrency, no SSL
)
```
- ✅ Medium-traffic internal services (500-5K req/s)
- ✅ When SSL is NOT needed
- ✅ Want better performance than SINGLE_THREAD but avoid external dependencies

**Override to NETTY:**
```java
@TcpRestServer(
    port = 8443,
    serverType = ServerType.NETTY,  // Override: High concurrency or SSL
    ssl = true
)
```
- ✅ High-traffic production services (>5K req/s)
- ✅ When SSL is required AND high performance needed
- ✅ Public-facing APIs

**Override protocol to AUTO:**
```java
@TcpRestServer(
    port = 8001,
    protocol = "AUTO"  // Override: Support both V1 and V2 clients
)
```
- ✅ During migration period (V1 clients → V2)
- ✅ Public APIs with legacy clients
- ❌ **Not recommended for new projects** (use V2 for clarity)

**Enable security features:**
```java
@TcpRestServer(
    port = 8001,
    checksumAlgorithm = "HMAC_SHA256",  // Override: Add security
    hmacSecret = "${tcprest.security.secret}"
)
```
- ✅ Production environments
- ✅ When message integrity is critical
- ✅ Public APIs or untrusted networks

### Minimal Configuration Examples

**Simplest server (all defaults):**
```java
// Uses: SINGLE_THREAD, V2, no SSL, no checksum
@TcpRestServer(port = 8001)
public class SimpleServer {
    @Bean
    public UserServiceImpl userService() {
        return new UserServiceImpl();
    }
}
```

**Simplest client (all defaults):**
```java
// Uses: V2, no SSL, no checksum
@TcpRestClient(host = "localhost", port = 8001)
public interface UserService {
    User getUser(int id);
}
```

**High-performance production (override server type only):**
```java
@TcpRestServer(
    port = 8001,
    serverType = ServerType.NETTY  // Only override for performance
    // Still uses: V2, no SSL, no checksum (add security via HMAC instead of SSL)
)
```

**Secure production (override server type + security):**
```java
@TcpRestServer(
    port = 8443,
    serverType = ServerType.NETTY,
    ssl = true,
    sslKeyStore = "classpath:server.jks",
    sslPassword = "${ssl.password}",
    checksumAlgorithm = "HMAC_SHA256",
    hmacSecret = "${tcprest.security.secret}"
)
```

### Default Values Summary

**@TcpRestServer defaults:**
```java
port = 8001
serverType = SINGLE_THREAD  // Simple, debuggable, zero deps
protocol = "V2"             // Latest protocol
ssl = false                 // Security explicit opt-in
checksumAlgorithm = "NONE"  // Performance first
compression = false         // No overhead by default
enableClassWhitelist = false
```

**@TcpRestClient defaults:**
```java
host = "localhost"
port = 8001
protocol = "V2"             // Match server
ssl = false                 // Match server
checksumAlgorithm = "NONE"  // Match server
timeout = 0                 // No default timeout
compression = false         // Match server
```

**Matching principle:** Client and server defaults are designed to work together without configuration. Override security/performance settings explicitly based on requirements.

## Migration Path

### Phase 1: Core Annotations (tcprest-commons)
- ✅ Keep @Timeout (already exists)
- Add @TcpRestClient (definition only)
- Add @TcpRestServer (definition only)

### Phase 2: CDI Support Module
- Create tcprest-cdi module
- Implement client producer
- Implement server manager
- Add Weld SE tests

### Phase 3: Spring Support Module
- Create tcprest-spring module
- Implement factory beans
- Implement auto-configuration
- Add Spring Boot starter

### Phase 4: Documentation
- Update README with annotation examples
- Add ANNOTATIONS.md guide
- Add migration guide from programmatic to declarative

## Benefits

### For Users
- ✅ **Less boilerplate** - No manual factory creation
- ✅ **CDI/Spring integration** - Use standard @Inject/@Autowired
- ✅ **Property placeholders** - Externalize configuration
- ✅ **Type-safe** - Compile-time checking
- ✅ **Zero config** - Sensible defaults

### For TcpRest
- ✅ **Zero-dependency core** - Annotations are just markers
- ✅ **Framework agnostic** - Support multiple DI containers
- ✅ **Maintainable** - Clear separation of concerns
- ✅ **Testable** - Can test with Weld/Spring Test

## Example Comparison

### Before (Programmatic)
```java
public class MyApp {
    private UserService userService;

    public void init() {
        TcpRestClientFactory<UserService> factory = new TcpRestClientFactory<>(
            UserService.class, "localhost", 8001
        );
        factory.withProtocolV2();
        factory.withTimeout(5, TimeUnit.SECONDS);
        this.userService = factory.getClient();
    }

    public void doWork() {
        User user = userService.getUser(123);
    }
}
```

### After (Declarative - CDI)
```java
@ApplicationScoped
public class MyApp {

    @Inject
    @TcpRestClient(host = "localhost", port = 8001, timeout = 5)
    UserService userService;

    public void doWork() {
        User user = userService.getUser(123);
    }
}
```

### After (Declarative - Spring)
```java
@Service
public class MyApp {

    @Autowired
    UserService userService;  // Configured via @TcpRestClient on interface

    public void doWork() {
        User user = userService.getUser(123);
    }
}
```

## Next Steps

1. **Review this design** - Confirm architecture and approach
2. **Create modules** - Add tcprest-cdi and tcprest-spring to parent pom
3. **Implement annotations** - Add to tcprest-commons
4. **Implement CDI support** - Start with client producer
5. **Add tests** - Verify with Weld SE
6. **Implement Spring support** - Add factory beans
7. **Document** - Update README and add ANNOTATIONS.md

## Questions for Review

1. ✅ Module structure - Separate cdi/spring modules OK?
2. ✅ Annotation attributes - Sufficient configuration options?
3. ✅ Property placeholders - ${...} syntax OK for both CDI/Spring?
4. ✅ Security parameters - HMAC secret, checksum algorithm, class whitelist included
5. ✅ SSL parameters - Keystore, truststore, passwords for both client and server
6. ✅ Server compatibility - Feature matrix and validation documented
7. ⚠️ Server auto-start - Should server start automatically or require explicit trigger?
8. ⚠️ Multiple clients - How to handle multiple @TcpRestClient for same interface (different servers)?
9. ⚠️ NIO + SSL validation - Should annotation processor fail fast at startup or runtime?
10. ⚠️ HMAC secret validation - Should we validate minimum length (32 chars) at startup?

---

**Note:** This is a proposal document. Implementation will follow after review and approval.
