package io.tcprest.mapper;

import io.tcprest.protocol.NullObj;

import java.util.*;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class MapperHelper {

    public static final Map<String, Mapper> DEFAULT_MAPPERS = new HashMap<String, Mapper>();

    // todo add Mappers for Collections and Map
    static {
        DEFAULT_MAPPERS.put(Byte.class.getCanonicalName(), new ByteMapper());
        DEFAULT_MAPPERS.put(Long.class.getCanonicalName(), new LongMapper());
        DEFAULT_MAPPERS.put(Float.class.getCanonicalName(), new FloatMapper());
        DEFAULT_MAPPERS.put(Double.class.getCanonicalName(), new DoubleMapper());
        DEFAULT_MAPPERS.put(Short.class.getCanonicalName(), new ShortMapper());
        DEFAULT_MAPPERS.put(Boolean.class.getCanonicalName(), new BooleanMapper());
        DEFAULT_MAPPERS.put(Integer.class.getCanonicalName(), new IntegerMapper());
        DEFAULT_MAPPERS.put(String.class.getCanonicalName(), new StringMapper());

        DEFAULT_MAPPERS.put("byte", new ByteMapper());
        DEFAULT_MAPPERS.put("long", new LongMapper());
        DEFAULT_MAPPERS.put("float", new FloatMapper());
        DEFAULT_MAPPERS.put("double", new DoubleMapper());
        DEFAULT_MAPPERS.put("short", new ShortMapper());
        DEFAULT_MAPPERS.put("int", new IntegerMapper());
        DEFAULT_MAPPERS.put("boolean", new BooleanMapper());
        DEFAULT_MAPPERS.put("integer", new IntegerMapper());

        // Implicitly set all collections as serializable
        // This may cause exceptions if user defined not serializable collections
        DEFAULT_MAPPERS.put(Collection.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(Set.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(List.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(Queue.class.getCanonicalName(), new RawTypeMapper());
        DEFAULT_MAPPERS.put(Map.class.getCanonicalName(), new RawTypeMapper());

        DEFAULT_MAPPERS.put(NullObj.class.getCanonicalName(), new NullMapper());

        DEFAULT_MAPPERS.put(Exception.class.getCanonicalName(), new ExceptionMapper());

    }
}
