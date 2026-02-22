package cn.huiwings.tcprest.server;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.security.SecurityConfig;

/**
 * TCP REST server interface. Extends {@link ResourceRegister} for resource registration,
 * {@link MapperRegister} for mapper registration; adds lifecycle (up/down) and
 * server configuration (compression, security, port).
 *
 * @author Weinan Li
 * @date Jul 29 2012
 */
public interface TcpRestServer extends ResourceRegister, MapperRegister {

    void up();

    void up(boolean setDaemon);

    void down();

    int getServerPort();

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
