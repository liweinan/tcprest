package cn.huiwings.tcprest.protocol;

/**
 * Protocol version enum for TcpRest protocol versioning.
 *
 * <p>This enum defines the supported protocol versions and auto-detection mode.</p>
 *
 * <p><b>Recommended:</b> Use V2 (default) for new applications. V2 provides:
 * cleaner format, method overloading support, exception propagation, and better performance.</p>
 *
 * @since 1.1.0
 * @version 2.0 (2026-02-19) - V2 is now the default and recommended protocol
 */
public enum ProtocolVersion {
    /**
     * Protocol v1 - Legacy protocol (deprecated).
     * Request format: 0|META|PARAMS
     * Response format: 0|base64(result)
     *
     * <p><b>Note:</b> V1 is maintained for backward compatibility only.
     * New applications should use V2.</p>
     */
    V1,

    /**
     * Protocol v2 - Modern simplified protocol (default, recommended).
     *
     * <p><b>Request format:</b> V2|0|{{base64(META)}}|[param1,param2,param3]</p>
     * <p><b>Response format:</b> V2|0|STATUS|{{base64(BODY)}}</p>
     *
     * <p><b>Features:</b></p>
     * <ul>
     *   <li>JSON-style array format for parameters</li>
     *   <li>Method signatures enable overloading support</li>
     *   <li>Status codes enable exception propagation</li>
     *   <li>Cleaner, more readable protocol</li>
     *   <li>Single-layer Base64 encoding (better performance)</li>
     * </ul>
     *
     * <p><b>Default:</b> V2 is the default protocol as of version 2.0 (2026-02-19).</p>
     */
    V2,

    /**
     * Auto-detect protocol version from request format.
     * Server-side default mode that supports both v1 and v2 clients.
     *
     * <p>Detection logic:</p>
     * <ul>
     *   <li>If request starts with "V2|" → use v2</li>
     *   <li>Otherwise → use v1</li>
     * </ul>
     */
    AUTO
}
