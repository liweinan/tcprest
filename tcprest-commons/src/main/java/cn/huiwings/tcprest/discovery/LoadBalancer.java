package cn.huiwings.tcprest.discovery;

import java.util.List;

/**
 * Selects one instance from a list for a single request. Callers must pass a non-empty list.
 *
 * @since 2.0.0
 */
public interface LoadBalancer {

    /**
     * Select one instance from the given list.
     *
     * @param instances current instances (must not be null or empty)
     * @return selected instance
     * @throws IllegalArgumentException if instances is null or empty
     */
    HostPort select(List<HostPort> instances);
}
