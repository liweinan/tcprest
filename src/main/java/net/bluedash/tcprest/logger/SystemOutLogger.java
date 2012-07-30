package net.bluedash.tcprest.logger;

/**
 * Output log to System.out
 *
 * @author Weinan Li
 *         CREATED AT: Jul 29 2012
 */
public class SystemOutLogger implements Logger {

    public void log(String message) {
        System.out.println(message);
    }
}
