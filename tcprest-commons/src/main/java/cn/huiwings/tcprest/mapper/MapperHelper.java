package cn.huiwings.tcprest.mapper;

import java.util.*;

/**
 * Helper class providing default mappers for common types.
 *
 * <p><b>Design Note:</b> Primitive types (int, long, double, etc.), wrapper classes, and String
 * are NOT included in DEFAULT_MAPPERS because Protocol V2's {@code convertToType()} method
 * handles them natively using direct parsing (Integer.parseInt(), etc.). This approach:</p>
 * <ul>
 *   <li>Reduces code complexity - no need for redundant mappers</li>
 *   <li>Improves performance - direct parsing is faster than mapper lookup + delegation</li>
 *   <li>Maintains clean separation - protocol-level types vs user-defined custom types</li>
 * </ul>
 *
 * <p><b>Included Mappers:</b></p>
 * <ul>
 *   <li>{@link RawTypeMapper} - For collections (List, Set, Map, Queue) and custom Serializable objects</li>
 *   <li>{@link ExceptionMapper} - For exception message transfer (security: no stack traces)</li>
 * </ul>
 *
 * <p><b>Custom Mappers:</b> Users can add custom mappers via:</p>
 * <pre>{@code
 * server.addMapper(MyCustomType.class.getCanonicalName(), new MyCustomMapper());
 * }</pre>
 *
 * @author Weinan Li
 * @date 07 31 2012
 */
public class MapperHelper {

    public static final HashMap<String, Mapper> DEFAULT_MAPPERS = new HashMap<String, Mapper>();

    static {
        // Collection mappers (use Java serialization for complex types)
        DEFAULT_MAPPERS.put(Collection.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(Set.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(List.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(Queue.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(Map.class.getCanonicalName(), new RawTypeMapper());

        // Exception mapper (transfers exception messages, not full stack traces for security)
        DEFAULT_MAPPERS.put(Exception.class.getCanonicalName(), new ExceptionMapper());
    }
}
