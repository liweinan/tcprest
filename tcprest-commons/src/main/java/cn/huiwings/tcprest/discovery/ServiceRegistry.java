package cn.huiwings.tcprest.discovery;

/**
 * Registry for service instances. Servers call {@link #register(String, String, int)} after
 * binding a port and {@link #deregister(String, String, int)} before shutdown.
 *
 * @since 2.0.0
 */
public interface ServiceRegistry {

    /**
     * Register a service instance.
     *
     * @param serviceName logical service name
     * @param host        advertised host (e.g. "localhost" or external IP)
     * @param port        server port
     */
    void register(String serviceName, String host, int port);

    /**
     * Deregister a service instance.
     *
     * @param serviceName logical service name
     * @param host        same host used in {@link #register}
     * @param port        same port used in {@link #register}
     */
    void deregister(String serviceName, String host, int port);
}
