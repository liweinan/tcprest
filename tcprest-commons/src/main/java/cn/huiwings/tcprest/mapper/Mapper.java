package cn.huiwings.tcprest.mapper;

/**
 * Custom type serialization/deserialization strategy for TcpRest protocol.
 *
 * <p>The Mapper interface allows users to define custom serialization logic for
 * complex types that cannot be handled by built-in converters. Mappers provide
 * bidirectional conversion between Java objects and their string representations.</p>
 *
 * <p><b>Primary use cases:</b></p>
 * <ul>
 *   <li>Custom serialization format (e.g., JSON via Gson/Jackson)</li>
 *   <li>Third-party types that aren't {@link java.io.Serializable}</li>
 *   <li>Special handling for specific types (e.g., Date formatting)</li>
 *   <li>Performance optimization (custom binary format)</li>
 * </ul>
 *
 * <p><b>V2 Protocol Mapper Priority:</b></p>
 * <p>In Protocol V2, mappers are checked in this order:</p>
 * <ol>
 *   <li><b>User-defined Mapper</b> (highest priority) - registered via this interface</li>
 *   <li><b>Collection interfaces</b> - List, Map, Set (built-in support)</li>
 *   <li><b>Auto-serialization</b> - {@link java.io.Serializable} types via {@link RawTypeMapper}</li>
 *   <li><b>Built-in types</b> - primitives, wrappers, String, arrays</li>
 * </ol>
 *
 * <p><b>Example 1: JSON Mapper with Gson</b></p>
 * <pre>
 * public class GsonUserMapper implements Mapper {
 *     private final Gson gson = new Gson();
 *
 *     &#64;Override
 *     public String objectToString(Object object) {
 *         return gson.toJson(object);  // User → {"name":"Alice","age":25}
 *     }
 *
 *     &#64;Override
 *     public Object stringToObject(String param) {
 *         return gson.fromJson(param, User.class);  // JSON → User object
 *     }
 * }
 *
 * // Register mapper
 * server.addMapper(User.class.getName(), new GsonUserMapper());
 * </pre>
 *
 * <p><b>Example 2: Date Mapper with Custom Format</b></p>
 * <pre>
 * public class DateMapper implements Mapper {
 *     private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
 *
 *     &#64;Override
 *     public String objectToString(Object object) {
 *         return format.format((Date) object);  // Date → "2026-02-19"
 *     }
 *
 *     &#64;Override
 *     public Object stringToObject(String param) {
 *         try {
 *             return format.parse(param);  // "2026-02-19" → Date
 *         } catch (ParseException e) {
 *             throw new RuntimeException(e);
 *         }
 *     }
 * }
 * </pre>
 *
 * <p><b>Built-in Mappers:</b></p>
 * <ul>
 *   <li>{@link RawTypeMapper} - Java serialization for {@link java.io.Serializable} types</li>
 *   <li>{@link IntegerMapper}, {@link LongMapper}, etc. - Primitive wrappers (V1 only)</li>
 *   <li>{@link StringMapper} - String type (V1 only)</li>
 * </ul>
 *
 * <p><b>Registration:</b></p>
 * <pre>
 * // Server-side
 * server.addMapper(User.class.getName(), new GsonUserMapper());
 *
 * // Client-side
 * Map&lt;String, Mapper&gt; mappers = new HashMap&lt;&gt;();
 * mappers.put(User.class.getName(), new GsonUserMapper());
 * TcpRestClientFactory factory = new TcpRestClientFactory(
 *     UserService.class, "localhost", 8080, mappers
 * );
 * </pre>
 *
 * <p><b>Important Notes:</b></p>
 * <ul>
 *   <li>Both client and server must use the same Mapper implementation</li>
 *   <li>Mapper is registered by fully qualified class name</li>
 *   <li>{@link #stringToObject(String)} should handle null/empty strings gracefully</li>
 *   <li>For V2 protocol, string output is typically Base64-encoded by the protocol layer</li>
 * </ul>
 *
 * @author Weinan Li
 * @since 1.0.0
 * @see RawTypeMapper
 * @see cn.huiwings.tcprest.converter.v2.ProtocolV2Converter
 * @see cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor
 */
public interface Mapper {
    /**
     * Convert string representation to Java object.
     *
     * <p>This method is called during request/response deserialization to
     * convert the protocol string back into the original Java object.</p>
     *
     * <p><b>Implementation guidelines:</b></p>
     * <ul>
     *   <li>Handle null or empty input gracefully</li>
     *   <li>Throw descriptive exceptions for invalid input</li>
     *   <li>Return type should match the expected class</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Gson mapper
     * public Object stringToObject(String param) {
     *     if (param == null || param.isEmpty()) {
     *         return null;
     *     }
     *     return gson.fromJson(param, User.class);
     * }
     * </pre>
     *
     * @param param the string representation (may be null or empty)
     * @return the deserialized Java object (may be null)
     */
    public Object stringToObject(String param);

    /**
     * Convert Java object to string representation.
     *
     * <p>This method is called during request/response serialization to
     * convert the Java object into a string that can be transmitted.</p>
     *
     * <p><b>Implementation guidelines:</b></p>
     * <ul>
     *   <li>Handle null input (typically return null or empty string)</li>
     *   <li>Output should be reversible by {@link #stringToObject(String)}</li>
     *   <li>Avoid including class metadata unless necessary</li>
     * </ul>
     *
     * <p><b>Example:</b></p>
     * <pre>
     * // Gson mapper
     * public String objectToString(Object object) {
     *     if (object == null) {
     *         return null;
     *     }
     *     return gson.toJson(object);  // User → {"name":"Alice","age":25}
     * }
     * </pre>
     *
     * @param object the Java object to serialize (may be null)
     * @return the string representation (may be null or empty)
     */
    public String objectToString(Object object);
}
