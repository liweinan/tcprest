package io.tcprest.server;

import io.tcprest.extractor.DefaultExtractor;
import io.tcprest.extractor.Extractor;
import io.tcprest.invoker.DefaultInvoker;
import io.tcprest.invoker.Invoker;
import io.tcprest.logger.Logger;
import io.tcprest.logger.LoggerFactory;
import io.tcprest.mapper.Mapper;
import io.tcprest.mapper.MapperHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Weinan Li
 * @created_at 08 26 2012
 */
public abstract class AbstractTcpRestServer implements TcpRestServer {

    protected final Map<String, Mapper> mappers = (Map<String, Mapper>) MapperHelper.DEFAULT_MAPPERS.clone();

    protected Logger logger = LoggerFactory.getDefaultLogger();

    protected String status = TcpRestServerStatus.PASSIVE;
    public final Map<String, Class> resourceClasses = new HashMap<String, Class>();

    public final Map<String, Object> singletonResources = new HashMap<String, Object>();

    public Extractor extractor = new DefaultExtractor(this);

    public Invoker invoker = new DefaultInvoker();

    public void addResource(Class resourceClass) {
        if (resourceClass == null) {
            return;
        }

        // Adding multiple instances of same class is meaningless. So every TcpRestServer implementation
        // should check and overwrite existing instances of same class and give out warning each time a
        // singleton resource is added.
        synchronized (resourceClasses) {
            if (resourceClasses.containsKey(resourceClass.getCanonicalName())) {
                logger.warn("Resource already exists for: " + resourceClass.getCanonicalName());
            }
            deleteResource(resourceClass);
            resourceClasses.put(resourceClass.getCanonicalName(), resourceClass);
        }
    }

    public void deleteResource(Class resourceClass) {
        synchronized (resourceClasses) {
            resourceClasses.remove(resourceClass.getCanonicalName());
        }
    }

    public void addSingletonResource(Object instance) {
        synchronized (singletonResources) {
            deleteSingletonResource(instance);
            singletonResources.put(instance.getClass().getCanonicalName(), instance);
        }
    }

    public void deleteSingletonResource(Object instance) {
        synchronized (singletonResources) {
            singletonResources.remove(instance.getClass().getCanonicalName());
        }
    }

    public Map<String, Class> getResourceClasses() {
        return new HashMap<String, Class>(resourceClasses);
    }

    public Map<String, Object> getSingletonResources() {
        return new HashMap<String, Object>(singletonResources);
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    protected String processRequest(String request) throws Exception {
        logger.debug("request: " + request);
        // extract calling class and method from request
        Context context = extractor.extract(request);
        // invoke real method
        Object responseObject = invoker.invoke(context);
        logger.debug("***responseObject: " + responseObject);

        // get returned object and encode it to string response
        Mapper responseMapper = context.getConverter().getMapper(mappers, responseObject.getClass());

        return context.getConverter().encodeParam(responseMapper.objectToString(responseObject));

    }

    public Map<String, Mapper> getMappers() {
        // We don't want user to modify the mappers by getMappers.
        // Use addMapper() to add mapper to server to ensure concurrency safety
        return new HashMap<String, Mapper>(this.mappers);
    }

    public void addMapper(String canonicalName, Mapper mapper) {
        synchronized (mappers) {
            mappers.put(canonicalName, mapper);
        }
    }

}
