package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.security.SecurityConfig;

import java.util.Map;

/**
 * TCP REST server interface. Extends {@link ResourceRegister} for resource registration
 * and lookup; adds lifecycle (up/down) and configuration (mappers, compression, security).
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface TcpRestServer extends ResourceRegister {

    void up();

    void up(boolean setDaemon);

    void down();

    /**
     * Get a cloned copy of mappers. The implementation should ensure thread safety.
     *
     * @return map of canonical class name to mapper
     */
    Map<String, Mapper> getMappers();

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
