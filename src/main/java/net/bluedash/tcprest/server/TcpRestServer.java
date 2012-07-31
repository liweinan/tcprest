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

    void deleteResource(Class resourceClass);

    List<Class> getResourceClasses();

    public void setLogger(Logger logger);

    /**
     * Get a cloned copy of mappers
     * The impelmentation should ensure thread safety
     * @return
     */
    public Map<String, Mapper> getMappers();

    public void setMappers(Map<String, Mapper> mappers);

    void addMapper(String canonicalName, Mapper mapper);
}
