package net.bluedash.tcprest.server;

import net.bluedash.tcprest.logger.Logger;

import java.util.List;

/**
 *
 * @author Weinan Li
 * Jul 29 2012
 */
public interface TcpRestServer {

    public void up();

    public void down();

    void addResource(Class resourceClass);

    List<Class> getResourceClasses();

    public void setLogger(Logger logger);
}
