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
     * Path to SSL keystore (for mutual TLS).
     * Supports property placeholders: ${tcprest.ssl.keystore}
     */
    String sslKeyStore() default "";

    /**
     * SSL keystore password.
     * Supports property placeholders: ${tcprest.ssl.password}
     */
    String sslPassword() default "";

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
 * <p><b>CDI Usage:</b></p>
 * <pre>
 * &#64;TcpRestServer(
 *     port = 8001,
 *     serverType = ServerType.NETTY,
 *     resources = {UserServiceImpl.class, OrderServiceImpl.class}
 * )
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         // CDI container will start server automatically
 *         SeContainer container = SeContainerInitializer.newInstance().initialize();
 *     }
 * }
 * </pre>
 *
 * <p><b>Spring Usage:</b></p>
 * <pre>
 * &#64;Configuration
 * &#64;TcpRestServer(port = 8001, serverType = ServerType.NETTY)
 * public class TcpRestConfig {
 *     &#64;Bean
 *     public UserServiceImpl userService() {
 *         return new UserServiceImpl();
 *     }
 * }
 * </pre>
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
     */
    ServerType serverType() default ServerType.NETTY;

    /**
     * Protocol version (V1, V2, AUTO).
     */
    String protocol() default "AUTO";

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
     */
    boolean ssl() default false;

    /**
     * Path to SSL keystore.
     * Supports property placeholders: ${tcprest.server.ssl.keystore}
     */
    String sslKeyStore() default "";

    /**
     * SSL keystore password.
     * Supports property placeholders: ${tcprest.server.ssl.password}
     */
    String sslPassword() default "";

    /**
     * Require client authentication (mutual TLS).
     */
    boolean requireClientAuth() default false;

    /**
     * Server type enumeration.
     */
    enum ServerType {
        SINGLE_THREAD,
        NIO,
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

// 3. Server configuration
@TcpRestServer(
    port = 8001,
    serverType = ServerType.NETTY,
    resources = {UserServiceImpl.class}
)
public class ServerApp {
    public static void main(String[] args) {
        SeContainer container = SeContainerInitializer.newInstance().initialize();
        // Server starts automatically
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

// 3. Server configuration
@Configuration
@TcpRestServer(port = 8001, serverType = ServerType.NETTY)
public class TcpRestConfig {

    @Bean
    public UserServiceImpl userService() {
        return new UserServiceImpl();
    }
}

// 4. application.properties
tcprest.host=localhost
tcprest.port=8001
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
4. ⚠️ Server auto-start - Should server start automatically or require explicit trigger?
5. ⚠️ Multiple clients - How to handle multiple @TcpRestClient for same interface (different servers)?

---

**Note:** This is a proposal document. Implementation will follow after review and approval.
