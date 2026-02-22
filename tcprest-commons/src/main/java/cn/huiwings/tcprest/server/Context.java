package cn.huiwings.tcprest.server;

import java.lang.reflect.Method;

/**
 * Method invocation context containing all information needed to execute a remote procedure call.
 *
 * <p>Context is the central data structure in the TcpRest server request processing pipeline,
 * serving as the container for all extracted method invocation details. It is created by
 * {@link cn.huiwings.tcprest.parser.RequestParser} during request parsing and consumed by
 * {@link cn.huiwings.tcprest.invoker.Invoker} during method execution.</p>
 *
 * <p><b>Lifecycle in request processing:</b></p>
 * <pre>
 * 1. Client sends request → Server receives protocol string
 * 2. RequestParser.parse(request) → Creates and populates Context
 * 3. Server resolves target instance (singleton or new instance)
 * 4. Invoker.invoke(context) → Executes method using Context data
 * 5. ProtocolCodec encodes result → Response sent back to client
 * </pre>
 *
 * <p><b>Example workflow (Protocol V2):</b></p>
 * <pre>
 * // Request: V2|0|{{base64(Calculator/add(II))}}|[5,3]
 *
 * // 1. RequestParser creates Context
 * Context context = new Context();
 * context.setTargetClass(Calculator.class);
 * context.setTargetMethod(Calculator.class.getMethod("add", int.class, int.class));
 * context.setParams(new Object[]{5, 3});
 *
 * // 2. Server sets target instance
 * context.setTargetInstance(calculatorInstance);  // Singleton or new instance
 *
 * // 3. Invoker executes method
 * Object result = context.getTargetMethod().invoke(
 *     context.getTargetInstance(),
 *     context.getParams()
 * );  // result = 8
 * </pre>
 *
 * <p><b>Field responsibilities:</b></p>
 * <ul>
 *   <li><b>targetClazz</b>: The service interface/class to invoke (e.g., Calculator.class)</li>
 *   <li><b>targetMethod</b>: The specific method to execute (resolved by RequestParser using signature)</li>
 *   <li><b>targetInstance</b>: The actual object to invoke method on (singleton or newly created)</li>
 *   <li><b>params</b>: Deserialized method arguments ready for invocation</li>
 *   <li><b>paramTypes</b>: Parameter type information (used for method resolution)</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> Context instances are NOT thread-safe. Each request should have
 * its own Context instance. Do not share Context instances across threads.</p>
 *
 * <p><b>Mutability:</b> Context is designed as a mutable data holder. Fields are set
 * incrementally during the request processing pipeline.</p>
 *
 * @author Weinan Li
 * @since 1.0.0
 * @see cn.huiwings.tcprest.parser.RequestParser
 * @see cn.huiwings.tcprest.invoker.Invoker
 */
public class Context {
    /**
     * Target service class to be invoked.
     * <p>This is the interface or class specified in the request (e.g., Calculator.class).</p>
     * <p>Set by: {@link cn.huiwings.tcprest.parser.RequestParser}</p>
     */
    private Class targetClazz;

    /**
     * Target service instance to invoke the method on.
     * <p>This can be:</p>
     * <ul>
     *   <li>A singleton instance registered via {@link TcpRestServer#addSingletonResource(Object)}</li>
     *   <li>A newly created instance via {@code targetClazz.newInstance()}</li>
     * </ul>
     * <p>Set by: Server before passing to Invoker</p>
     */
    private Object targetInstance;

    /**
     * Target method to be invoked.
     * <p>Resolved by RequestParser using method name and signature.</p>
     * <p>Protocol V2 supports method overloading by including type signatures in requests.</p>
     * <p>Set by: {@link cn.huiwings.tcprest.parser.RequestParser}</p>
     */
    private Method targetMethod;

    /**
     * Deserialized method parameters ready for invocation.
     * <p>Array length matches the method's parameter count.</p>
     * <p>Values are already converted from protocol strings to Java objects by the RequestParser.</p>
     * <p>Set by: {@link cn.huiwings.tcprest.parser.RequestParser}</p>
     */
    private Object[] params;

    /**
     * Parameter type information used for method resolution.
     * <p>Used by V2 protocol to support method overloading.</p>
     * <p>Contains type descriptors like "I" for int, "Ljava/lang/String;" for String.</p>
     * <p>Set by: {@link cn.huiwings.tcprest.parser.RequestParser}</p>
     */
    private Object[] paramTypes;

    /**
     * Creates a new empty Context.
     * <p>Fields are populated incrementally by RequestParser and Server.</p>
     */
    public Context() {
    }

    /**
     * Gets the target service class.
     *
     * @return the target class (e.g., Calculator.class), may be null if not yet set
     */
    public Class getTargetClass() {
        return targetClazz;
    }

    /**
     * Gets the target method to be invoked.
     *
     * @return the target method, may be null if not yet set
     */
    public Method getTargetMethod() {
        return targetMethod;
    }

    /**
     * Sets the target service class.
     *
     * @param clazz the target class (e.g., Calculator.class)
     */
    public void setTargetClass(Class clazz) {
        this.targetClazz = clazz;
    }

    /**
     * Sets the target method to be invoked.
     *
     * @param mtd the target method
     */
    public void setTargetMethod(Method mtd) {
        this.targetMethod = mtd;
    }

    /**
     * Gets the deserialized method parameters.
     *
     * @return array of method arguments, may be null or empty
     */
    public Object[] getParams() {
        return params;
    }

    /**
     * Sets the deserialized method parameters.
     *
     * @param params array of method arguments
     */
    public void setParams(Object[] params) {
        this.params = params;
    }

    /**
     * Gets the parameter type information.
     *
     * @return array of type descriptors, may be null
     */
    public Object[] getParamTypes() {
        return paramTypes;
    }

    /**
     * Sets the parameter type information.
     *
     * @param paramTypes array of type descriptors (e.g., "I", "Ljava/lang/String;")
     */
    public void setParamTypes(Object[] paramTypes) {
        this.paramTypes = paramTypes;
    }

    /**
     * Gets the target service instance.
     *
     * @return the target instance (singleton or newly created), may be null if not yet set
     */
    public Object getTargetInstance() {
        return targetInstance;
    }

    /**
     * Sets the target service instance.
     *
     * @param targetInstance the target instance (singleton or newly created)
     */
    public void setTargetInstance(Object targetInstance) {
        this.targetInstance = targetInstance;
    }
}
