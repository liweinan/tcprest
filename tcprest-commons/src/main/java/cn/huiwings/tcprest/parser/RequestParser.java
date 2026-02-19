package cn.huiwings.tcprest.parser;

import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.server.Context;

/**
 * Protocol request parser for extracting method invocation context from incoming requests.
 *
 * <p>RequestParser performs the server-side counterpart of {@link cn.huiwings.tcprest.codec.ProtocolCodec}.
 * While ProtocolCodec encodes requests on the client side, RequestParser parses incoming requests
 * on the server side and extracts executable method invocation details.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li><b>Protocol parsing:</b> Decode protocol request strings</li>
 *   <li><b>Class resolution:</b> Find target service class from metadata</li>
 *   <li><b>Method resolution:</b> Match method signature (supports overloading in V2)</li>
 *   <li><b>Parameter deserialization:</b> Convert encoded parameters to Java objects</li>
 *   <li><b>Security validation:</b> Verify checksums, validate class names, enforce whitelists</li>
 * </ul>
 *
 * <p><b>Counterpart:</b> {@link cn.huiwings.tcprest.codec.ProtocolCodec} performs encoding
 * on the client side.</p>
 *
 * <p><b>Implementations:</b></p>
 * <ul>
 *   <li>{@link cn.huiwings.tcprest.parser.DefaultRequestParser} - Protocol V1 (legacy, deprecated)</li>
 *   <li>{@link cn.huiwings.tcprest.parser.v2.ProtocolV2Parser} - Protocol V2 (recommended)</li>
 * </ul>
 *
 * <p><b>Typical server-side workflow:</b></p>
 * <pre>
 * // 1. Server receives request from client
 * String request = readRequest(socket);
 * // → "V2|0|{{base64(Calculator/add(II))}}|[NQ==,Mw==]"
 *
 * // 2. Parse request into executable context
 * RequestParser parser = new ProtocolV2Parser(mappers);
 * Context context = parser.parse(request);
 *
 * // 3. Context contains all necessary information:
 * context.getTargetClass()   // → Calculator.class
 * context.getTargetMethod()  // → public int add(int, int)
 * context.getParams()        // → [5, 3]
 *
 * // 4. Server invokes the method
 * Object result = context.getTargetMethod().invoke(instance, context.getParams());
 * </pre>
 *
 * <p><b>Protocol Detection:</b></p>
 * <p>Servers use {@link cn.huiwings.tcprest.server.AbstractTcpRestServer} to automatically
 * detect protocol version from request prefix and route to the appropriate parser:</p>
 * <pre>
 * if (request.startsWith("V2|")) {
 *     context = v2Parser.parse(request);
 * } else if (request.startsWith("0|")) {
 *     context = v1Parser.parse(request);
 * }
 * </pre>
 *
 * <p><b>Security Considerations:</b></p>
 * <ul>
 *   <li>V2 protocol includes checksum verification (CRC32/HMAC-SHA256)</li>
 *   <li>Class name validation prevents injection attacks</li>
 *   <li>Optional class whitelist restricts allowed service classes</li>
 *   <li>Base64 encoding prevents delimiter injection</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b> Implementations should be thread-safe as they may be
 * shared across multiple server worker threads.</p>
 *
 * <p><b>History:</b> Renamed from {@code Extractor} in version 1.1.0 to better
 * reflect its request parsing responsibility. The term "Parser" is more accurate
 * and aligns with industry standard terminology (JSON parsers, XML parsers, etc.).</p>
 *
 * @author Weinan Li
 * @since 1.1.0
 * @see cn.huiwings.tcprest.codec.ProtocolCodec
 * @see cn.huiwings.tcprest.parser.v2.ProtocolV2Parser
 * @see cn.huiwings.tcprest.server.Context
 */
public interface RequestParser {
    /**
     * Parse method invocation context from protocol request string.
     *
     * <p>Parses the incoming request and extracts all necessary information
     * to invoke the target method: target class, target method (with exact signature
     * for overloading support in V2), and deserialized parameters.</p>
     *
     * <p><b>Parsing steps:</b></p>
     * <ol>
     *   <li>Validate request format and protocol version</li>
     *   <li>Verify integrity checksum (if present)</li>
     *   <li>Decode metadata (class name, method name, signature)</li>
     *   <li>Validate class name (security check)</li>
     *   <li>Check class whitelist (if enabled)</li>
     *   <li>Resolve target class from registered resources</li>
     *   <li>Resolve target method (exact match for V2 overloading)</li>
     *   <li>Deserialize parameters using appropriate mappers</li>
     *   <li>Populate Context object</li>
     * </ol>
     *
     * <p><b>Example (V2 protocol):</b></p>
     * <pre>
     * String request = "V2|0|{{base64(Calculator/add(II))}}|[NQ==,Mw==]";
     * Context context = parser.parse(request);
     *
     * context.getTargetClass()   // → Calculator.class
     * context.getTargetMethod()  // → public int add(int, int)
     * context.getParams()        // → [5, 3]
     * context.getCodec()         // → ProtocolV2Codec instance
     * </pre>
     *
     * <p><b>V1 vs V2 Protocol Differences:</b></p>
     * <table border="1">
     *   <tr>
     *     <th>Feature</th>
     *     <th>V1 Protocol</th>
     *     <th>V2 Protocol</th>
     *   </tr>
     *   <tr>
     *     <td>Method Overloading</td>
     *     <td>❌ Not supported (first name match)</td>
     *     <td>✅ Supported (method signatures)</td>
     *   </tr>
     *   <tr>
     *     <td>Checksum</td>
     *     <td>Optional (CRC32/HMAC)</td>
     *     <td>Optional (CRC32/HMAC)</td>
     *   </tr>
     *   <tr>
     *     <td>Mapper Resolution</td>
     *     <td>Basic (custom → serializable → fail)</td>
     *     <td>Intelligent (custom → collection → auto-serialize → builtin)</td>
     *   </tr>
     * </table>
     *
     * <p><b>Error Handling:</b></p>
     * <ul>
     *   <li><b>ClassNotFoundException:</b> Service class not registered</li>
     *   <li><b>NoSuchMethodException:</b> Method signature not found (V2) or method name not found (V1)</li>
     *   <li><b>ParseException:</b> Invalid protocol format, malformed request</li>
     *   <li><b>MapperNotFoundException:</b> No mapper for custom parameter type</li>
     *   <li><b>SecurityException:</b> Checksum failed, invalid class/method name, class not whitelisted</li>
     * </ul>
     *
     * @param request the protocol request string from client
     * @return Context object containing target class, method, deserialized parameters, and codec
     * @throws ClassNotFoundException if the target service class cannot be found or is not registered
     * @throws NoSuchMethodException if the target method (with signature) cannot be found
     * @throws cn.huiwings.tcprest.exception.ProtocolException if the request format is invalid or parsing fails
     * @throws cn.huiwings.tcprest.exception.SecurityException if security validation fails
     * @see cn.huiwings.tcprest.server.Context
     */
    Context parse(String request) throws ClassNotFoundException, NoSuchMethodException;
}
