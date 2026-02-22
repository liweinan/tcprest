package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.ssl.SSLParams;

import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * Factory for creating Protocol V2 TcpRest client instances.
 *
 * <p>Only <b>interfaces</b> may be registered; passing a concrete class will throw
 * {@link IllegalArgumentException}. Supports both single-interface and multi-interface
 * registration. When multiple interfaces are registered, use {@link #getInstance(Class)}
 * to obtain a proxy for a specific interface.</p>
 *
 * <p><b>Single interface (basic):</b></p>
 * <pre>
 * Calculator calc = new TcpRestClientFactory(Calculator.class, "localhost", 8080)
 *     .getInstance();
 * </pre>
 *
 * <p><b>Multiple interfaces (varargs):</b></p>
 * <pre>
 * TcpRestClientFactory factory = new TcpRestClientFactory(
 *     "localhost", 8080, Calculator.class, ExceptionService.class);
 * Calculator calc = factory.getInstance(Calculator.class);
 * ExceptionService svc = factory.getInstance(ExceptionService.class);
 * </pre>
 *
 * <p><b>With Compression:</b></p>
 * <pre>
 * Calculator calc = new TcpRestClientFactory(Calculator.class, "localhost", 8080)
 *     .withCompression()
 *     .getInstance();
 * </pre>
 *
 * <p><b>With SSL:</b></p>
 * <pre>
 * SSLParam sslParam = new SSLParam();
 * sslParam.setKeyStorePath("keystore.jks");
 * Calculator calc = new TcpRestClientFactory(Calculator.class, "localhost", 8443, null, sslParam)
 *     .getInstance();
 * </pre>
 *
 * @author Weinan Li
 * @date 07 31 2012
 */
public class TcpRestClientFactory {

    private final Class<?>[] resourceClasses;
    String host;
    int port;
    Map<String, Mapper> extraMappers;
    SSLParams sslParams;
    CompressionConfig compressionConfig;
    SecurityConfig securityConfig;

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port) {
        this.resourceClasses = new Class<?>[]{validateInterface(resourceClass)};
        this.host = host;
        this.port = port;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers) {
        this.resourceClasses = new Class<?>[]{validateInterface(resourceClass)};
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams) {
        this.resourceClasses = new Class<?>[]{validateInterface(resourceClass)};
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams, CompressionConfig compressionConfig) {
        this.resourceClasses = new Class<?>[]{validateInterface(resourceClass)};
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
        this.compressionConfig = compressionConfig;
    }

    private static Class<?> validateInterface(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("resourceClass must not be null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("resourceClass must be an interface: " + type.getName());
        }
        return type;
    }

    public TcpRestClientFactory(Class<?>[] resourceClasses, String host, int port) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
    }

    public TcpRestClientFactory(Class<?>[] resourceClasses, String host, int port, Map<String, Mapper> extraMappers) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
    }

    public TcpRestClientFactory(Class<?>[] resourceClasses, String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
    }

    public TcpRestClientFactory(Class<?>[] resourceClasses, String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams, CompressionConfig compressionConfig) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
        this.compressionConfig = compressionConfig;
    }

    /**
     * Multi-interface constructor (varargs). Registers all given interface classes.
     *
     * @param host server host
     * @param port server port
     * @param resourceClasses one or more interface classes (varargs)
     * @throws IllegalArgumentException if resourceClasses is null, empty, or any element is not an interface
     */
    public TcpRestClientFactory(String host, int port, Class<?>... resourceClasses) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
    }

    /**
     * Multi-interface constructor (varargs) with custom mappers.
     */
    public TcpRestClientFactory(String host, int port, Map<String, Mapper> extraMappers, Class<?>... resourceClasses) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
    }

    /**
     * Multi-interface constructor (varargs) with custom mappers and SSL.
     */
    public TcpRestClientFactory(String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams, Class<?>... resourceClasses) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
    }

    /**
     * Multi-interface constructor (varargs) with full configuration.
     */
    public TcpRestClientFactory(String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams, CompressionConfig compressionConfig, Class<?>... resourceClasses) {
        this.resourceClasses = validateResourceClasses(resourceClasses);
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
        this.compressionConfig = compressionConfig;
    }

    private static Class<?>[] validateResourceClasses(Class<?>[] classes) {
        if (classes == null || classes.length == 0) {
            throw new IllegalArgumentException("resourceClasses must not be null or empty");
        }
        for (int i = 0; i < classes.length; i++) {
            if (classes[i] == null) {
                throw new IllegalArgumentException("resourceClasses[" + i + "] must not be null");
            }
            if (!classes[i].isInterface()) {
                throw new IllegalArgumentException("resourceClasses[" + i + "] must be an interface: " + classes[i].getName());
            }
        }
        return classes.clone();
    }

    /**
     * Create and return client instance. When only one interface is registered,
     * returns the proxy for that interface. When multiple interfaces are registered,
     * throws IllegalStateException; use {@link #getInstance(Class)} instead.
     *
     * @param <T> client interface type
     * @return client proxy instance
     * @throws IllegalStateException if multiple interfaces are registered
     */
    public <T> T getInstance() {
        if (resourceClasses.length != 1) {
            throw new IllegalStateException("Multiple interfaces registered; use getInstance(Class<T>) to get a proxy.");
        }
        return (T) createProxy(resourceClasses[0]);
    }

    /**
     * Create and return client instance for the given interface type.
     * The type must be one of the interfaces registered with this factory.
     *
     * @param <T> client interface type
     * @param type the interface class to get a proxy for
     * @return client proxy instance for the given type
     * @throws IllegalArgumentException if type is not registered with this factory
     */
    public <T> T getInstance(Class<T> type) {
        if (type == null || !type.isInterface()) {
            throw new IllegalArgumentException("type must be a non-null interface");
        }
        for (Class<?> c : resourceClasses) {
            if (c == type) {
                return (T) createProxy(type);
            }
        }
        throw new IllegalArgumentException("Interface not registered with this factory: " + type.getName());
    }

    private Object createProxy(Class<?> type) {
        return Proxy.newProxyInstance(type.getClassLoader(),
                new Class<?>[]{type},
                new TcpRestClientProxy(type.getCanonicalName(), host, port,
                        extraMappers, sslParams, compressionConfig, securityConfig));
    }

    /**
     * Convenience method to get client instance.
     * Alias for getInstance().
     *
     * @param <T> client interface type
     * @return client proxy instance
     * @throws IllegalStateException if multiple interfaces are registered
     */
    public <T> T getClient() {
        return getInstance();
    }

    /**
     * Get client instance for the given interface type.
     * Alias for getInstance(Class).
     *
     * @param <T> client interface type
     * @param type the interface class to get a proxy for
     * @return client proxy instance for the given type
     * @throws IllegalArgumentException if type is not registered with this factory
     */
    public <T> T getClient(Class<T> type) {
        return getInstance(type);
    }

    public void setCompressionConfig(CompressionConfig compressionConfig) {
        this.compressionConfig = compressionConfig;
    }

    public CompressionConfig getCompressionConfig() {
        return compressionConfig;
    }

    /**
     * Enable compression with default settings.
     *
     * @return this factory for chaining
     */
    public TcpRestClientFactory withCompression() {
        if (this.compressionConfig == null) {
            this.compressionConfig = new CompressionConfig(true);
        } else {
            this.compressionConfig.setEnabled(true);
        }
        return this;
    }

    /**
     * Enable compression with custom configuration.
     *
     * @param config compression configuration
     * @return this factory for chaining
     */
    public TcpRestClientFactory withCompression(CompressionConfig config) {
        this.compressionConfig = config;
        return this;
    }

    /**
     * Set security configuration.
     *
     * @param securityConfig security configuration
     * @return this factory for chaining
     */
    public TcpRestClientFactory withSecurity(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
        return this;
    }

    /**
     * Get security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    /**
     * Set security configuration.
     *
     * @param securityConfig security configuration
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig;
    }
}
