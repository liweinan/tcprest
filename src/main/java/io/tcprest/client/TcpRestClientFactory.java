package io.tcprest.client;

import io.tcprest.mapper.Mapper;
import io.tcprest.ssl.SSLParam;

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

    public <T> T getInstance() {
        return (T) Proxy.newProxyInstance(resourceClass.getClassLoader(),
                new Class[]{resourceClass}, new TcpRestClientProxy(resourceClass.getCanonicalName(), host, port, extraMappers, sslParam));

    }

}
