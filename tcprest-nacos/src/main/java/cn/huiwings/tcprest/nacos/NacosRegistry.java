package cn.huiwings.tcprest.nacos;

import cn.huiwings.tcprest.discovery.HostPort;
import cn.huiwings.tcprest.discovery.ServiceDiscovery;
import cn.huiwings.tcprest.discovery.ServiceRegistry;

import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingFactory;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Nacos implementation of {@link ServiceRegistry} and {@link ServiceDiscovery}.
 * Uses Nacos NamingService for registration and instance lookup.
 *
 * <p>Configuration: pass {@link Properties} with at least {@code serverAddr} (e.g. "localhost:8848").
 * Optional: {@code namespace}, {@code username}, {@code password}.</p>
 *
 * @since 2.0.0
 */
public class NacosRegistry implements ServiceRegistry, ServiceDiscovery {

    private final NamingService namingService;
    private final String groupName;

    /**
     * Create with NamingService and default group "DEFAULT_GROUP".
     */
    public NacosRegistry(NamingService namingService) {
        this(namingService, "DEFAULT_GROUP");
    }

    /**
     * Create with NamingService and custom group.
     */
    public NacosRegistry(NamingService namingService, String groupName) {
        this.namingService = namingService;
        this.groupName = groupName == null ? "DEFAULT_GROUP" : groupName;
    }

    /**
     * Create from properties. Properties must include "serverAddr" (e.g. "localhost:8848").
     */
    public static NacosRegistry fromProperties(Properties properties) throws NacosException {
        NamingService ns = NamingFactory.createNamingService(properties);
        String group = properties.getProperty("groupName", "DEFAULT_GROUP");
        return new NacosRegistry(ns, group);
    }

    @Override
    public void register(String serviceName, String host, int port) {
        try {
            namingService.registerInstance(serviceName, groupName, host, port);
        } catch (NacosException e) {
            throw new IllegalStateException("Nacos register failed: " + serviceName + " " + host + ":" + port, e);
        }
    }

    @Override
    public void deregister(String serviceName, String host, int port) {
        try {
            namingService.deregisterInstance(serviceName, groupName, host, port);
        } catch (NacosException e) {
            throw new IllegalStateException("Nacos deregister failed: " + serviceName + " " + host + ":" + port, e);
        }
    }

    @Override
    public List<HostPort> getInstances(String serviceName) {
        try {
            List<Instance> instances = namingService.selectInstances(serviceName, groupName, true);
            List<HostPort> result = new ArrayList<>(instances.size());
            for (Instance inst : instances) {
                result.add(new HostPort(inst.getIp(), inst.getPort()));
            }
            return result;
        } catch (NacosException e) {
            throw new IllegalStateException("Nacos getInstances failed: " + serviceName, e);
        }
    }
}
