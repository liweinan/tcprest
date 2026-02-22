package cn.huiwings.tcprest.client;

import java.lang.reflect.Proxy;

/**
 * Factory for creating UDP-backed RPC client proxies using Netty.
 *
 * <p>Only interfaces may be registered. Each request/response is one UDP datagram.
 * No SSL/DTLS. Use when the server is {@link cn.huiwings.tcprest.server.NettyUdpRestServer}.</p>
 *
 * <p>Example:</p>
 * <pre>
 * NettyUdpRestClientFactory factory = new NettyUdpRestClientFactory(
 *     HelloWorld.class, "localhost", 9090);
 * HelloWorld client = factory.getClient();
 * String result = client.helloWorld();
 * factory.shutdown(); // release Netty event loop
 * </pre>
 */
public class NettyUdpRestClientFactory {

    private final Class<?> interfaceClass;
    private final String host;
    private final int port;
    private NettyUdpRestClient udpClient;

    public NettyUdpRestClientFactory(Class<?> interfaceClass, String host, int port) {
        if (interfaceClass == null || !interfaceClass.isInterface()) {
            throw new IllegalArgumentException("interfaceClass must be a non-null interface");
        }
        this.interfaceClass = interfaceClass;
        this.host = host;
        this.port = port;
    }

    /**
     * Create and return a client proxy. The first call creates the underlying UDP client.
     *
     * @param <T> interface type
     * @return proxy implementing the interface
     */
    @SuppressWarnings("unchecked")
    public <T> T getClient() {
        if (udpClient == null) {
            try {
                udpClient = new NettyUdpRestClient(interfaceClass.getCanonicalName(), host, port);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Failed to create UDP client", e);
            }
        }
        TcpRestClientProxy proxy = new TcpRestClientProxy(
                interfaceClass.getCanonicalName(),
                udpClient,
                null, null, null, null);
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                new Class<?>[]{interfaceClass},
                proxy);
    }

    /**
     * Release the UDP client and event loop. Call when done using the client(s).
     */
    public void shutdown() {
        if (udpClient != null) {
            udpClient.shutdown();
            udpClient = null;
        }
    }
}
