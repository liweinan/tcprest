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
 * <p>Provides a fluent API for configuring and creating client proxies.</p>
 *
 * <p><b>Basic Usage:</b></p>
 * <pre>
 * Calculator calc = new TcpRestClientFactory(Calculator.class, "localhost", 8080)
 *     .getInstance();
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

    Class<?> resourceClass;
    String host;
    int port;
    Map<String, Mapper> extraMappers;
    SSLParams sslParams;
    CompressionConfig compressionConfig;
    SecurityConfig securityConfig;

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers, SSLParams sslParams, CompressionConfig compressionConfig) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParams = sslParams;
        this.compressionConfig = compressionConfig;
    }

    /**
     * Create and return client instance.
     *
     * @param <T> client interface type
     * @return client proxy instance
     */
    public <T> T getInstance() {
        return (T) Proxy.newProxyInstance(resourceClass.getClassLoader(),
                new Class[]{resourceClass},
                new TcpRestClientProxy(resourceClass.getCanonicalName(), host, port,
                        extraMappers, sslParams, compressionConfig, securityConfig));
    }

    /**
     * Convenience method to get client instance.
     * Alias for getInstance().
     *
     * @param <T> client interface type
     * @return client proxy instance
     */
    public <T> T getClient() {
        return getInstance();
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
