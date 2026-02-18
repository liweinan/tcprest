package cn.huiwings.tcprest.protocol;

/**
 * TcpRest Protocol Constants.
 *
 * <p><b>Security-Enhanced Protocol Format (2026-02-18):</b></p>
 *
 * <p><b>V1 Request:</b> {@code 0|COMP|META|PARAMS|CHK:value}</p>
 * <ul>
 *   <li>{@code 0} - Compression flag (0=none, 1=gzip)</li>
 *   <li>{@code COMP} - Compression indicator</li>
 *   <li>{@code META} - Base64-encoded metadata: {@code ClassName/methodName}</li>
 *   <li>{@code PARAMS} - Base64-encoded parameters</li>
 *   <li>{@code CHK:value} - Optional checksum (CRC32 or HMAC)</li>
 * </ul>
 *
 * <p><b>V1 Response:</b> {@code 0|STATUS|RESULT|CHK:value}</p>
 *
 * <p><b>V2 Request:</b> {@code V2|COMP|META|PARAMS|CHK:value}</p>
 * <ul>
 *   <li>{@code META} - Base64-encoded: {@code ClassName/methodName(TypeSignature)}</li>
 * </ul>
 *
 * <p><b>V2 Response:</b> {@code V2|COMP|STATUS|RESULT|CHK:value}</p>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>All variable content (class names, method names, parameters) are Base64-encoded</li>
 *   <li>Prevents injection attacks (path traversal, delimiter injection, etc.)</li>
 *   <li>Optional integrity verification via CRC32 or HMAC-SHA256</li>
 *   <li>Optional class name whitelist validation</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 07 31 2012
 */
public class TcpRestProtocol {
    /** Main component separator in protocol */
    public static final String COMPONENT_SEPARATOR = "|";

    /**
     * Parameter separator (used within encoded param block in V1 protocol).
     * @deprecated V1 protocol specific. V2 uses comma separator. Use {@link cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants#PARAM_SEPARATOR} for V2.
     */
    @Deprecated
    public static final String PARAM_SEPARATOR = ":::";

    /**
     * Null object marker (V1 protocol).
     * @deprecated V1 protocol specific. V2 uses "NULL" marker in parameter arrays.
     */
    @Deprecated
    public static final String NULL = "TCPREST.NULL";

    /** V2 protocol version prefix */
    public static final String V2_PREFIX = "V2";

    /** Checksum prefix */
    public static final String CHECKSUM_PREFIX = "CHK:";

    /** Legacy separator name for backward compatibility */
    @Deprecated
    public static final String PATH_SEPERATOR = PARAM_SEPARATOR;
}
