package cn.huiwings.tcprest.discovery;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round-robin implementation of {@link LoadBalancer}. Thread-safe.
 *
 * @since 2.0.0
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public HostPort select(List<HostPort> instances) {
        if (instances == null || instances.isEmpty()) {
            throw new IllegalArgumentException("instances must not be null or empty");
        }
        int index = Math.abs(counter.getAndIncrement() % instances.size());
        return instances.get(index);
    }
}
