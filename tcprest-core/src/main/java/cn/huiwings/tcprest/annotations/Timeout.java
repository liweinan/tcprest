package cn.huiwings.tcprest.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a timeout in seconds for a client method invocation.
 *
 * <p>When a method is annotated with {@code @Timeout}, the TcpRest client
 * will wait up to the specified number of seconds for the server response
 * before timing out.
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * public interface MyService {
 *     @Timeout(second = 5)
 *     String slowOperation();  // Max 5 seconds wait
 *
 *     String fastOperation();  // Default timeout (no limit)
 * }
 * }</pre>
 *
 * @author Weinan Li
 * @date Aug 21 2012
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Timeout {
    /**
     * The timeout duration in seconds.
     *
     * @return timeout in seconds (must be positive)
     */
    int second();
}
