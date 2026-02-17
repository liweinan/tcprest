package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.mapper.Mapper;
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
                new Class[]{resourceClass}, new TcpRestClientProxy(resourceClass.getCanonicalName(), host, port, extraMappers, sslParam, compressionConfig));

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

}
