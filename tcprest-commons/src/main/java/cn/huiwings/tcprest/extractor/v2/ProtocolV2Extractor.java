package cn.huiwings.tcprest.extractor.v2;

import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.extractor.Extractor;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.TypeSignatureUtil;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.Context;

import java.lang.reflect.Method;
import java.util.Base64;

/**
 * Security-Enhanced Protocol v2 Extractor (Simplified Format).
 *
 * <p>This extractor parses the simplified v2 request format that includes
 * method signatures, enabling precise method selection for overloaded methods.</p>
 *
 * <p><b>Simplified Request Format (2026-02-19):</b></p>
 * <pre>
 * V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,param3]|CHK:value
 *
 * Examples:
 * V2|0|{{Q2FsY3VsYXRvci9hZGQoSUkp}}|[MQ==,Mg==]|CHK:a1b2c3d4
 * V2|0|{{U2VydmljZS9wcm9jZXNzKExqYXZhL2xhbmcvU3RyaW5nO1op}}|[aGVsbG8=,dHJ1ZQ==]|CHK:def567
 * </pre>
 *
 * <p><b>Key Improvements:</b></p>
 * <ul>
 *   <li>JSON-style array format for parameters: [p1,p2,p3]</li>
 *   <li>Single-layer Base64 encoding (no double encoding)</li>
 *   <li>Cleaner, more readable protocol</li>
 *   <li>Easier to parse and debug</li>
 * </ul>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>Metadata Base64-encoded (prevents injection)</li>
 *   <li>Optional checksum verification (CRC32/HMAC)</li>
 *   <li>Class name validation</li>
 *   <li>Method name validation</li>
 *   <li>Optional class whitelist</li>
 * </ul>
 *
 * @since 1.1.0
 * @version 2.0 (2026-02-19) - Simplified format with JSON-style arrays
 */
public class ProtocolV2Extractor implements Extractor {

    private SecurityConfig securityConfig;
    private java.util.Map<String, cn.huiwings.tcprest.mapper.Mapper> mappers;

    /**
     * Create extractor with default security (no checksum, no whitelist).
     */
    public ProtocolV2Extractor() {
        this(null, null);
    }

    /**
     * Create extractor with mappers support.
     *
     * @param mappers mapper registry (optional)
     */
    public ProtocolV2Extractor(java.util.Map<String, cn.huiwings.tcprest.mapper.Mapper> mappers) {
        this(null, mappers);
    }

    /**
     * Create extractor with custom security configuration.
     *
     * @param securityConfig security configuration
     */
    public ProtocolV2Extractor(SecurityConfig securityConfig) {
        this(securityConfig, null);
    }

    /**
     * Create extractor with custom security configuration and mappers.
     *
     * @param securityConfig security configuration
     * @param mappers mapper registry (optional)
     */
    public ProtocolV2Extractor(SecurityConfig securityConfig, java.util.Map<String, cn.huiwings.tcprest.mapper.Mapper> mappers) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
        this.mappers = mappers;
    }

    /**
     * Set security configuration.
     *
     * @param securityConfig security configuration
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
    }

    /**
     * Get security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    /**
     * Extract context from v2 request (simplified format).
     *
     * <p><b>Simplified format:</b> V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,param3]|CHK:value</p>
     *
     * <p>Parses the request to extract:</p>
     * <ul>
     *   <li>Class name</li>
     *   <li>Method name</li>
     *   <li>Method signature (type signature)</li>
     *   <li>Parameters (from JSON-style array)</li>
     * </ul>
     *
     * <p>Uses the type signature to find the exact method, solving the
     * overloading problem.</p>
     *
     * @param request the v2 request string
     * @return Context object with extracted information
     * @throws ClassNotFoundException if class cannot be found
     * @throws NoSuchMethodException if method cannot be found
     * @throws ParseException if parsing fails
     * @throws MapperNotFoundException not thrown in V2 (retained for interface compatibility)
     */
    @Override
    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException, MapperNotFoundException {
        try {
            if (request == null || request.isEmpty()) {
                throw new ParseException("Request cannot be null or empty");
            }

            if (!request.startsWith(ProtocolV2Constants.PREFIX)) {
                throw new ParseException("Not a v2 request: " + request);
            }

            // Step 1: Split checksum if present
            String[] checksumParts = ProtocolSecurity.splitChecksum(request);
            String messageWithoutChecksum = checksumParts[0];
            String checksum = checksumParts[1];

            // Step 2: Verify checksum if present
            if (!checksum.isEmpty()) {
                if (!ProtocolSecurity.verifyChecksum(messageWithoutChecksum, checksum, securityConfig)) {
                    throw new ParseException(
                        "Checksum verification failed - message may have been tampered with"
                    );
                }
            }

            // Step 3: Parse request parts: V2|0|{{META}}|[PARAMS]
            String[] parts = messageWithoutChecksum.split("\\" + ProtocolV2Constants.SEPARATOR, 4);
            if (parts.length < 3) {
                throw new ParseException("Invalid v2 request format: " + request);
            }

            String metaWrapped = parts[2];
            String paramsArray = parts.length > 3 ? parts[3] : "[]";

            // Step 4: Unwrap and decode metadata from {{base64(...)}}
            if (!metaWrapped.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) ||
                !metaWrapped.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
                throw new ParseException("Invalid metadata format, expected {{...}}: " + metaWrapped);
            }

            String metaBase64 = metaWrapped.substring(
                ProtocolV2Constants.PARAM_WRAPPER_START.length(),
                metaWrapped.length() - ProtocolV2Constants.PARAM_WRAPPER_END.length()
            );
            String meta = ProtocolSecurity.decodeComponent(metaBase64);

            // Step 5: Parse class name and method signature
            int slashIndex = meta.indexOf(ProtocolV2Constants.CLASS_METHOD_SEPARATOR);
            if (slashIndex == -1) {
                throw new ParseException("Missing class/method separator: " + meta);
            }

            String className = meta.substring(0, slashIndex);
            String methodPart = meta.substring(slashIndex + 1);

            // Step 6: Validate class name and method name
            // Parse method name and signature
            // Format: methodName(SIGNATURE)
            int firstParenIndex = methodPart.indexOf('(');
            if (firstParenIndex == -1) {
                throw new ParseException("Missing method signature: " + methodPart);
            }

            String methodName = methodPart.substring(0, firstParenIndex);

            // Validate class name
            if (!ProtocolSecurity.isValidClassName(className)) {
                throw new ParseException(
                    "Invalid class name format (possible injection attempt): " + className
                );
            }

            // Check class whitelist if enabled
            if (!securityConfig.isClassAllowed(className)) {
                throw new ParseException(
                    "Class not in whitelist: " + className
                );
            }

            // Validate method name
            if (!ProtocolSecurity.isValidMethodName(methodName)) {
                throw new ParseException(
                    "Invalid method name format (possible injection attempt): " + methodName
                );
            }

            // Step 7: Parse signature
            // Find the signature (between first '(' and first ')')
            int signatureEnd = methodPart.indexOf(')', firstParenIndex);
            if (signatureEnd == -1) {
                throw new ParseException("Malformed method signature: " + methodPart);
            }

            String signature = methodPart.substring(firstParenIndex, signatureEnd + 1);

            // Step 8: Load class
            Class<?> clazz = Class.forName(className);

            // Step 9: Find method by signature
            Method method = TypeSignatureUtil.findMethodBySignature(clazz, methodName, signature);

            // Step 10: Parse parameters from array format
            Object[] params = parseParametersArray(paramsArray, method.getParameterTypes());

            // Step 11: Create and return context
            Context context = new Context();
            context.setTargetClass(clazz);
            context.setTargetMethod(method);
            context.setParams(params);

            return context;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            // Re-throw these as-is (they're declared in the interface)
            throw e;
        } catch (ParseException e) {
            // Re-throw ParseException as-is
            throw e;
        } catch (Exception e) {
            // Wrap all other exceptions as ParseException
            throw new ParseException("Failed to extract v2 request: " + e.getMessage());
        }
    }

    /**
     * Parse parameter array into object array.
     *
     * <p><b>New format:</b> [base64_1,base64_2,base64_3]</p>
     *
     * @param paramsArray the parameters array string (e.g., "[p1,p2,p3]")
     * @param paramTypes the expected parameter types
     * @return array of parameter objects
     * @throws ParseException if parsing fails
     */
    private Object[] parseParametersArray(String paramsArray, Class<?>[] paramTypes) throws ParseException {
        try {
            if (paramsArray == null || paramsArray.isEmpty()) {
                paramsArray = "[]";
            }

            // Validate array format
            if (!paramsArray.startsWith(ProtocolV2Constants.PARAMS_ARRAY_START) ||
                !paramsArray.endsWith(ProtocolV2Constants.PARAMS_ARRAY_END)) {
                throw new ParseException("Invalid parameter array format, expected [...]: " + paramsArray);
            }

            // Extract content between [ and ]
            String arrayContent = paramsArray.substring(1, paramsArray.length() - 1).trim();

            // Handle empty array content
            if (arrayContent.isEmpty()) {
                if (paramTypes.length == 0) {
                    return new Object[0];
                } else if (paramTypes.length == 1) {
                    // Single empty string parameter: [] represents one empty string
                    return new Object[]{""};
                } else {
                    throw new ParseException(
                        "Parameter count mismatch: expected " + paramTypes.length + ", got 0"
                    );
                }
            }

            // Split by parameter separator (comma)
            String[] paramParts = arrayContent.split(ProtocolV2Constants.PARAM_SEPARATOR);

            if (paramParts.length != paramTypes.length) {
                throw new ParseException(
                    "Parameter count mismatch: expected " + paramTypes.length +
                    ", got " + paramParts.length
                );
            }

            Object[] params = new Object[paramTypes.length];

            for (int i = 0; i < paramParts.length; i++) {
                params[i] = parseParameter(paramParts[i].trim(), paramTypes[i]);
            }

            return params;
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw (ParseException) e;
            }
            throw new ParseException("Failed to parse parameters array: " + e.getMessage());
        }
    }

    /**
     * Parse a single parameter with intelligent type mapping.
     *
     * <p><b>Decoding Priority:</b></p>
     * <ol>
     *   <li><b>Null marker</b>: "~" → null (tilde, not in Base64 charset)</li>
     *   <li><b>Empty string</b>: "" → "" (consecutive commas in array)</li>
     *   <li><b>User-defined Mapper</b>: Use custom mapper if provided</li>
     *   <li><b>Common collection interfaces</b>: List, Map, Set, etc. → auto-deserialization via RawTypeMapper</li>
     *   <li><b>Auto Deserialization</b>: For Serializable types, use RawTypeMapper</li>
     *   <li><b>Built-in conversion</b>: For primitives, arrays, and other types</li>
     * </ol>
     *
     * @param paramStr the parameter string (base64-encoded or special marker)
     * @param paramType the expected parameter type
     * @return parsed parameter object
     * @throws ParseException if parsing fails
     */
    private Object parseParameter(String paramStr, Class<?> paramType) throws ParseException {
        try {
            if (paramStr == null) {
                throw new ParseException("Parameter cannot be null");
            }

            // Handle empty string (consecutive commas: [a,,b])
            if (paramStr.isEmpty()) {
                return "";
            }

            // Handle ~ marker for null
            if ("~".equals(paramStr)) {
                return null;
            }

            // Priority 1: User-defined Mapper
            if (mappers != null) {
                cn.huiwings.tcprest.mapper.Mapper mapper = mappers.get(paramType.getName());
                if (mapper != null) {
                    // Decode from Base64 first
                    String decoded = new String(Base64.getDecoder().decode(paramStr));
                    return mapper.stringToObject(decoded);
                }
            }

            // Priority 2: Common collection interfaces (List, Map, Set, etc.)
            // These interfaces aren't Serializable themselves, but their implementations are
            if (isCommonCollectionInterface(paramType)) {
                // Use RawTypeMapper to deserialize the actual implementation (ArrayList, HashMap, etc.)
                cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
                return rawMapper.stringToObject(paramStr);
            }

            // Priority 3: Auto Deserialization for Serializable types
            if (java.io.Serializable.class.isAssignableFrom(paramType) &&
                paramType != String.class &&
                !paramType.isArray() &&
                !isWrapperType(paramType)) {
                // RawTypeMapper expects direct Base64 string (not decoded)
                cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
                return rawMapper.stringToObject(paramStr);
            }

            // Priority 3: Decode from Base64
            String decoded = new String(Base64.getDecoder().decode(paramStr));

            // Priority 4: Convert to expected type
            return convertToType(decoded, paramType);
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw (ParseException) e;
            }
            throw new ParseException("Failed to parse parameter: " + e.getMessage());
        }
    }

    /**
     * Check if a class is a primitive wrapper type.
     *
     * @param clazz the class to check
     * @return true if wrapper type
     */
    private boolean isWrapperType(Class<?> clazz) {
        return clazz == Integer.class || clazz == Long.class || clazz == Double.class ||
               clazz == Float.class || clazz == Boolean.class || clazz == Byte.class ||
               clazz == Short.class || clazz == Character.class;
    }

    /**
     * Check if a class is a common collection interface that should use auto-serialization.
     *
     * <p>These interfaces themselves are not Serializable, but their common implementations
     * (ArrayList, HashMap, HashSet, etc.) are Serializable and can be deserialized via
     * RawTypeMapper.</p>
     *
     * <p><b>Supported interfaces:</b></p>
     * <ul>
     *   <li>java.util.List (ArrayList, LinkedList, etc.)</li>
     *   <li>java.util.Map (HashMap, TreeMap, etc.)</li>
     *   <li>java.util.Set (HashSet, TreeSet, etc.)</li>
     *   <li>java.util.Queue (LinkedList, PriorityQueue, etc.)</li>
     *   <li>java.util.Deque (ArrayDeque, LinkedList, etc.)</li>
     *   <li>java.util.Collection (parent interface)</li>
     * </ul>
     *
     * <p><b>Note:</b> Without generic type information, collections are deserialized as-is
     * (e.g., List&lt;Object&gt;). If specific types are needed, users should provide custom mappers.</p>
     *
     * @param clazz the class to check
     * @return true if it's a common collection interface
     */
    private boolean isCommonCollectionInterface(Class<?> clazz) {
        return clazz == java.util.List.class ||
               clazz == java.util.Map.class ||
               clazz == java.util.Set.class ||
               clazz == java.util.Queue.class ||
               clazz == java.util.Deque.class ||
               clazz == java.util.Collection.class;
    }

    /**
     * Convert string value to expected type.
     *
     * @param value the string value
     * @param targetType the target type
     * @return converted object
     */
    private Object convertToType(String value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        // Handle arrays
        if (targetType.isArray()) {
            return parseArray(value, targetType.getComponentType());
        }

        // Handle primitives and wrappers
        if (targetType == String.class) {
            return value;
        } else if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value);
        } else if (targetType == double.class || targetType == Double.class) {
            return Double.parseDouble(value);
        } else if (targetType == boolean.class || targetType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (targetType == long.class || targetType == Long.class) {
            return Long.parseLong(value);
        } else if (targetType == float.class || targetType == Float.class) {
            return Float.parseFloat(value);
        } else if (targetType == byte.class || targetType == Byte.class) {
            return Byte.parseByte(value);
        } else if (targetType == short.class || targetType == Short.class) {
            return Short.parseShort(value);
        } else if (targetType == char.class || targetType == Character.class) {
            return value.length() > 0 ? value.charAt(0) : '\0';
        }

        // For other types, return string
        return value;
    }

    /**
     * Parse array from string representation like "[1, 2, 3]".
     *
     * @param value the string value
     * @param componentType the array component type
     * @return parsed array
     */
    private Object parseArray(String value, Class<?> componentType) {
        // Remove brackets and split by comma
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Invalid array format: " + value);
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            // Empty array
            return java.lang.reflect.Array.newInstance(componentType, 0);
        }

        String[] parts = content.split(",\\s*");

        if (componentType == int.class) {
            int[] array = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Integer.parseInt(parts[i].trim());
            }
            return array;
        } else if (componentType == double.class) {
            double[] array = new double[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Double.parseDouble(parts[i].trim());
            }
            return array;
        } else if (componentType == long.class) {
            long[] array = new long[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Long.parseLong(parts[i].trim());
            }
            return array;
        } else if (componentType == boolean.class) {
            boolean[] array = new boolean[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Boolean.parseBoolean(parts[i].trim());
            }
            return array;
        } else if (componentType == String.class) {
            String[] array = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = parts[i].trim();
            }
            return array;
        }

        // For other types, use string array as fallback
        return parts;
    }

    /**
     * Check if a request is a v2 request.
     *
     * @param request the request string
     * @return true if v2 request
     */
    public static boolean isV2Request(String request) {
        return request != null && request.startsWith(ProtocolV2Constants.PREFIX);
    }
}
