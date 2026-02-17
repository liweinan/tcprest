package cn.huiwings.tcprest.server;

/**
 * Interface for resource registration and lookup.
 *
 * <p>Provides access to registered service instances (resources)
 * for method invocation.</p>
 *
 * @since 1.1.0
 */
public interface ResourceRegister {

    /**
     * Get resource instance by class name.
     *
     * @param className fully qualified class name
     * @return resource instance, or null if not found
     */
    Object getResource(String className);

    /**
     * Check if resource is registered.
     *
     * @param className fully qualified class name
     * @return true if registered
     */
    boolean hasResource(String className);
}
