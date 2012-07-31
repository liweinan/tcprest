package io.tcprest.logger;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface Logger {

    public void log(String message);

    public void log(String message, int log_level);
}
