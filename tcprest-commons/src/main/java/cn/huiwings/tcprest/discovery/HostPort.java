package cn.huiwings.tcprest.discovery;

import java.util.Objects;

/**
 * Immutable value object representing a single service instance address (host and port).
 * Used by {@link ServiceDiscovery} and {@link LoadBalancer} for service resolution.
 *
 * @since 2.0.0
 */
public final class HostPort {

    private final String host;
    private final int port;

    public HostPort(String host, int port) {
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HostPort hostPort = (HostPort) o;
        return port == hostPort.port && host.equals(hostPort.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
