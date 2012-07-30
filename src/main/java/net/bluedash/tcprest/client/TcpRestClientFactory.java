package net.bluedash.tcprest.client;

import java.lang.reflect.Proxy;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class TcpRestClientFactory {

    Class resourceClass;
    String host;
    int port;

    public TcpRestClientFactory(Class resourceClass, String host, int port) {
        this.resourceClass = resourceClass;
        this.host = host;
        this.port = port;
    }

    public Object getInstance() {
        return Proxy.newProxyInstance(resourceClass.getClassLoader(),
                new Class[]{resourceClass}, new TcpRestClientProxy(resourceClass.getCanonicalName(), host, port));

    }

}
