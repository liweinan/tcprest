package cn.huiwings.tcprest.server;

import java.util.Map;

/**
 * Interface for resource registration and lookup.
 *
 * <p>Defines the full contract for registering and resolving service instances (resources)
 * for method invocation. {@link TcpRestServer} extends this interface and
 * {@link MapperRegister}, and adds lifecycle and server configuration (compression, security).</p>
 *
 * @since 1.1.0
 */
public interface ResourceRegister {

    /**
     * Register a resource class. A new instance will be created per request.
     *
     * @param resourceClass the resource class to register
     */
    void addResource(Class resourceClass);

    /**
     * Remove a resource class registration.
     *
     * @param resourceClass the resource class to remove
     */
    void deleteResource(Class resourceClass);

    /**
     * Register a singleton resource instance. Adding multiple instances of the same class
     * overwrites the previous one; implementations should warn when overwriting.
     *
     * @param instance the singleton instance to register
     */
    void addSingletonResource(Object instance);

    /**
     * Remove a singleton resource registration.
     *
     * @param instance the singleton instance to remove
     */
    void deleteSingletonResource(Object instance);

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

    /**
     * Get all singleton resources.
     *
     * @return map of class name to singleton instance
     */
    Map<String, Object> getSingletonResources();

    /**
     * Get all resource classes.
     *
     * @return map of class name to resource class
     */
    Map<String, Class> getResourceClasses();

    /**
     * When true, addResource/addSingletonResource throw if any DTO type is neither
     * Serializable nor has a mapper. When false (default), only a warning is logged.
     *
     * @param strictTypeCheck true to fail registration on unsupported types
     */
    void setStrictTypeCheck(boolean strictTypeCheck);

    /**
     * Whether strict DTO/mapper type check is enabled.
     *
     * @return true if unsupported types cause registration to throw
     */
    boolean isStrictTypeCheck();
}
