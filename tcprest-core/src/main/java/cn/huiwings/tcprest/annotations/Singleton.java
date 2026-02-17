package cn.huiwings.tcprest.annotations;

/**
 * Marks a resource class to be managed as a singleton by the server.
 *
 * <p>When a class is annotated with {@code @Singleton}, the server will:
 * <ul>
 *   <li>Create a single instance of the resource at registration time</li>
 *   <li>Reuse the same instance for all client requests</li>
 *   <li>Maintain state across multiple invocations</li>
 * </ul>
 *
 * <p><b>Example:</b></p>
 * <pre>{@code
 * @Singleton
 * public class CounterResource implements Counter {
 *     private int count = 0;
 *
 *     public int getCounter() {
 *         return count;
 *     }
 *
 *     public void increaseCounter() {
 *         count++;
 *     }
 * }
 *
 * // Server setup
 * server.addSingletonResource(new CounterResource());
 * }</pre>
 *
 * <p><b>Thread Safety:</b> Singleton resources should be thread-safe when used
 * with multi-threaded servers ({@link cn.huiwings.tcprest.server.NioTcpRestServer}
 * or NettyTcpRestServer).</p>
 *
 * @see cn.huiwings.tcprest.server.AbstractTcpRestServer#addSingletonResource(Object)
 * @author Weinan Li
 * @date Aug 20 2012
 */
@Server
public @interface Singleton {
}
