package cn.huiwings.tcprest.invoker.v2;

import cn.huiwings.tcprest.exception.BusinessException;
import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.server.Context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Protocol v2 invoker that propagates exceptions instead of swallowing them.
 *
 * <p>Unlike DefaultInvoker which catches InvocationTargetException and returns
 * NullObj, ProtocolV2Invoker throws exceptions so they can be properly
 * propagated to the client with status codes.</p>
 *
 * <p><b>Exception Handling:</b></p>
 * <ul>
 *   <li><b>BusinessException</b>: Re-thrown as-is for proper categorization</li>
 *   <li><b>InvocationTargetException</b>: Unwrapped and cause is thrown</li>
 *   <li><b>Other exceptions</b>: Re-thrown as-is</li>
 * </ul>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * ProtocolV2Invoker invoker = new ProtocolV2Invoker();
 * try {
 *     Object result = invoker.invoke(context);
 *     // Success - encode with StatusCode.SUCCESS
 * } catch (BusinessException e) {
 *     // Business error - encode with StatusCode.BUSINESS_EXCEPTION
 * } catch (Exception e) {
 *     // Server error - encode with StatusCode.SERVER_ERROR
 * }
 * </pre>
 *
 * @since 1.1.0
 */
public class ProtocolV2Invoker implements Invoker {

    /**
     * Invoke the target method and return result or throw exception.
     *
     * <p>This method does NOT catch exceptions - they are propagated to the
     * caller for proper status code encoding.</p>
     *
     * @param context the invocation context
     * @return the method result
     * @throws InstantiationException if target instance cannot be created
     * @throws IllegalAccessException if method cannot be accessed
     */
    @Override
    public Object invoke(Context context) throws InstantiationException, IllegalAccessException {
        if (context == null) {
            throw new ProtocolException("Context cannot be null");
        }

        Object targetInstance = context.getTargetInstance();
        Method targetMethod = context.getTargetMethod();
        Object[] params = context.getParams();

        if (targetInstance == null) {
            throw new ProtocolException("Target instance is null");
        }

        if (targetMethod == null) {
            throw new ProtocolException("Target method is null");
        }

        try {
            // Invoke the method - let exceptions propagate
            return targetMethod.invoke(targetInstance, params);
        } catch (InvocationTargetException e) {
            // Unwrap the real exception thrown by the method
            Throwable cause = e.getCause();

            if (cause instanceof BusinessException) {
                // Re-throw business exceptions for proper categorization (RuntimeException)
                throw (BusinessException) cause;
            } else if (cause instanceof RuntimeException) {
                // Re-throw runtime exceptions
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                // Re-throw errors
                throw (Error) cause;
            } else {
                // Wrap checked exceptions in RuntimeException
                throw new RuntimeException("Method invocation failed", cause);
            }
        } catch (IllegalAccessException e) {
            // Re-throw as declared in interface
            throw e;
        } catch (IllegalArgumentException e) {
            throw new ProtocolException("Invalid arguments for method: " + targetMethod.getName(), e);
        }
    }

    /**
     * Create target instance from class.
     *
     * @param clazz the target class
     * @return new instance
     * @throws Exception if instantiation fails
     */
    public Object createInstance(Class<?> clazz) throws Exception {
        if (clazz == null) {
            throw new ProtocolException("Class cannot be null");
        }

        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new ProtocolException("Cannot instantiate class: " + clazz.getName(), e);
        }
    }
}
