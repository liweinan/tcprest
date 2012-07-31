package io.tcprest.logger;

/**
 * This logger eats all messages :-)
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class NullLogger implements Logger {

    public void log(String message) {
        // eating the message!
    }

    public void log(String message, int log_level) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
