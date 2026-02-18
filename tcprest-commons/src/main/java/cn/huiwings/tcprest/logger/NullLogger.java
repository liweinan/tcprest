package cn.huiwings.tcprest.logger;

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

    public void log(String message, LoggerLevel level) {

    }

    public void debug(String message) {

    }

    public void info(String message) {

    }

    public void warn(String message) {

    }

    public void error(String message) {

    }
}
