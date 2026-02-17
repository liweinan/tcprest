package cn.huiwings.tcprest.logger;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface Logger {

    /**
     * Output log message in default level
     *
     * @param message
     */
    public void log(String message);


    /**
     * Output log message with error level
     *
     * @param message
     * @param level
     */
    public void log(String message, LoggerLevel level);

    /**
     * Output log message to debug level
     *
     * @param message
     */
    public void debug(String message);

    /**
     * Output log message to info level
     *
     * @param message
     */
    public void info(String message);

    /**
     * Output log message to warning level
     *
     * @param message
     */
    public void warn(String message);

    /**
     * Output log message to error level
     *
     * @param message
     */
    public void error(String message);


}
