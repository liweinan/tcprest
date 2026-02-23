package cn.huiwings.tcprest.discovery;

import java.util.List;

/**
 * Discovery for service instances. Clients use this to resolve a service name to a list of
 * {@link HostPort} instances, then use a {@link LoadBalancer} to select one per request.
 *
 * @since 2.0.0
 */
public interface ServiceDiscovery {

    /**
     * Get current instances for the given service name.
     *
     * @param serviceName logical service name
     * @return list of instances (may be empty, must not be null)
     */
    List<HostPort> getInstances(String serviceName);
}
