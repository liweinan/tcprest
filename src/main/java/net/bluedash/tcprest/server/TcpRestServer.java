package net.bluedash.tcprest.server;

import net.bluedash.tcprest.logger.Logger;
import net.bluedash.tcprest.mapper.Mapper;

import java.util.List;
import java.util.Map;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface TcpRestServer {

    public void up();

    public void down();

    void addResource(Class resourceClass);

    List<Class> getResourceClasses();

    public void setLogger(Logger logger);

    public Map<String, Mapper> getMappers();

    public void setMappers(Map<String, Mapper> mappers);

}
