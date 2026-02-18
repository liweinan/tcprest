package cn.huiwings.tcprest.protocol;

/**
 * Protocol version enum for TcpRest protocol versioning.
 *
 * <p>This enum defines the supported protocol versions and auto-detection mode.
 * Protocol v2 adds method signature support for overloading and exception propagation.</p>
 *
 * @since 1.1.0
 */
public enum ProtocolVersion {
    /**
     * Protocol v1 - Original protocol without method signatures or exception propagation.
     * Request format: ClassName/methodName(PARAMS)
     * Response format: base64-encoded result or NullObj
     */
    V1,

    /**
     * Protocol v2 - Enhanced protocol with method signatures and status codes.
     * Request format: V2|COMPRESSION|ClassName/methodName(TYPE_SIGNATURE)(PARAMS)
     * Response format: V2|COMPRESSION|STATUS|BODY
     *
     * <p>Features:</p>
     * <ul>
     *   <li>Method signatures enable overloading support</li>
     *   <li>Status codes enable exception propagation</li>
     *   <li>Full backward compatibility with v1</li>
     * </ul>
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
