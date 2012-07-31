package io.tcprest.mapper;

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
        DEFAULT_MAPPERS.put(String.class.getCanonicalName(), new StringMapper());
        DEFAULT_MAPPERS.put(Boolean.class.getCanonicalName(), new BooleanMapper());
        DEFAULT_MAPPERS.put(Integer.class.getCanonicalName(), new IntegerMapper());
    }
}
