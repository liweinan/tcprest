package cn.huiwings.tcprest.registry;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.discovery.ServiceDiscovery;
import cn.huiwings.tcprest.discovery.ServiceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory implementation of {@link ServiceRegistry} and {@link ServiceDiscovery}.
 * No external storage or network; suitable for single-JVM tests and lightweight use.
 *
 * @since 2.0.0
 */
public class InMemoryRegistry implements ServiceRegistry, ServiceDiscovery {

    private final Map<String, List<HostPort>> serviceToInstances = new ConcurrentHashMap<>();

    @Override
    public void register(String serviceName, String host, int port) {
        serviceToInstances.computeIfAbsent(serviceName, k -> new CopyOnWriteArrayList<>())
                .add(new HostPort(host, port));
    }

    @Override
    public void deregister(String serviceName, String host, int port) {
        List<HostPort> list = serviceToInstances.get(serviceName);
        if (list != null) {
            list.remove(new HostPort(host, port));
        }
    }

    @Override
    public List<HostPort> getInstances(String serviceName) {
        List<HostPort> list = serviceToInstances.get(serviceName);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }
}
