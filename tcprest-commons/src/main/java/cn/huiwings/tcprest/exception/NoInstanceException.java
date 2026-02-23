package cn.huiwings.tcprest.exception;

/**
 * Thrown when no service instances are available for a given service name
 * (e.g. {@link cn.huiwings.tcprest.discovery.ServiceDiscovery#getInstances(String)} returned empty).
 *
 * @since 2.0.0
 */
public class NoInstanceException extends RuntimeException {

    private final String serviceName;

    public NoInstanceException(String serviceName) {
        super("No instances available for service: " + serviceName);
        this.serviceName = serviceName;
    }

    public NoInstanceException(String serviceName, Throwable cause) {
        super("No instances available for service: " + serviceName, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
