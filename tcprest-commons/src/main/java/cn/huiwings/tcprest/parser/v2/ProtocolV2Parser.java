package cn.huiwings.tcprest.parser.v2;

import cn.huiwings.tcprest.exception.ProtocolException;
import cn.huiwings.tcprest.parser.RequestParser;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.TypeSignatureUtil;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.Context;

import java.lang.reflect.Method;
import java.util.Base64;

/**
 * Security-Enhanced Protocol V2 Request Parser.
 *
 * <p>This parser extracts method invocation context from V2 protocol requests,
 * including method signatures for precise overloading support.</p>
 *
 * <p><b>V2 Request Format:</b></p>
 * <pre>
 * V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,param3]|CHK:value
 *
 * Examples:
 * V2|0|{{Q2FsY3VsYXRvci9hZGQoSUkp}}|[MQ==,Mg==]|CHK:a1b2c3d4
 * V2|0|{{U2VydmljZS9wcm9jZXNzKExqYXZhL2xhbmcvU3RyaW5nO1op}}|[aGVsbG8=,dHJ1ZQ==]|CHK:def567
 * </pre>
 *
 * <p><b>Key Improvements over V1:</b></p>
 * <ul>
 *   <li><b>Method Overloading:</b> Type signatures enable exact method matching</li>
 *   <li><b>JSON-style Arrays:</b> Cleaner parameter format [p1,p2,p3]</li>
 *   <li><b>Single-layer Base64:</b> No double encoding, easier to debug</li>
 *   <li><b>Intelligent Mappers:</b> Auto-serialization, collection support</li>
 * </ul>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>Metadata Base64-encoded (prevents injection)</li>
 *   <li>Optional checksum verification (CRC32/HMAC-SHA256)</li>
 *   <li>Class name validation (regex-based)</li>
 *   <li>Method name validation (regex-based)</li>
 *   <li>Optional class whitelist</li>
 * </ul>
 *
 * <p><b>Intelligent Type Mapping:</b></p>
 * <ol>
 *   <li><b>User-defined Mapper:</b> Custom serialization for specific types</li>
 *   <li><b>Collection Interfaces:</b> List, Map, Set → auto-deserialize implementations</li>
 *   <li><b>Serializable Types:</b> Automatic RawTypeMapper</li>
 *   <li><b>Built-in Types:</b> Primitives, arrays, String</li>
 * </ol>
 *
 * @author Weinan Li
 * @since 1.1.0
 * @version 2.0 (2026-02-19) - JSON-style array format
 */
public class ProtocolV2Parser implements RequestParser {

    private SecurityConfig securityConfig;
    private java.util.Map<String, cn.huiwings.tcprest.mapper.Mapper> mappers;

    /**
     * Create parser with default security (no checksum, no whitelist).
     */
    public ProtocolV2Parser() {
        this(null, null);
    }

    /**
     * Create parser with mappers support.
     *
     * @param mappers mapper registry (optional)
     */
    public ProtocolV2Parser(java.util.Map<String, cn.huiwings.tcprest.mapper.Mapper> mappers) {
        this(null, mappers);
    }

    /**
     * Create parser with custom security configuration.
     *
     * @param securityConfig security configuration
     */
    public ProtocolV2Parser(SecurityConfig securityConfig) {
        this(securityConfig, null);
    }

    /**
     * Create parser with custom security configuration and mappers.
     *
     * @param securityConfig security configuration
     * @param mappers mapper registry (optional)
     */
    public ProtocolV2Parser(SecurityConfig securityConfig, java.util.Map<String, cn.huiwings.tcprest.mapper.Mapper> mappers) {
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
     * Parse context from V2 request.
     *
     * <p><b>Format:</b> V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,param3]|CHK:value</p>
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
     * @throws ProtocolException if parsing fails
     */
    @Override
    public Context parse(String request) throws ClassNotFoundException, NoSuchMethodException {
        try {
            if (request == null || request.isEmpty()) {
                throw new ProtocolException("Request cannot be null or empty");
            }

            if (!request.startsWith(ProtocolV2Constants.PREFIX)) {
                throw new ProtocolException("Not a v2 request: " + request);
            }

            // Step 1: Split checksum if present
            String[] checksumParts = ProtocolSecurity.splitChecksum(request);
            String messageWithoutChecksum = checksumParts[0];
            String checksum = checksumParts[1];

            // Step 2: Verify checksum (enforce server security requirements)
            if (securityConfig.isChecksumEnabled()) {
                // Server requires checksum - client must provide it
                if (checksum.isEmpty()) {
                    throw new cn.huiwings.tcprest.exception.SecurityException(
                        "Server requires " + securityConfig.getChecksumAlgorithm() +
                        " checksum, but client did not provide one"
                    );
                }
                // Verify the provided checksum
                if (!ProtocolSecurity.verifyChecksum(messageWithoutChecksum, checksum, securityConfig)) {
                    throw new cn.huiwings.tcprest.exception.SecurityException(
                        "Checksum verification failed - message may have been tampered with"
                    );
                }
            } else if (!checksum.isEmpty()) {
                // Server doesn't require checksum, but client sent one - still verify it
                if (!ProtocolSecurity.verifyChecksum(messageWithoutChecksum, checksum, securityConfig)) {
                    throw new cn.huiwings.tcprest.exception.SecurityException(
                        "Checksum verification failed - message may have been tampered with"
                    );
                }
            }

            // Step 3: Parse request parts: V2|0|{{META}}|[PARAMS]
            String[] parts = messageWithoutChecksum.split("\\" + ProtocolV2Constants.SEPARATOR, 4);
            if (parts.length < 3) {
                throw new ProtocolException("Invalid v2 request format: " + request);
            }

            String metaWrapped = parts[2];
            String paramsArray = parts.length > 3 ? parts[3] : "[]";

            // Step 4: Unwrap and decode metadata from {{base64(...)}}
            if (!metaWrapped.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) ||
                !metaWrapped.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
                throw new ProtocolException("Invalid metadata format, expected {{...}}: " + metaWrapped);
            }

            String metaBase64 = metaWrapped.substring(
                ProtocolV2Constants.PARAM_WRAPPER_START.length(),
                metaWrapped.length() - ProtocolV2Constants.PARAM_WRAPPER_END.length()
            );
            String meta = ProtocolSecurity.decodeComponent(metaBase64);

            // Step 5: Parse class name and method signature
            int slashIndex = meta.indexOf(ProtocolV2Constants.CLASS_METHOD_SEPARATOR);
            if (slashIndex == -1) {
                throw new ProtocolException("Missing class/method separator: " + meta);
            }

            String className = meta.substring(0, slashIndex);
            String methodPart = meta.substring(slashIndex + 1);

            // Step 6: Validate class name and method name
            // Parse method name and signature
            // Format: methodName(SIGNATURE)
            int firstParenIndex = methodPart.indexOf('(');
            if (firstParenIndex == -1) {
                throw new ProtocolException("Missing method signature: " + methodPart);
            }

            String methodName = methodPart.substring(0, firstParenIndex);

            // Validate class name
            if (!ProtocolSecurity.isValidClassName(className)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Invalid class name format (possible injection attempt): " + className
                );
            }

            // Check class whitelist if enabled
            if (!securityConfig.isClassAllowed(className)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Class not in whitelist: " + className
                );
            }

            // Validate method name
            if (!ProtocolSecurity.isValidMethodName(methodName)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Invalid method name format (possible injection attempt): " + methodName
                );
            }

            // Step 7: Parse signature
            // Find the signature (between first '(' and first ')')
            int signatureEnd = methodPart.indexOf(')', firstParenIndex);
            if (signatureEnd == -1) {
                throw new ProtocolException("Malformed method signature: " + methodPart);
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
        } catch (ProtocolException e) {
            // Re-throw ParseException as-is
            throw e;
        } catch (cn.huiwings.tcprest.exception.SecurityException e) {
            // Re-throw SecurityException as-is (let AbstractTcpRestServer handle it)
            throw e;
        } catch (Exception e) {
            // Wrap all other exceptions as ParseException
            throw new ProtocolException("Failed to parse v2 request: " + e.getMessage());
        }
    }

    /**
     * Parse parameter array into object array.
     *
     * <p><b>Format:</b> [base64_1,base64_2,base64_3]</p>
     *
     * @param paramsArray the parameters array string (e.g., "[p1,p2,p3]")
     * @param paramTypes the expected parameter types
     * @return array of parameter objects
     * @throws ProtocolException if parsing fails
     */
    private Object[] parseParametersArray(String paramsArray, Class<?>[] paramTypes) throws ProtocolException {
        try {
            if (paramsArray == null || paramsArray.isEmpty()) {
                paramsArray = "[]";
            }

            // Validate array format
            if (!paramsArray.startsWith(ProtocolV2Constants.PARAMS_ARRAY_START) ||
                !paramsArray.endsWith(ProtocolV2Constants.PARAMS_ARRAY_END)) {
                throw new ProtocolException("Invalid parameter array format, expected [...]: " + paramsArray);
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
                    throw new ProtocolException(
                        "Parameter count mismatch: expected " + paramTypes.length + ", got 0"
                    );
                }
            }

            // Split by parameter separator (comma)
            String[] paramParts = arrayContent.split(ProtocolV2Constants.PARAM_SEPARATOR);

            if (paramParts.length != paramTypes.length) {
                throw new ProtocolException(
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
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            }
            throw new ProtocolException("Failed to parse parameters array: " + e.getMessage());
        }
    }

    /**
     * Parse a single parameter with intelligent type mapping.
     *
     * <p><b>Decoding Priority:</b></p>
     * <ol>
     *   <li><b>Null marker:</b> "~" → null (tilde, not in Base64 charset)</li>
     *   <li><b>Empty string:</b> "" → "" (consecutive commas in array)</li>
     *   <li><b>User-defined Mapper:</b> Use custom mapper if provided</li>
     *   <li><b>Common collection interfaces:</b> List, Map, Set, etc. → auto-deserialization via RawTypeMapper</li>
     *   <li><b>Auto Deserialization:</b> For Serializable types, use RawTypeMapper</li>
     *   <li><b>Built-in conversion:</b> For primitives, arrays, and other types</li>
     * </ol>
     *
     * @param paramStr the parameter string (base64-encoded or special marker)
     * @param paramType the expected parameter type
     * @return parsed parameter object
     * @throws ProtocolException if parsing fails
     */
    private Object parseParameter(String paramStr, Class<?> paramType) throws ProtocolException {
        try {
            if (paramStr == null) {
                throw new ProtocolException("Parameter cannot be null");
            }

            // Handle empty string (consecutive commas: [a,,b])
            if (paramStr.isEmpty()) {
                return "";
            }

            // Handle ~ marker for null
            if ("~".equals(paramStr)) {
                return null;
            }

            // Priority 1: Primitives, wrappers, String, primitive arrays, String[] (fast path).
            // Wire format is Base64(toString) or Base64("[...]"); decode once then convertToType.
            if (paramType == String.class || isWrapperType(paramType) || paramType.isPrimitive() ||
                (paramType.isArray() && isPrimitiveOrStringComponent(paramType.getComponentType()))) {
                String standardBase64 = convertUrlSafeToStandard(paramStr);
                String decoded = new String(Base64.getDecoder().decode(standardBase64));
                return convertToType(decoded, paramType);
            }

            // Priority 2: Object arrays (e.g. PersonDto[]) - serialized as Base64 by client, not "[...]" format.
            if (paramType.isArray()) {
                String standardBase64 = convertUrlSafeToStandard(paramStr);
                cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
                return rawMapper.stringToObject(standardBase64);
            }

            // Priority 3: User-defined Mapper
            if (mappers != null) {
                cn.huiwings.tcprest.mapper.Mapper mapper = mappers.get(paramType.getCanonicalName());
                if (mapper != null) {
                    String standardBase64 = convertUrlSafeToStandard(paramStr);
                    if (mapper instanceof cn.huiwings.tcprest.mapper.RawTypeMapper) {
                        return mapper.stringToObject(standardBase64);
                    }
                    String decoded = new String(Base64.getDecoder().decode(standardBase64));
                    return mapper.stringToObject(decoded);
                }
            }

            // Priority 4: Common collection interfaces (List, Map, Set, Deque, etc.)
            if (isCommonCollectionInterface(paramType)) {
                String standardBase64 = convertUrlSafeToStandard(paramStr);
                cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
                return rawMapper.stringToObject(standardBase64);
            }

            // Priority 5: Auto Deserialization for Serializable types (non-array)
            if (java.io.Serializable.class.isAssignableFrom(paramType) &&
                paramType != String.class &&
                !paramType.isArray() &&
                !isWrapperType(paramType)) {
                String standardBase64 = convertUrlSafeToStandard(paramStr);
                cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
                return rawMapper.stringToObject(standardBase64);
            }

            // Priority 6: Fallback — decode and convert (e.g. unknown types → string)
            String standardBase64 = convertUrlSafeToStandard(paramStr);
            String decoded = new String(Base64.getDecoder().decode(standardBase64));
            return convertToType(decoded, paramType);
        } catch (Exception e) {
            if (e instanceof ProtocolException) {
                throw (ProtocolException) e;
            }
            throw new ProtocolException("Failed to parse parameter: " + e.getMessage());
        }
    }

    /**
     * Convert URL-safe Base64 to standard Base64.
     *
     * <p>Converts '-' → '+', '_' → '/', and adds padding '='</p>
     *
     * @param urlSafeBase64 URL-safe Base64 string
     * @return standard Base64 string
     */
    private String convertUrlSafeToStandard(String urlSafeBase64) {
        // Restore standard Base64 characters
        String standard = urlSafeBase64.replace('-', '+').replace('_', '/');

        // Add padding if needed
        int padding = (4 - standard.length() % 4) % 4;
        for (int i = 0; i < padding; i++) {
            standard += "=";
        }

        return standard;
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

    private boolean isPrimitiveOrStringComponent(Class<?> componentType) {
        return componentType == int.class || componentType == long.class
            || componentType == double.class || componentType == float.class
            || componentType == byte.class || componentType == short.class
            || componentType == boolean.class || componentType == char.class
            || componentType == String.class;
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
     * Check if a request is a V2 request.
     *
     * @param request the request string
     * @return true if V2 request
     */
    public static boolean isV2Request(String request) {
        return request != null && request.startsWith(ProtocolV2Constants.PREFIX);
    }
}
