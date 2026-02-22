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
 * <p><b>V2 Request:</b> {@code V2|COMP|META|PARAMS|CHK:value|SIG:value}</p>
 * <ul>
 *   <li>{@code META} - Base64-encoded: {@code ClassName/methodName(TypeSignature)}</li>
 *   <li>{@code CHK:value} - Optional integrity checksum (CRC32 or HMAC)</li>
 *   <li>{@code SIG:value} - Optional origin signature (e.g. SIG:RSA:base64)</li>
 *   <li>When both present, order is CHK then SIG</li>
 * </ul>
 *
 * <p><b>V2 Response:</b> {@code V2|COMP|STATUS|RESULT|CHK:value|SIG:value}</p>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>All variable content (class names, method names, parameters) are Base64-encoded</li>
 *   <li>Prevents injection attacks (path traversal, delimiter injection, etc.)</li>
 *   <li>Optional integrity verification via CHK (CRC32 or HMAC-SHA256)</li>
 *   <li>Optional origin signature via SIG (e.g. RSA-SHA256; GPG in optional modules)</li>
 *   <li>Optional class name whitelist validation</li>
 * </ul>
 *
 * @author Weinan Li
 * @date 07 31 2012
 */
public class TcpRestProtocol {
    /** Main component separator in protocol */
    public static final String COMPONENT_SEPARATOR = "|";

    /** V2 protocol version prefix */
    public static final String V2_PREFIX = "V2";

    /** Checksum prefix (integrity only) */
    public static final String CHECKSUM_PREFIX = "CHK:";

    /** Signature prefix (origin authentication) */
    public static final String SIGNATURE_PREFIX = "SIG:";

}
