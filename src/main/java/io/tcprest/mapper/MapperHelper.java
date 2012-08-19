package io.tcprest.mapper;

import io.tcprest.protocol.NullObj;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Weinan Li
 * @date 07 31 2012
 */
public class MapperHelper {

    public static final Map<String,Mapper> DEFAULT_MAPPERS = new HashMap<String, Mapper>();

    static {
        DEFAULT_MAPPERS.put(Byte.class.getCanonicalName(), new ByteMapper());
        DEFAULT_MAPPERS.put(Long.class.getCanonicalName(), new LongMapper());
        DEFAULT_MAPPERS.put(Float.class.getCanonicalName(), new FloatMapper());
        DEFAULT_MAPPERS.put(Double.class.getCanonicalName(), new DoubleMapper());
        DEFAULT_MAPPERS.put(Short.class.getCanonicalName(), new ShortMapper());
        DEFAULT_MAPPERS.put(Boolean.class.getCanonicalName(), new BooleanMapper());
        DEFAULT_MAPPERS.put(Integer.class.getCanonicalName(), new IntegerMapper());
        DEFAULT_MAPPERS.put(String.class.getCanonicalName(), new StringMapper());
        DEFAULT_MAPPERS.put(NullObj.class.getCanonicalName(), new NullMapper());
        DEFAULT_MAPPERS.put("byte", new ByteMapper());
        DEFAULT_MAPPERS.put("long", new LongMapper());
        DEFAULT_MAPPERS.put("float", new FloatMapper());
        DEFAULT_MAPPERS.put("double", new DoubleMapper());
        DEFAULT_MAPPERS.put("short", new ShortMapper());
        DEFAULT_MAPPERS.put("int", new IntegerMapper());
        DEFAULT_MAPPERS.put("boolean", new BooleanMapper());
        DEFAULT_MAPPERS.put("integer", new IntegerMapper());
    }
}
