package cn.huiwings.tcprest.protocol.v2;

import cn.huiwings.tcprest.mapper.Mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Protocol V2 type support rules: which types can be serialized/deserialized.
 * Used at resource registration time to validate DTO/parameter/return types;
 * rules align with {@link cn.huiwings.tcprest.parser.v2.ProtocolV2Parser} and
 * {@link cn.huiwings.tcprest.codec.v2.ProtocolV2Codec}.
 *
 * @since 1.1.0
 */
public final class ProtocolV2TypeSupport {

    private ProtocolV2TypeSupport() {
    }

    /**
     * Collect fully qualified type names that are not supported (no Serializable, no mapper).
     * Matches Protocol V2 semantics: primitives, String, wrappers, collection interfaces,
     * primitive/String arrays are supported; other types need Serializable or mapper.
     *
     * @param resourceClass the resource class to scan (methods' params and return types)
     * @param mappers       mapper registry (may be null)
     * @return list of unsupported type canonical names
     */
    public static List<String> collectUnsupportedTypes(Class<?> resourceClass, Map<String, Mapper> mappers) {
        List<String> unsupported = new ArrayList<>();
        for (java.lang.reflect.Method method : resourceClass.getMethods()) {
            if (method.getDeclaringClass() == Object.class) {
                continue;
            }
            for (Class<?> paramType : method.getParameterTypes()) {
                if (!isTypeSupported(paramType, mappers)) {
                    String name = paramType.getCanonicalName();
                    if (name != null && !unsupported.contains(name)) {
                        unsupported.add(name);
                    }
                }
            }
            Class<?> returnType = method.getReturnType();
            if (returnType != void.class && !isTypeSupported(returnType, mappers)) {
                String name = returnType.getCanonicalName();
                if (name != null && !unsupported.contains(name)) {
                    unsupported.add(name);
                }
            }
        }
        return unsupported;
    }

    /**
     * Whether the type is supported by Protocol V2 (can be serialized/deserialized).
     */
    public static boolean isTypeSupported(Class<?> type, Map<String, Mapper> mappers) {
        if (type == null || type == void.class) {
            return true;
        }
        if (type.isPrimitive()) {
            return true;
        }
        if (type == String.class) {
            return true;
        }
        if (isWrapperType(type)) {
            return true;
        }
        if (isCommonCollectionInterface(type)) {
            return true;
        }
        if (type.isArray()) {
            Class<?> component = type.getComponentType();
            if (component.isPrimitive() || component == String.class) {
                return true;
            }
            return java.io.Serializable.class.isAssignableFrom(component)
                || (mappers != null && mappers.containsKey(component.getCanonicalName()));
        }
        return java.io.Serializable.class.isAssignableFrom(type)
            || (mappers != null && mappers.containsKey(type.getCanonicalName()));
    }

    private static boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class || clazz == Double.class
            || clazz == Float.class || clazz == Boolean.class || clazz == Byte.class
            || clazz == Short.class || clazz == Character.class;
    }

    private static boolean isCommonCollectionInterface(Class<?> clazz) {
        return clazz == java.util.List.class || clazz == java.util.Map.class
            || clazz == java.util.Set.class || clazz == java.util.Queue.class
            || clazz == java.util.Deque.class || clazz == java.util.Collection.class;
    }
}
