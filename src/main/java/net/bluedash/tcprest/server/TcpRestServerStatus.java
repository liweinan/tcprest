package net.bluedash.tcprest.server;

/**
 * Defines the status of TcpRestServer.
 * The initial status is always PASSIVE, after server starts it transformed into RUNNING status.
 * If the TcpRestServer is set to CLOSING status, the server will fire the shutdown process.
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class TcpRestServerStatus {

    public static String PASSIVE = "passive";
    public static String RUNNING = "running";
    public static String CLOSING = "closing";
}
