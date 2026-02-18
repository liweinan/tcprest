package cn.huiwings.tcprest.converter;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.mapper.Mapper;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Protocol converter for transforming method calls into wire protocol format.
 *
 * <p>The Converter interface is the counterpart of {@link cn.huiwings.tcprest.extractor.Extractor}.
 * While Extractor parses incoming requests on the server side, Converter encodes outgoing
 * requests on the client side.</p>
 *
 * <p><b>Primary use case:</b> TcpRest client library uses Converter to transform method
 * invocations into protocol-compliant request strings that can be transmitted over TCP.</p>
 *
 * <p><b>Implementations:</b></p>
 * <ul>
 *   <li>{@link cn.huiwings.tcprest.converter.DefaultConverter} - Protocol V1 (legacy, deprecated)</li>
 *   <li>{@link cn.huiwings.tcprest.converter.v2.ProtocolV2Converter} - Protocol V2 (recommended)</li>
 * </ul>
 *
 * <p><b>Typical workflow:</b></p>
 * <pre>
 * // Client-side encoding
 * Converter converter = new ProtocolV2Converter();
 * String request = converter.encode(MyService.class, method, args, mappers);
 * // → "V2|0|{{base64(META)}}|[param1,param2]"
 *
 * // Server sends response, client decodes
 * Object[] resultParams = converter.decode(method, responseBody, mappers);
 * </pre>
 *
 * @author Weinan Li
 * @since 1.0.0
 * @see cn.huiwings.tcprest.extractor.Extractor
 * @see cn.huiwings.tcprest.converter.v2.ProtocolV2Converter
 */
public interface Converter {
    /**
     * Encode a method invocation into protocol request format.
     *
     * <p>Converts a method call (class, method, parameters) into a protocol-compliant
     * request string that can be transmitted to the server.</p>
     *
     * <p><b>Example (V2 protocol):</b></p>
     * <pre>
     * Method method = Calculator.class.getMethod("add", int.class, int.class);
     * Object[] params = new Object[]{5, 3};
     * String request = converter.encode(Calculator.class, method, params, null);
     * // → "V2|0|{{base64(Calculator/add(II))}}|[NQ==,Mw==]"
     * </pre>
     *
     * @param clazz the target service class (interface or implementation)
     * @param method the method to invoke
     * @param params the method parameters (may be null or empty for no-arg methods)
     * @param mappers optional custom mappers for parameter serialization (may be null)
     * @return protocol request string ready for transmission
     * @throws MapperNotFoundException if a required mapper is not found
     */
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException;

    /**
     * Decode response body into method return value(s).
     *
     * <p>Parses the response body from server and converts it back to Java objects
     * according to the method's return type.</p>
     *
     * <p><b>Note:</b> This method is primarily used for decoding response bodies,
     * not request parameters.</p>
     *
     * @param targetMethod the method being invoked (used to determine return type)
     * @param paramToken the encoded response body string
     * @param mappers optional custom mappers for deserialization (may be null)
     * @return decoded parameters as object array
     * @throws MapperNotFoundException if a required mapper is not found
     */
    public Object[] decode(Method targetMethod, String paramToken, Map<String, Mapper> mappers) throws MapperNotFoundException;

    /**
     * Encode a single parameter value into protocol format.
     *
     * <p>Wraps or encodes a single parameter string (typically Base64-encoded)
     * according to protocol specifications.</p>
     *
     * <p><b>V2 protocol example:</b></p>
     * <pre>
     * encodeParam("hello")  → "aGVsbG8="  (Base64)
     * encodeParam(null)     → "~"         (null marker)
     * encodeParam("")       → ""          (empty string)
     * </pre>
     *
     * @param message the parameter string to encode
     * @return encoded parameter string
     */
    public String encodeParam(String message);

    /**
     * Decode a single parameter value from protocol format.
     *
     * <p>Reverses the {@link #encodeParam(String)} operation, converting
     * protocol-encoded parameter back to its original string representation.</p>
     *
     * <p><b>V2 protocol example:</b></p>
     * <pre>
     * decodeParam("aGVsbG8=")  → "hello"  (Base64 decoded)
     * decodeParam("~")         → null     (null marker)
     * decodeParam("")          → ""       (empty string)
     * </pre>
     *
     * @param message the encoded parameter string
     * @return decoded parameter string (may be null)
     */
    public String decodeParam(String message);

    /**
     * Retrieve a mapper for the specified target class.
     *
     * @param mappers the mapper registry
     * @param targetClazz the class needing serialization/deserialization
     * @return the corresponding mapper
     * @throws MapperNotFoundException if no mapper is registered for the target class
     */
    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) throws MapperNotFoundException;

    /**
     * Retrieve a mapper for the specified target class name.
     *
     * @param mappers the mapper registry
     * @param targetClazzName the fully qualified class name needing serialization/deserialization
     * @return the corresponding mapper
     * @throws MapperNotFoundException if no mapper is registered for the target class
     */
    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) throws MapperNotFoundException;
}
