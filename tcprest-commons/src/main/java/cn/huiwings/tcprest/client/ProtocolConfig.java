package cn.huiwings.tcprest.client;

import cn.huiwings.tcprest.protocol.ProtocolVersion;

/**
 * Configuration for client protocol version.
 *
 * <p>This class allows clients to specify which protocol version to use
 * when communicating with the server.</p>
 *
 * <p><b>Usage:</b></p>
 * <pre>
 * // Use Protocol v2 (default, recommended)
 * ProtocolConfig config = new ProtocolConfig();
 *
 * // Use Protocol v1 (legacy)
 * ProtocolConfig config = new ProtocolConfig(ProtocolVersion.V1);
 *
 * // Or via factory:
 * TcpRestClientFactory factory = new TcpRestClientFactory(...)
 *     .withProtocolV2();  // Default
 * </pre>
 *
 * @since 1.1.0
 * @version 2.0 (2026-02-19) - Default protocol changed to V2
 */
public class ProtocolConfig {

    private ProtocolVersion version;

    /**
     * Create configuration with default protocol (V2).
     *
     * <p><b>Note:</b> As of version 2.0, the default protocol is V2.
     * V2 provides better performance, cleaner format, and supports method overloading.</p>
     */
    public ProtocolConfig() {
        this.version = ProtocolVersion.V2;
    }

    /**
     * Create configuration with specified protocol version.
     *
     * @param version the protocol version
     */
    public ProtocolConfig(ProtocolVersion version) {
        this.version = version != null ? version : ProtocolVersion.V2;
    }

    /**
     * Get the protocol version.
     *
     * @return protocol version
     */
    public ProtocolVersion getVersion() {
        return version;
    }

    /**
     * Set the protocol version.
     *
     * @param version the protocol version
     */
    public void setVersion(ProtocolVersion version) {
        this.version = version != null ? version : ProtocolVersion.V1;
    }

    /**
     * Check if using Protocol v2.
     *
     * @return true if v2, false otherwise
     */
    public boolean isV2() {
        return version == ProtocolVersion.V2;
    }

    /**
     * Check if using Protocol v1.
     *
     * @return true if v1, false otherwise
     */
    public boolean isV1() {
        return version == ProtocolVersion.V1;
    }
}
