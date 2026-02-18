package cn.huiwings.tcprest.invoker;

import cn.huiwings.tcprest.server.Context;

/**
 * Method invocation strategy for executing remote procedure calls on the server side.
 *
 * <p>The Invoker interface is responsible for the final step in request processing:
 * taking the parsed {@link Context} from a {@link cn.huiwings.tcprest.parser.RequestParser}
 * and actually invoking the target method using Java reflection.</p>
 *
 * <p><b>Primary responsibilities:</b></p>
 * <ul>
 *   <li>Obtain target service instance (singleton or create new)</li>
 *   <li>Invoke target method with deserialized parameters</li>
 *   <li>Handle method invocation exceptions</li>
 *   <li>Return method result for encoding by {@link cn.huiwings.tcprest.codec.ProtocolCodec}</li>
 * </ul>
 *
 * <p><b>Implementations:</b></p>
 * <ul>
 *   <li>{@link cn.huiwings.tcprest.invoker.DefaultInvoker} - Protocol V1 (swallows exceptions, returns NullObj)</li>
 *   <li>{@link cn.huiwings.tcprest.invoker.v2.ProtocolV2Invoker} - Protocol V2 (propagates exceptions for status codes)</li>
 * </ul>
 *
 * <p><b>Typical workflow:</b></p>
 * <pre>
 * // 1. RequestParser parses request into Context
 * Context context = parser.parse(request);
 *
 * // 2. Invoker executes the method
 * Invoker invoker = new ProtocolV2Invoker();
 * Object result = invoker.invoke(context);
 *
 * // 3. ProtocolCodec encodes result back to client
 * String response = codec.encode(result);
 * </pre>
 *
 * <p><b>V1 vs V2 Behavior:</b></p>
 * <table border="1">
 *   <tr>
 *     <th>Aspect</th>
 *     <th>DefaultInvoker (V1)</th>
 *     <th>ProtocolV2Invoker (V2)</th>
 *   </tr>
 *   <tr>
 *     <td>Exception handling</td>
 *     <td>Catches InvocationTargetException, returns NullObj</td>
 *     <td>Propagates exceptions to caller</td>
 *   </tr>
 *   <tr>
 *     <td>Status codes</td>
 *     <td>No status codes (success/failure indistinguishable)</td>
 *     <td>Exceptions → status codes (SUCCESS, BUSINESS_EXCEPTION, SERVER_ERROR)</td>
 *   </tr>
 *   <tr>
 *     <td>Null handling</td>
 *     <td>Returns NullObj for null results</td>
 *     <td>Returns null directly (V2 protocol handles it)</td>
 *   </tr>
 * </table>
 *
 * <p><b>Example - Custom Invoker with Logging:</b></p>
 * <pre>
 * public class LoggingInvoker implements Invoker {
 *     private final Logger logger;
 *
 *     &#64;Override
 *     public Object invoke(Context context) throws InstantiationException, IllegalAccessException {
 *         Object instance = context.getTargetInstance();
 *         if (instance == null) {
 *             instance = context.getTargetClass().newInstance();
 *         }
 *
 *         Method method = context.getTargetMethod();
 *         Object[] params = context.getParams();
 *
 *         logger.info("Invoking: " + method.getName());
 *         try {
 *             Object result = method.invoke(instance, params);
 *             logger.info("Success: " + result);
 *             return result;
 *         } catch (InvocationTargetException e) {
 *             logger.error("Failed: " + e.getCause());
 *             throw new RuntimeException(e.getCause());
 *         }
 *     }
 * }
 * </pre>
 *
 * @author Weinan Li
 * @since 1.0.0
 * @see cn.huiwings.tcprest.server.Context
 * @see cn.huiwings.tcprest.invoker.DefaultInvoker
 * @see cn.huiwings.tcprest.invoker.v2.ProtocolV2Invoker
 * @see cn.huiwings.tcprest.parser.RequestParser
 * @see cn.huiwings.tcprest.codec.ProtocolCodec
 */
public interface Invoker {
    /**
     * Invoke the target method specified in the context.
     *
     * <p>This method performs the actual method invocation using Java reflection.
     * The behavior depends on the implementation:</p>
     *
     * <p><b>DefaultInvoker (V1):</b></p>
     * <ol>
     *   <li>Get target instance from context (singleton) or create new instance</li>
     *   <li>Invoke method with parameters from context</li>
     *   <li>Catch InvocationTargetException and return NullObj on failure</li>
     *   <li>Return NullObj if result is null (for V1 protocol compatibility)</li>
     * </ol>
     *
     * <p><b>ProtocolV2Invoker (V2):</b></p>
     * <ol>
     *   <li>Validate context is not null</li>
     *   <li>Get target instance (must not be null)</li>
     *   <li>Invoke method with parameters</li>
     *   <li>Propagate exceptions (BusinessException, RuntimeException) to caller</li>
     *   <li>Return result directly (null is allowed)</li>
     * </ol>
     *
     * <p><b>Example usage (V2):</b></p>
     * <pre>
     * Context context = extractor.extract(request);
     * Invoker invoker = new ProtocolV2Invoker();
     *
     * try {
     *     Object result = invoker.invoke(context);
     *     // Encode success response with status code SUCCESS
     * } catch (BusinessException e) {
     *     // Encode error response with status code BUSINESS_EXCEPTION
     * } catch (Exception e) {
     *     // Encode error response with status code SERVER_ERROR
     * }
     * </pre>
     *
     * @param context the invocation context containing target class, method, instance, and parameters
     * @return the method invocation result (may be null in V2, NullObj in V1)
     * @throws InstantiationException if the target class cannot be instantiated
     * @throws IllegalAccessException if the target method cannot be accessed
     * @see cn.huiwings.tcprest.server.Context
     */
    public Object invoke(Context context) throws InstantiationException, IllegalAccessException;
}
