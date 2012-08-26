package io.tcprest.server;

import io.tcprest.logger.Logger;
import io.tcprest.mapper.Mapper;

import java.util.Map;

/**
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface TcpRestServer {

    public void up();

    public void up(boolean setDaemon);

    public void down();

    void addResource(Class resourceClass);

    void deleteResource(Class resourceClass);

    /**
     * Adding multiple instances of same class is meaningless. So every TcpRestServer implementation
     * should check and overwrite existing instances of same class and give out warning each time a
     * singleton resource is added.
     * @param instance
     */
    void addSingletonResource(Object instance);

    void deleteSingletonResource(Object instance);

    Map<String, Class> getResourceClasses();

    Map<String, Object> getSingletonResources();

    public void setLogger(Logger logger);

    /**
     * Get a cloned copy of mappers
     * The impelmentation should ensure thread safety
     * @return
     */
    public Map<String, Mapper> getMappers();

    void addMapper(String canonicalName, Mapper mapper);

    public int getServerPort();
}
