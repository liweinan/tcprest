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

    public void log(String message, int log_level) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
