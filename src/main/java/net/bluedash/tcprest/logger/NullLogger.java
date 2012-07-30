package net.bluedash.tcprest.logger;

/**
 * This logger eats all messages :-)
 *
 * @author Weinan Li
 *         CREATED AT: Jul 29 2012
 */
public class NullLogger implements Logger {

    public void log(String message) {
        // eating the message!
    }
}
