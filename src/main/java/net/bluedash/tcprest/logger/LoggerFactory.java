package net.bluedash.tcprest.logger;

/**
 * @author Weinan Li
 *         CREATED AT: Jul 29 2012
 */
public class LoggerFactory {

    public static Logger getDefaultLogger() {
        return new SystemOutLogger();
    }
}
