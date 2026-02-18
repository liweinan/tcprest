package cn.huiwings.tcprest.extractor;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.server.Context;

/**
 * Protocol extractor for parsing incoming requests into executable method contexts.
 *
 * <p>The Extractor interface is the counterpart of {@link cn.huiwings.tcprest.converter.Converter}.
 * While Converter encodes requests on the client side, Extractor parses incoming requests
 * on the server side and extracts the method invocation details.</p>
 *
 * <p><b>Primary use case:</b> TcpRest server uses Extractor to parse incoming protocol
 * requests and extract:
 * <ul>
 *   <li>Target service class</li>
 *   <li>Target method (with exact signature for overloading)</li>
 *   <li>Deserialized method parameters</li>
 * </ul>
 * </p>
 *
 * <p><b>Implementations:</b></p>
 * <ul>
 *   <li>{@link cn.huiwings.tcprest.extractor.DefaultExtractor} - Protocol V1 (legacy, deprecated)</li>
 *   <li>{@link cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor} - Protocol V2 (recommended)</li>
 * </ul>
 *
 * <p><b>Typical workflow:</b></p>
 * <pre>
 * // Server receives request from client
 * String request = "V2|0|{{base64(Calculator/add(II))}}|[NQ==,Mw==]";
 *
 * // Extract method invocation details
 * Extractor extractor = new ProtocolV2Extractor();
 * Context context = extractor.extract(request);
 *
 * // Context contains:
 * // - targetClass: Calculator.class
 * // - targetMethod: add(int, int)
 * // - params: [5, 3]
 *
 * // Server then invokes the method
 * Object result = context.getTargetMethod().invoke(instance, context.getParams());
 * </pre>
 *
 * <p><b>Protocol Detection:</b></p>
 * <p>Servers typically use {@link cn.huiwings.tcprest.server.ProtocolRouter} to automatically
 * detect protocol version and route to the appropriate Extractor implementation.</p>
 *
 * @author Weinan Li
 * @since 1.0.0
 * @see cn.huiwings.tcprest.converter.Converter
 * @see cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor
 * @see cn.huiwings.tcprest.server.Context
 * @see cn.huiwings.tcprest.server.ProtocolRouter
 */
public interface Extractor {
    /**
     * Extract method invocation context from protocol request string.
     *
     * <p>Parses the incoming request and extracts all necessary information
     * to invoke the target method: target class, target method (with signature),
     * and deserialized parameters.</p>
     *
     * <p><b>Example (V2 protocol):</b></p>
     * <pre>
     * String request = "V2|0|{{Y2FsYy9hZGQoSUkp}}|[NQ==,Mw==]";
     * Context context = extractor.extract(request);
     *
     * context.getTargetClass()   // → Calculator.class
     * context.getTargetMethod()  // → public int add(int, int)
     * context.getParams()        // → [5, 3]
     * </pre>
     *
     * <p><b>V2 Protocol Features:</b></p>
     * <ul>
     *   <li>Method signature support enables overloading</li>
     *   <li>Intelligent mapper system (user-defined → collections → auto-serialization → built-in)</li>
     *   <li>Security features (checksum verification, class whitelist)</li>
     * </ul>
     *
     * @param request the protocol request string from client
     * @return Context object containing target class, method, and deserialized parameters
     * @throws ClassNotFoundException if the target service class cannot be found
     * @throws NoSuchMethodException if the target method (with signature) cannot be found
     * @throws ParseException if the request format is invalid or parsing fails
     * @throws MapperNotFoundException if a required custom mapper is not found
     * @see cn.huiwings.tcprest.server.Context
     */
    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException, MapperNotFoundException;
}
