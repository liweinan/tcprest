package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.security.SecurityConfig;

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

    /**
     * When true, addResource/addSingletonResource throw if any DTO type is neither
     * Serializable nor has a mapper. When false (default), only a warning is logged.
     *
     * @param strictTypeCheck true to fail registration on unsupported types
     */
    void setStrictTypeCheck(boolean strictTypeCheck);

    /**
     * Whether strict DTO/mapper type check is enabled.
     *
     * @return true if unsupported types cause registration to throw
     */
    boolean isStrictTypeCheck();

    /**
     * Get a cloned copy of mappers
     * The impelmentation should ensure thread safety
     * @return
     */
    public Map<String, Mapper> getMappers();

    void addMapper(String canonicalName, Mapper mapper);

    public int getServerPort();

    /**
     * Get compression configuration
     */
    CompressionConfig getCompressionConfig();

    /**
     * Set compression configuration
     */
    void setCompressionConfig(CompressionConfig compressionConfig);

    /**
     * Enable compression with default settings
     */
    void enableCompression();

    /**
     * Disable compression
     */
    void disableCompression();

    /**
     * Set security configuration
     */
    void setSecurityConfig(SecurityConfig securityConfig);

    /**
     * Get security configuration
     */
    SecurityConfig getSecurityConfig();
}
