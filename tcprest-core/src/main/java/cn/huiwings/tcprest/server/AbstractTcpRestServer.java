package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import cn.huiwings.tcprest.extractor.DefaultExtractor;
import cn.huiwings.tcprest.extractor.Extractor;
import cn.huiwings.tcprest.invoker.DefaultInvoker;
import cn.huiwings.tcprest.invoker.Invoker;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.MapperHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Weinan Li
 * @created_at 08 26 2012
 */
public abstract class AbstractTcpRestServer implements TcpRestServer {

    protected final Map<String, Mapper> mappers = new HashMap<>(MapperHelper.DEFAULT_MAPPERS);

    protected Logger logger = LoggerFactory.getDefaultLogger();

    protected String status = TcpRestServerStatus.PASSIVE;
    public final Map<String, Class> resourceClasses = new HashMap<String, Class>();

    public final Map<String, Object> singletonResources = new HashMap<String, Object>();

    public Extractor extractor = new DefaultExtractor(this);

    public Invoker invoker = new DefaultInvoker();

    protected CompressionConfig compressionConfig = new CompressionConfig(); // Default: disabled

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

        // Decompress request if needed
        String decompressedRequest = request;
        if (request != null && (request.startsWith("0|") || request.startsWith("1|"))) {
            try {
                decompressedRequest = CompressionUtil.decompress(request);
                if (CompressionUtil.isCompressed(request)) {
                    logger.debug("Decompressed incoming request");
                }
            } catch (IOException e) {
                logger.error("Failed to decompress request: " + e.getMessage());
                // Continue with original request as fallback
                decompressedRequest = request;
            }
        }

        // extract calling class and method from request
        Context context = extractor.extract(decompressedRequest);
        // invoke real method
        Object responseObject = invoker.invoke(context);
        logger.debug("***responseObject: " + responseObject);

        // get returned object and encode it to string response
        Mapper responseMapper = context.getConverter().getMapper(mappers, responseObject.getClass());
        String response = context.getConverter().encodeParam(responseMapper.objectToString(responseObject));

        // Compress response if enabled
        if (compressionConfig.isEnabled()) {
            try {
                String compressed = CompressionUtil.compress(response, compressionConfig);
                if (CompressionUtil.isCompressed(compressed)) {
                    double ratio = CompressionUtil.getCompressionRatio(response, compressed);
                    logger.debug("Compressed response (saved " + String.format("%.1f", ratio) + "%)");
                }
                return compressed;
            } catch (IOException e) {
                logger.error("Failed to compress response: " + e.getMessage());
                return "0|" + response; // Fallback with uncompressed prefix
            }
        } else {
            // Add prefix for protocol compatibility
            return "0|" + response;
        }
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

    public CompressionConfig getCompressionConfig() {
        return compressionConfig;
    }

    public void setCompressionConfig(CompressionConfig compressionConfig) {
        if (compressionConfig == null) {
            throw new IllegalArgumentException("Compression config cannot be null");
        }
        this.compressionConfig = compressionConfig;
        logger.info("Compression configured: " + compressionConfig);
    }

    /**
     * Enable compression with default settings
     */
    public void enableCompression() {
        this.compressionConfig.setEnabled(true);
        logger.info("Compression enabled with default settings");
    }

    /**
     * Disable compression
     */
    public void disableCompression() {
        this.compressionConfig.setEnabled(false);
        logger.info("Compression disabled");
    }

}
