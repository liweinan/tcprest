package io.tcprest.logger;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public class LoggerFactory {

    public static Logger getDefaultLogger() {
        return new SystemOutLogger();
//        return new NullLogger();
    }
}
