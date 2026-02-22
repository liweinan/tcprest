package cn.huiwings.tcprest.server;

/**
 * Defines the status of TcpRestServer.
 * Initial and after shutdown: CLOSED. After {@code up()}: RUNNING. When {@code down()} is called: CLOSING, then CLOSED when shutdown completes.
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class TcpRestServerStatus {

    public static final String CLOSED = "closed";
    public static final String RUNNING = "running";
    public static final String CLOSING = "closing";
}
