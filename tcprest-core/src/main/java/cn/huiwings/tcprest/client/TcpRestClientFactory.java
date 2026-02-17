package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.protocol.ProtocolVersion;
import cn.huiwings.tcprest.ssl.SSLParam;

import java.lang.reflect.Proxy;
import java.util.Map;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class TcpRestClientFactory {

    Class<?> resourceClass;
    String host;
    int port;
    Map<String, Mapper> extraMappers;
    SSLParam sslParam;
    CompressionConfig compressionConfig;
    ProtocolConfig protocolConfig = new ProtocolConfig(); // Default: V1

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

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParam = sslParam;
    }

    public TcpRestClientFactory(Class<?> resourceClass, String host, int port, Map<String, Mapper> extraMappers, SSLParam sslParam, CompressionConfig compressionConfig) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
        this.extraMappers = extraMappers;
        this.sslParam = sslParam;
        this.compressionConfig = compressionConfig;
    }

    public <T> T getInstance() {
        return (T) Proxy.newProxyInstance(resourceClass.getClassLoader(),
                new Class[]{resourceClass}, new TcpRestClientProxy(resourceClass.getCanonicalName(), host, port, extraMappers, sslParam, compressionConfig, protocolConfig));

    }

    /**
     * Convenience method to get client instance.
     * Alias for getInstance().
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
     * Enable compression with default settings
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
     * Enable compression with custom configuration
     */
    public TcpRestClientFactory withCompression(CompressionConfig config) {
        this.compressionConfig = config;
        return this;
    }

    /**
     * Get protocol configuration.
     *
     * @return protocol configuration
     */
    public ProtocolConfig getProtocolConfig() {
        return protocolConfig;
    }

    /**
     * Set protocol configuration.
     *
     * @param protocolConfig protocol configuration
     */
    public void setProtocolConfig(ProtocolConfig protocolConfig) {
        this.protocolConfig = protocolConfig != null ? protocolConfig : new ProtocolConfig();
    }

    /**
     * Enable Protocol v2.
     *
     * @return this factory for chaining
     */
    public TcpRestClientFactory withProtocolV2() {
        this.protocolConfig.setVersion(ProtocolVersion.V2);
        return this;
    }

    /**
     * Set protocol version.
     *
     * @param version protocol version
     * @return this factory for chaining
     */
    public TcpRestClientFactory withProtocolVersion(ProtocolVersion version) {
        this.protocolConfig.setVersion(version);
        return this;
    }

}
