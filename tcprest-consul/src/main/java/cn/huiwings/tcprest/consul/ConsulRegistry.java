package cn.huiwings.tcprest.consul;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.discovery.ServiceDiscovery;
import cn.huiwings.tcprest.discovery.ServiceRegistry;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;

import java.util.ArrayList;
import java.util.List;

/**
 * Consul implementation of {@link ServiceRegistry} and {@link ServiceDiscovery}.
 * Uses Consul agent for registration and health API for discovery (healthy instances only).
 *
 * <p>Registration uses a unique service ID: {@code serviceName-host-port} so that deregister can remove the correct instance.</p>
 *
 * @since 2.0.0
 */
public class ConsulRegistry implements ServiceRegistry, ServiceDiscovery {

    private final ConsulClient consulClient;

    public ConsulRegistry(ConsulClient consulClient) {
        this.consulClient = consulClient;
    }

    /**
     * Create client connecting to Consul at host:8500.
     */
    public ConsulRegistry(String agentHost) {
        this(new ConsulClient(agentHost));
    }

    /**
     * Create client connecting to Consul at host:port.
     */
    public ConsulRegistry(String agentHost, int agentPort) {
        this(new ConsulClient(agentHost, agentPort));
    }

    private static String serviceId(String serviceName, String host, int port) {
        return serviceName + "-" + host + "-" + port;
    }

    @Override
    public void register(String serviceName, String host, int port) {
        NewService service = new NewService();
        service.setId(serviceId(serviceName, host, port));
        service.setName(serviceName);
        service.setAddress(host);
        service.setPort(port);
        consulClient.agentServiceRegister(service);
    }

    @Override
    public void deregister(String serviceName, String host, int port) {
        consulClient.agentServiceDeregister(serviceId(serviceName, host, port));
    }

    @Override
    public List<HostPort> getInstances(String serviceName) {
        Response<List<HealthService>> resp = consulClient.getHealthServices(serviceName, true, QueryParams.DEFAULT);
        List<HostPort> result = new ArrayList<>();
        if (resp.getValue() == null) {
            return result;
        }
        for (HealthService hs : resp.getValue()) {
            String address = hs.getService().getAddress();
            if (address == null || address.isEmpty()) {
                address = hs.getNode().getAddress();
            }
            if (address == null) {
                address = "localhost";
            }
            Integer port = hs.getService().getPort();
            if (port != null) {
                result.add(new HostPort(address, port));
            }
        }
        return result;
    }
}
