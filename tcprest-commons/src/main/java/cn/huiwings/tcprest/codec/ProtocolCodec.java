package cn.huiwings.tcprest.codec;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.mapper.Mapper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Protocol codec for encoding/decoding method calls in TcpRest wire protocol.
 *
 * <p>ProtocolCodec handles bidirectional transformation between Java method invocations
 * and protocol-compliant request/response strings transmitted over TCP.</p>
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *   <li><b>Encoding (Client-side):</b> Transform method calls into protocol request strings</li>
 *   <li><b>Decoding (Client-side):</b> Parse response strings back into Java objects</li>
 *   <li><b>Parameter serialization:</b> Handle individual parameter encoding/decoding</li>
 *   <li><b>Mapper resolution:</b> Find appropriate serializers for custom types</li>
 * </ul>
 *
 * <p><b>Counterpart:</b> {@link cn.huiwings.tcprest.parser.RequestParser} performs the reverse
 * operation on the server side, parsing incoming requests into executable method contexts.</p>
 *
 * <p><b>Implementations:</b></p>
 * <ul>
 *   <li>{@link cn.huiwings.tcprest.codec.DefaultProtocolCodec} - Protocol V1 (legacy, deprecated)</li>
 *   <li>{@link cn.huiwings.tcprest.codec.v2.ProtocolV2Codec} - Protocol V2 (recommended)</li>
 *   <li>{@link cn.huiwings.tcprest.compression.CompressingProtocolCodec} - Compression decorator</li>
 * </ul>
 *
 * <p><b>Typical client-side workflow:</b></p>
 * <pre>
 * // 1. Client encodes method call
 * ProtocolCodec codec = new ProtocolV2Codec();
 * String request = codec.encode(Calculator.class, addMethod, new Object[]{5, 3}, mappers);
 * // → "V2|0|{{base64(Calculator/add(II))}}|[NQ==,Mw==]"
 *
 * // 2. Send request over TCP
 * socket.getOutputStream().write(request.getBytes());
 *
 * // 3. Receive response from server
 * String response = readResponse(socket);
 *
 * // 4. Client decodes response
 * Object[] result = codec.decode(addMethod, responseBody, mappers);
 * // → [8]
 * </pre>
 *
 * <p><b>Thread Safety:</b> Implementations should be thread-safe as they may be
 * shared across multiple client threads.</p>
 *
 * <p><b>History:</b> Renamed from {@code Converter} in version 1.1.0 to better
 * reflect its bidirectional encoding/decoding responsibility. The term "Codec"
 * (coder-decoder) is industry standard and more accurate than "Converter".</p>
 *
 * @author Weinan Li
 * @since 1.1.0
 * @see cn.huiwings.tcprest.parser.RequestParser
 * @see cn.huiwings.tcprest.codec.v2.ProtocolV2Codec
 */
public interface ProtocolCodec {
    /**
     * Encode a method invocation into protocol request format.
     *
     * <p>Transforms a method call (class, method, parameters) into a protocol-compliant
     * request string that can be transmitted to the server over TCP.</p>
     *
     * <p><b>Encoding steps:</b></p>
     * <ol>
     *   <li>Build metadata (class name, method name, method signature)</li>
     *   <li>Serialize each parameter using appropriate mappers</li>
     *   <li>Apply protocol-specific encoding (Base64, compression, etc.)</li>
     *   <li>Assemble final protocol string with delimiters</li>
     *   <li>Optionally add integrity checksum</li>
     * </ol>
     *
     * <p><b>Example (V2 protocol):</b></p>
     * <pre>
     * Method method = Calculator.class.getMethod("add", int.class, int.class);
     * Object[] params = new Object[]{5, 3};
     * String request = codec.encode(Calculator.class, method, params, null);
     * // → "V2|0|{{base64(Calculator/add(II))}}|[NQ==,Mw==]"
     * </pre>
     *
     * @param clazz the target service class (interface or implementation)
     * @param method the method to invoke
     * @param params the method parameters (may be null or empty for no-arg methods)
     * @param mappers optional custom mappers for parameter serialization (may be null)
     * @return protocol request string ready for transmission
     * @throws MapperNotFoundException if a required mapper is not found for parameter types
     */
    String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException;

    /**
     * Decode response body into method return value(s).
     *
     * <p>Parses the response body from server and converts it back to Java objects
     * according to the method's return type. Uses the method signature to determine
     * the expected return type and select appropriate deserializers.</p>
     *
     * <p><b>Decoding steps:</b></p>
     * <ol>
     *   <li>Parse protocol response format</li>
     *   <li>Extract encoded parameters/return values</li>
     *   <li>Determine target types from method signature</li>
     *   <li>Deserialize using appropriate mappers</li>
     *   <li>Handle special cases (null values, primitives, arrays)</li>
     * </ol>
     *
     * <p><b>Note:</b> This method is primarily used for decoding response bodies,
     * not request parameters. Server-side parameter parsing is handled by
     * {@link cn.huiwings.tcprest.parser.RequestParser}.</p>
     *
     * @param targetMethod the method being invoked (used to determine return type)
     * @param paramToken the encoded response body string
     * @param mappers optional custom mappers for deserialization (may be null)
     * @return decoded parameters as object array
     * @throws MapperNotFoundException if a required mapper is not found
     */
    Object[] decode(Method targetMethod, String paramToken, Map<String, Mapper> mappers) throws MapperNotFoundException;

    /**
     * Encode a single parameter value into protocol format.
     *
     * <p>Wraps or encodes a single parameter string (typically Base64-encoded)
     * according to protocol specifications. Used internally by {@link #encode}
     * for individual parameter serialization.</p>
     *
     * <p><b>V2 protocol examples:</b></p>
     * <pre>
     * encodeParam("hello")  → "aGVsbG8="  (Base64)
     * encodeParam(null)     → "~"         (null marker)
     * encodeParam("")       → ""          (empty string)
     * </pre>
     *
     * @param message the parameter string to encode
     * @return encoded parameter string
     */
    String encodeParam(String message);

    /**
     * Decode a single parameter value from protocol format.
     *
     * <p>Reverses the {@link #encodeParam(String)} operation, converting
     * protocol-encoded parameter back to its original string representation.</p>
     *
     * <p><b>V2 protocol examples:</b></p>
     * <pre>
     * decodeParam("aGVsbG8=")  → "hello"  (Base64 decoded)
     * decodeParam("~")         → null     (null marker)
     * decodeParam("")          → ""       (empty string)
     * </pre>
     *
     * @param message the encoded parameter string
     * @return decoded parameter string (may be null)
     */
    String decodeParam(String message);

    /**
     * Retrieve a mapper for the specified target class.
     *
     * <p>Resolves the appropriate {@link Mapper} for serializing/deserializing
     * instances of the given class. The resolution strategy typically follows:</p>
     * <ol>
     *   <li>Check user-provided custom mappers</li>
     *   <li>Check for collection/array types</li>
     *   <li>Check for Serializable types (use RawTypeMapper)</li>
     *   <li>Check built-in mappers (primitives, String, etc.)</li>
     * </ol>
     *
     * @param mappers the mapper registry (may be null)
     * @param targetClazz the class needing serialization/deserialization
     * @return the corresponding mapper
     * @throws MapperNotFoundException if no mapper is registered for the target class
     */
    Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) throws MapperNotFoundException;

    /**
     * Retrieve a mapper for the specified target class name.
     *
     * <p>Class-name-based variant of {@link #getMapper(Map, Class)}.
     * Used when the actual Class object is not yet loaded (e.g., during
     * protocol parsing on the server side).</p>
     *
     * @param mappers the mapper registry (may be null)
     * @param targetClazzName the fully qualified class name needing serialization/deserialization
     * @return the corresponding mapper
     * @throws MapperNotFoundException if no mapper is registered for the target class
     */
    Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) throws MapperNotFoundException;
}
