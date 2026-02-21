package cn.huiwings.tcprest.server;

import java.util.logging.Logger;

import java.util.Map;

/**
 * Utility class for resolving resource instances from {@link ResourceRegister}.
 *
 * <p>The resource registration and lookup contract is defined by {@link ResourceRegister};
 * {@link TcpRestServer} extends that interface and adds lifecycle and configuration.</p>
 *
 * <p><b>Purpose:</b> Provides centralized resource resolution logic.
 * Used by {@link AbstractTcpRestServer} for resource lookup and instance creation.</p>
 *
 * <p><b>This resolver handles:</b></p>
 * <ul>
 *   <li>Finding singleton resources by class name</li>
 *   <li>Finding implementation classes for interfaces</li>
 *   <li>Creating new instances when not found</li>
 *   <li>Handling canonical name vs internal name (Foo.Bar vs Foo$Bar)</li>
 * </ul>
 *
 * <p><b>Resolution Strategy:</b></p>
 * <pre>
 * 1. Try direct singleton lookup by class name
 * 2. If target is interface, find implementation class
 * 3. Try singleton lookup by implementation class name
 * 4. Create new instance from implementation class
 * 5. If no implementation found, create instance from target class directly
 * </pre>
 *
 * <p><b>Usage in TcpRest:</b></p>
 * <ul>
 *   <li>Used by {@link cn.huiwings.tcprest.parser.RequestParser} for resource lookup and instance creation</li>
 *   <li>Handles both singleton resources and per-request instances</li>
 * </ul>
 *
 * <p><b>Example Usage:</b></p>
 * <pre>
 * // Find or create Calculator instance
 * Object instance = ResourceResolver.findResourceInstance(
 *     Calculator.class,
 *     resourceRegister,
 *     logger
 * );
 *
 * // If Calculator is an interface, finds implementation class
 * // If singleton exists, returns it
 * // Otherwise creates new instance
 * </pre>
 *
 * @since 1.1.0
 */
public final class ResourceResolver {

    private static final Logger DEFAULT_LOGGER = Logger.getLogger(ResourceResolver.class.getName());

    private ResourceResolver() {
        // Utility class, prevent instantiation
    }

    /**
     * Find or create resource instance for target class.
     *
     * <p>This method searches both resource classes and singleton resources.
     * If target class is an interface, it finds the implementation class.</p>
     *
     * @param targetClass the target class or interface
     * @param resourceRegister resource register
     * @param logger logger instance
     * @return resource instance
     * @throws Exception if instance cannot be created
     */
    public static Object findResourceInstance(
            Class<?> targetClass,
            ResourceRegister resourceRegister,
            Logger logger) throws Exception {

        String targetClassName = targetClass.getName();
        Logger log = (logger != null) ? logger : DEFAULT_LOGGER;

        // Step 1: Try direct singleton lookup
        Object instance = resourceRegister.getResource(targetClassName);
        if (instance != null) {
            return instance;
        }

        // Step 2: If target is an interface, search for implementation
        // Check both resource classes and singleton resources
        Class<?> implClass = findImplementationClass(targetClassName, resourceRegister, log);

        if (implClass != null) {
            // Found implementation class
            targetClassName = implClass.getName();

            // Try singleton lookup again with implementation class name
            instance = resourceRegister.getResource(targetClassName);
            if (instance != null) {
                return instance;
            }

            // Also try with canonical name (for inner classes: Foo.Bar vs Foo$Bar)
            String canonicalName = implClass.getCanonicalName();
            if (canonicalName != null && !canonicalName.equals(targetClassName)) {
                instance = resourceRegister.getResource(canonicalName);
                if (instance != null) {
                    return instance;
                }
            }

            // Create new instance from implementation class
            return implClass.newInstance();
        }

        // Step 3: No implementation found, try to create instance directly
        return targetClass.newInstance();
    }

    /**
     * Find implementation class for an interface.
     *
     * <p>Searches both resource classes and singleton resources.
     * Returns the first class that implements the interface.</p>
     *
     * @param interfaceName the interface name
     * @param resourceRegister resource register
     * @param logger logger instance
     * @return implementation class, or null if not found
     */
    public static Class<?> findImplementationClass(
            String interfaceName,
            ResourceRegister resourceRegister,
            Logger logger) {

        Logger log = (logger != null) ? logger : DEFAULT_LOGGER;

        // Check resource classes
        Map<String, Class> resourceClasses = resourceRegister.getResourceClasses();
        for (Class<?> clazz : resourceClasses.values()) {
            // Check if this class directly matches
            if (clazz.getName().equals(interfaceName)) {
                return clazz;
            }
            // Check if this class implements the interface
            for (Class<?> ifc : clazz.getInterfaces()) {
                if (ifc.getName().equals(interfaceName)) {
                    log.fine("Found resource class implementing interface: " + interfaceName +
                            " -> " + clazz.getName());
                    return clazz;
                }
            }
        }

        // Check singleton resources
        Map<String, Object> singletons = resourceRegister.getSingletonResources();
        for (Object singleton : singletons.values()) {
            Class<?> clazz = singleton.getClass();
            // Check if this class directly matches
            if (clazz.getName().equals(interfaceName)) {
                return clazz;
            }
            // Check if this singleton implements the interface
            for (Class<?> ifc : clazz.getInterfaces()) {
                if (ifc.getName().equals(interfaceName)) {
                    log.fine("Found singleton implementing interface: " + interfaceName +
                            " -> " + clazz.getName());
                    return clazz;
                }
            }
        }

        return null;
    }
}
