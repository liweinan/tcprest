package io.tcprest.logger;

/**
 * Output log to System.out
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class SystemOutLogger implements Logger {

    public void log(String message) {
        System.out.println(message);
    }

    public void log(String message, LoggerLevel level) {
        System.out.println(message);
    }

    public void debug(String message) {
        log(message, LoggerLevel.DEBUG);
    }

    public void info(String message) {
        log(message, LoggerLevel.INFO);
    }

    public void warn(String message) {
        log(message, LoggerLevel.WARN);
    }

    public void error(String message) {
        log(message, LoggerLevel.ERROR);
    }
}
