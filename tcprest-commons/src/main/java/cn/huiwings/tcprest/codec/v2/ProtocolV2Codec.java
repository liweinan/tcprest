package cn.huiwings.tcprest.codec.v2;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.StatusCode;
import cn.huiwings.tcprest.protocol.v2.TypeSignatureUtil;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Map;

/**
 * Security-Enhanced Protocol V2 Codec.
 *
 * <p>This codec implements the TcpRest Protocol V2 with method signature support,
 * enabling method overloading and intelligent type mapping.</p>
 *
 * <p><b>Secure Request Format:</b></p>
 * <pre>
 * V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,param3]|CHK:value
 *
 * Example:
 * V2|0|Q2FsY3VsYXRvci9hZGQoSUkp|[MQ==,Mg==]|CHK:a1b2c3d4
 * </pre>
 *
 * <p><b>Secure Response Format:</b></p>
 * <pre>
 * V2|0|STATUS|{{base64(BODY)}}|CHK:value
 *
 * Examples:
 * V2|0|0|{{base64_result}}|CHK:value           # Success
 * V2|0|1|{{base64_error_message}}|CHK:value    # Business exception
 * V2|0|2|{{base64_error_message}}|CHK:value    # Server error
 * </pre>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li><b>Method Overloading:</b> Type signatures enable exact method matching</li>
 *   <li><b>Intelligent Mapper:</b> User-defined → Auto-serialization → Built-in</li>
 *   <li><b>Security:</b> Base64 encoding, checksum verification, class whitelist</li>
 *   <li><b>Status Codes:</b> Exception propagation with status codes</li>
 * </ul>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>All metadata (class/method/signature) Base64-encoded</li>
 *   <li>All parameters Base64-encoded</li>
 *   <li>Optional checksum verification (CRC32/HMAC-SHA256)</li>
 *   <li>Optional class whitelist validation</li>
 *   <li>Class/method name validation (prevents injection)</li>
 * </ul>
 *
 * @author Weinan Li
 * @since 1.1.0
 */
public class ProtocolV2Codec implements ProtocolCodec {

    private static final String COMPRESSION_DISABLED = "0";
    private SecurityConfig securityConfig;
    private Map<String, Mapper> mappers;

    /**
     * Create codec with default security (no checksum, no whitelist).
     */
    public ProtocolV2Codec() {
        this(null, null);
    }

    /**
     * Create codec with mappers support.
     *
     * @param mappers mapper registry (optional)
     */
    public ProtocolV2Codec(Map<String, Mapper> mappers) {
        this(null, mappers);
    }

    /**
     * Create codec with custom security configuration.
     *
     * @param securityConfig security configuration
     */
    public ProtocolV2Codec(SecurityConfig securityConfig) {
        this(securityConfig, null);
    }

    /**
     * Create codec with custom security configuration and mappers.
     *
     * @param securityConfig security configuration
     * @param mappers mapper registry (optional)
     */
    public ProtocolV2Codec(SecurityConfig securityConfig, Map<String, Mapper> mappers) {
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
     * Encode request with method signature support (V2 format).
     *
     * <p><b>Format:</b> V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|[param1,param2,param3]|CHK:value</p>
     *
     * <p>Parameters are encoded as a JSON-style array:</p>
     * <ul>
     *   <li>Each parameter is Base64-encoded individually</li>
     *   <li>Parameters are separated by commas</li>
     *   <li>The entire parameter list is wrapped in square brackets</li>
     * </ul>
     *
     * <p><b>Intelligent Mapper Support:</b></p>
     * <ol>
     *   <li><b>User-defined Mapper:</b> Custom mapper for specific types</li>
     *   <li><b>Auto Serialization:</b> Serializable objects use RawTypeMapper</li>
     *   <li><b>Built-in conversion:</b> Primitives, arrays, toString() for others</li>
     * </ol>
     *
     * @param clazz the interface class
     * @param method the method to invoke
     * @param params the method parameters
     * @param mappers mapper registry (optional - for custom type mapping)
     * @return encoded request string
     * @throws MapperNotFoundException if mapper is required but not found
     */
    @Override
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException {
        // Step 1: Build metadata (ClassName/methodName(TYPE_SIGNATURE))
        String className = clazz.getName();
        String methodName = method.getName();
        String signature = TypeSignatureUtil.getMethodSignature(method);
        String meta = className + "/" + methodName + signature;

        // Step 2: Validate class name and method name
        if (!ProtocolSecurity.isValidClassName(className)) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Invalid class name format: " + className
            );
        }
        if (!ProtocolSecurity.isValidMethodName(methodName)) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Invalid method name format: " + methodName
            );
        }

        // Step 3: Check class whitelist if enabled
        if (!securityConfig.isClassAllowed(className)) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Class not in whitelist: " + className
            );
        }

        // Step 4: Build parameters array (JSON-style format)
        StringBuilder paramsBuilder = new StringBuilder();
        paramsBuilder.append(ProtocolV2Constants.PARAMS_ARRAY_START);
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    paramsBuilder.append(ProtocolV2Constants.PARAM_SEPARATOR);
                }
                paramsBuilder.append(encodeParam(params[i], mappers));
            }
        }
        paramsBuilder.append(ProtocolV2Constants.PARAMS_ARRAY_END);
        String paramsArray = paramsBuilder.toString();

        // Step 5: Encode metadata using Base64 (wrapped with {{}})
        String metaBase64 = ProtocolV2Constants.PARAM_WRAPPER_START +
                           ProtocolSecurity.encodeComponent(meta) +
                           ProtocolV2Constants.PARAM_WRAPPER_END;

        // Step 6: Build protocol message: V2|0|{{META}}|[PARAMS]
        String message = ProtocolV2Constants.PREFIX + COMPRESSION_DISABLED +
                        ProtocolV2Constants.SEPARATOR + metaBase64 +
                        ProtocolV2Constants.SEPARATOR + paramsArray;

        // Step 7: Add checksum if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }

        return message;
    }

    /**
     * Encode a single parameter to Base64 with intelligent type mapping.
     *
     * <p><b>Encoding Priority:</b></p>
     * <ol>
     *   <li><b>NULL marker:</b> null → "~"</li>
     *   <li><b>User-defined Mapper:</b> Use custom mapper if provided</li>
     *   <li><b>Auto Serialization:</b> For Serializable objects, use RawTypeMapper</li>
     *   <li><b>Arrays:</b> Use Arrays.toString() format</li>
     *   <li><b>Primitives/Strings:</b> Use toString() then Base64</li>
     * </ol>
     *
     * @param param the parameter value
     * @param mappers optional user-defined mappers
     * @return Base64-encoded parameter string (or special markers)
     */
    private String encodeParam(Object param, Map<String, Mapper> mappers) {
        if (param == null) {
            return "~"; // Tilde marker for null (not in Base64 charset)
        }

        String paramStr;

        // Priority 1: User-defined Mapper
        if (mappers != null) {
            String mapperKey = param.getClass().getName();
            Mapper mapper = mappers.get(mapperKey);
            if (mapper != null) {
                paramStr = mapper.objectToString(param);
                if (paramStr == null) {
                    return "~";
                }
                if (paramStr.isEmpty()) {
                    return ""; // Empty string
                }
                return Base64.getEncoder().encodeToString(paramStr.getBytes());
            }
        }

        // Priority 2: Auto Serialization for Serializable objects (except String and primitives)
        if (param instanceof java.io.Serializable &&
            !(param instanceof String) &&
            !param.getClass().isArray() &&
            !isWrapperType(param.getClass())) {
            cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
            // RawTypeMapper already returns Base64-encoded string, no need to encode again
            return rawMapper.objectToString(param);
        }

        // Priority 3: Arrays
        if (param.getClass().isArray()) {
            paramStr = arrayToString(param);
        } else {
            // Priority 4: Primitives and other types
            paramStr = param.toString();
        }

        // Handle empty string
        if (paramStr.isEmpty()) {
            return ""; // Empty string
        }

        return Base64.getEncoder().encodeToString(paramStr.getBytes());
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
     * Convert array to string representation.
     *
     * @param array the array object
     * @return string representation
     */
    private String arrayToString(Object array) {
        Class<?> componentType = array.getClass().getComponentType();

        if (componentType == int.class) {
            return java.util.Arrays.toString((int[]) array);
        } else if (componentType == double.class) {
            return java.util.Arrays.toString((double[]) array);
        } else if (componentType == long.class) {
            return java.util.Arrays.toString((long[]) array);
        } else if (componentType == boolean.class) {
            return java.util.Arrays.toString((boolean[]) array);
        } else if (componentType == byte.class) {
            return java.util.Arrays.toString((byte[]) array);
        } else if (componentType == short.class) {
            return java.util.Arrays.toString((short[]) array);
        } else if (componentType == float.class) {
            return java.util.Arrays.toString((float[]) array);
        } else if (componentType == char.class) {
            return java.util.Arrays.toString((char[]) array);
        } else {
            // Object arrays
            return java.util.Arrays.toString((Object[]) array);
        }
    }

    /**
     * Decode response with status code handling (V2 format).
     *
     * <p>Format: V2|0|STATUS|{{base64(BODY)}}|CHK:value</p>
     *
     * @param response the response string
     * @param expectedType the expected return type
     * @return decoded result
     * @throws Exception if status indicates error or decoding fails
     */
    public Object decode(String response, Class expectedType) throws Exception {
        if (response == null || response.isEmpty()) {
            return null;
        }

        // Step 1: Split checksum if present
        String[] checksumParts = ProtocolSecurity.splitChecksum(response);
        String messageWithoutChecksum = checksumParts[0];
        String checksum = checksumParts[1];

        // Step 2: Verify checksum if present
        if (!checksum.isEmpty()) {
            if (!ProtocolSecurity.verifyChecksum(messageWithoutChecksum, checksum, securityConfig)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Response checksum verification failed"
                );
            }
        }

        // Step 3: Parse response parts: V2|0|STATUS|BODY
        String[] parts = messageWithoutChecksum.split("\\" + ProtocolV2Constants.SEPARATOR, 4);
        if (parts.length < ProtocolV2Constants.MIN_RESPONSE_PARTS) {
            throw new IllegalArgumentException("Invalid v2 response format: " + response);
        }

        String statusStr = parts[ProtocolV2Constants.RESPONSE_STATUS_INDEX];
        String bodyEncoded = parts[ProtocolV2Constants.RESPONSE_BODY_INDEX];

        StatusCode status = StatusCode.fromString(statusStr);

        // Handle different status codes
        switch (status) {
            case SUCCESS:
                return decodeSuccessBody(bodyEncoded, expectedType);

            case BUSINESS_EXCEPTION:
                throw decodeException(bodyEncoded, true);

            case SERVER_ERROR:
                throw decodeException(bodyEncoded, false);

            case PROTOCOL_ERROR:
                throw new RuntimeException("Protocol error: " + bodyEncoded);

            default:
                throw new IllegalStateException("Unknown status code: " + status);
        }
    }

    /**
     * Decode successful response body with intelligent type mapping.
     *
     * <p><b>Decoding Priority:</b></p>
     * <ol>
     *   <li><b>null/NullObj:</b> return null or NullObj</li>
     *   <li><b>User-defined Mapper:</b> Use custom mapper if provided</li>
     *   <li><b>Auto Deserialization:</b> For Serializable types, use RawTypeMapper</li>
     *   <li><b>Built-in conversion:</b> For primitives, arrays, and other types</li>
     * </ol>
     *
     * @param body the response body
     * @param expectedType the expected return type
     * @return decoded object
     */
    private Object decodeSuccessBody(String body, Class expectedType) {
        if (body == null || body.isEmpty() || "null".equals(body)) {
            return null;
        }

        // Handle NullObj marker
        if (body.contains("NullObj")) {
            return new NullObj();
        }

        // Extract Base64 content from {{...}}
        String base64Content;
        if (body.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) &&
            body.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
            base64Content = body.substring(2, body.length() - 2);
        } else {
            base64Content = body;
        }

        // Priority 1: User-defined Mapper
        if (mappers != null && expectedType != null) {
            Mapper mapper = mappers.get(expectedType.getName());
            if (mapper != null) {
                String decoded = new String(Base64.getDecoder().decode(base64Content));
                return mapper.stringToObject(decoded);
            }
        }

        // Priority 2: Auto Deserialization for Serializable types
        if (expectedType != null &&
            java.io.Serializable.class.isAssignableFrom(expectedType) &&
            expectedType != String.class &&
            !expectedType.isArray() &&
            !isWrapperType(expectedType)) {
            // RawTypeMapper expects direct Base64 string
            cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
            return rawMapper.stringToObject(base64Content);
        }

        // Priority 3: Decode from Base64
        String decoded = new String(Base64.getDecoder().decode(base64Content));

        // Priority 4: Convert to expected type
        return convertToType(decoded, expectedType);
    }

    /**
     * Decode exception from response body.
     *
     * @param body the exception body
     * @param isBusinessException true if business exception
     * @return decoded exception
     */
    private Exception decodeException(String body, boolean isBusinessException) {
        if (body == null || body.isEmpty()) {
            return isBusinessException ?
                    new RuntimeException("Unknown business exception") :
                    new RuntimeException("Unknown server error");
        }

        // Decode from Base64 if wrapped
        String decoded;
        if (body.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) &&
            body.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
            String base64 = body.substring(2, body.length() - 2);
            decoded = new String(Base64.getDecoder().decode(base64));
        } else {
            decoded = body;
        }

        // Create exception with message
        return new RuntimeException(decoded);
    }

    /**
     * Convert string value to expected type.
     *
     * @param value the string value
     * @param expectedType the expected type
     * @return converted object
     */
    private Object convertToType(String value, Class expectedType) {
        if (expectedType == null || expectedType == String.class) {
            return value;
        }

        // Handle arrays
        if (expectedType.isArray()) {
            return parseArray(value, expectedType.getComponentType());
        }

        if (expectedType == int.class || expectedType == Integer.class) {
            return Integer.parseInt(value);
        } else if (expectedType == double.class || expectedType == Double.class) {
            return Double.parseDouble(value);
        } else if (expectedType == boolean.class || expectedType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (expectedType == long.class || expectedType == Long.class) {
            return Long.parseLong(value);
        } else if (expectedType == float.class || expectedType == Float.class) {
            return Float.parseFloat(value);
        } else if (expectedType == byte.class || expectedType == Byte.class) {
            return Byte.parseByte(value);
        } else if (expectedType == short.class || expectedType == Short.class) {
            return Short.parseShort(value);
        } else if (expectedType == char.class || expectedType == Character.class) {
            return value.charAt(0);
        }

        // For other types, return string representation
        return value;
    }

    /**
     * Parse array from string representation like "[a, b, c]".
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
        } else if (componentType == byte.class) {
            byte[] array = new byte[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Byte.parseByte(parts[i].trim());
            }
            return array;
        } else if (componentType == short.class) {
            short[] array = new short[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Short.parseShort(parts[i].trim());
            }
            return array;
        } else if (componentType == float.class) {
            float[] array = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = Float.parseFloat(parts[i].trim());
            }
            return array;
        } else if (componentType == String.class) {
            String[] array = new String[parts.length];
            for (int i = 0; i < parts.length; i++) {
                array[i] = parts[i].trim();
            }
            return array;
        }

        // For other types, create an Object array
        Object[] array = (Object[]) java.lang.reflect.Array.newInstance(componentType, parts.length);
        for (int i = 0; i < parts.length; i++) {
            array[i] = parts[i].trim();
        }
        return array;
    }

    /**
     * Encode response with status code (V2 format).
     *
     * <p><b>Format:</b> V2|0|STATUS|{{base64(BODY)}}|CHK:value</p>
     *
     * @param result the result object
     * @param status the status code
     * @return encoded response string
     */
    public String encodeResponse(Object result, StatusCode status) {
        // Step 1: Encode body with {{}} wrapper
        String bodyString = encodeBodyToString(result);

        // Step 2: Build protocol message: V2|0|STATUS|{{BODY}}
        String message = ProtocolV2Constants.PREFIX + COMPRESSION_DISABLED +
                        ProtocolV2Constants.SEPARATOR + status.getCode() +
                        ProtocolV2Constants.SEPARATOR + bodyString;

        // Step 3: Add checksum if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }

        return message;
    }

    /**
     * Encode body to string with intelligent type mapping.
     *
     * <p><b>Encoding Priority:</b></p>
     * <ol>
     *   <li><b>null:</b> return "null"</li>
     *   <li><b>NullObj:</b> return "NullObj"</li>
     *   <li><b>User-defined Mapper:</b> Use custom mapper if provided</li>
     *   <li><b>Auto Serialization:</b> For Serializable objects, use RawTypeMapper</li>
     *   <li><b>Arrays:</b> Use Arrays.toString() format</li>
     *   <li><b>Others:</b> Use toString() then Base64</li>
     * </ol>
     *
     * @param obj the object to encode
     * @return encoded body string in format {{base64}}
     */
    private String encodeBodyToString(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof NullObj) {
            return "NullObj";
        }

        String value;

        // Priority 1: User-defined Mapper
        if (mappers != null) {
            Mapper mapper = mappers.get(obj.getClass().getName());
            if (mapper != null) {
                value = mapper.objectToString(obj);
                String base64 = Base64.getEncoder().encodeToString(value.getBytes());
                return ProtocolV2Constants.PARAM_WRAPPER_START + base64 + ProtocolV2Constants.PARAM_WRAPPER_END;
            }
        }

        // Priority 2: Auto Serialization for Serializable objects
        if (obj instanceof java.io.Serializable &&
            !(obj instanceof String) &&
            !obj.getClass().isArray() &&
            !isWrapperType(obj.getClass())) {
            cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
            String base64Serialized = rawMapper.objectToString(obj);
            // RawTypeMapper already returns Base64, wrap it with {{}}
            return ProtocolV2Constants.PARAM_WRAPPER_START + base64Serialized + ProtocolV2Constants.PARAM_WRAPPER_END;
        }

        // Priority 3: Arrays
        if (obj.getClass().isArray()) {
            value = arrayToString(obj);
        } else {
            // Priority 4: Primitives and others
            value = obj.toString();
        }

        String base64 = Base64.getEncoder().encodeToString(value.getBytes());
        return ProtocolV2Constants.PARAM_WRAPPER_START + base64 + ProtocolV2Constants.PARAM_WRAPPER_END;
    }

    /**
     * Encode exception response (V2 format).
     *
     * <p>Format: V2|0|STATUS|{{base64(exception)}}|CHK:value</p>
     *
     * @param exception the exception
     * @param status the status code (BUSINESS_EXCEPTION or SERVER_ERROR)
     * @return encoded exception response
     */
    public String encodeException(Throwable exception, StatusCode status) {
        // Step 1: Encode exception details
        String exceptionStr = exception.getClass().getSimpleName() + ": " +
                             (exception.getMessage() != null ? exception.getMessage() : "");
        String base64 = Base64.getEncoder().encodeToString(exceptionStr.getBytes());
        String bodyString = ProtocolV2Constants.PARAM_WRAPPER_START + base64 + ProtocolV2Constants.PARAM_WRAPPER_END;

        // Step 2: Build protocol message: V2|0|STATUS|BODY
        String message = ProtocolV2Constants.PREFIX + COMPRESSION_DISABLED +
                        ProtocolV2Constants.SEPARATOR + status.getCode() +
                        ProtocolV2Constants.SEPARATOR + bodyString;

        // Step 3: Add checksum if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }

        return message;
    }

    /**
     * Decode parameters from protocol v2 format.
     *
     * <p>Note: This method is provided for ProtocolCodec interface compatibility.
     * Protocol V2 uses its own parameter parsing in ProtocolV2Parser.</p>
     *
     * @param targetMethod target method
     * @param paramToken parameter token
     * @param mappers mapper registry (not used in Protocol V2)
     * @return decoded parameters
     * @throws MapperNotFoundException not thrown in V2
     */
    @Override
    public Object[] decode(Method targetMethod, String paramToken, Map<String, Mapper> mappers) throws MapperNotFoundException {
        // Protocol V2 handles parameter decoding in ProtocolV2Parser
        // This method is provided for interface compatibility
        throw new UnsupportedOperationException(
            "Protocol V2 uses ProtocolV2Parser for parameter decoding. " +
            "Use decode(String response, Class expectedType) for response decoding instead."
        );
    }

    /**
     * Encode a single parameter (V2 format).
     *
     * <p><b>Format:</b> base64_value (or special markers: ~, empty)</p>
     *
     * @param message the parameter value
     * @return encoded parameter (Base64 or special marker)
     */
    @Override
    public String encodeParam(String message) {
        if (message == null) {
            return "~";
        }
        if (message.isEmpty()) {
            return "";
        }
        return Base64.getEncoder().encodeToString(message.getBytes());
    }

    /**
     * Decode a single parameter (V2 format).
     *
     * <p><b>Format:</b> base64_value (or special markers: ~, empty)</p>
     *
     * @param message the encoded parameter (Base64 or special marker)
     * @return decoded parameter value
     */
    @Override
    public String decodeParam(String message) {
        if (message == null) {
            return null;
        }

        // Handle ~ marker (null value)
        if ("~".equals(message)) {
            return null;
        }

        // Handle empty string
        if (message.isEmpty()) {
            return "";
        }

        // Decode from Base64
        return new String(Base64.getDecoder().decode(message));
    }

    /**
     * Get mapper by class.
     *
     * <p><b>V2 Mapper Support:</b> Protocol V2 supports intelligent type mapping:</p>
     * <ol>
     *   <li>User-defined mappers (if provided)</li>
     *   <li>Auto serialization for Serializable objects (RawTypeMapper)</li>
     *   <li>Built-in conversion for primitives and arrays</li>
     * </ol>
     *
     * @param mappers mapper registry
     * @param targetClazz target class
     * @return mapper if found, null otherwise
     */
    @Override
    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) throws MapperNotFoundException {
        if (mappers == null || targetClazz == null) {
            return null;
        }
        return mappers.get(targetClazz.getName());
    }

    /**
     * Get mapper by class name.
     *
     * <p><b>V2 Mapper Support:</b> Protocol V2 supports intelligent type mapping.</p>
     *
     * @param mappers mapper registry
     * @param targetClazzName target class name
     * @return mapper if found, null otherwise
     */
    @Override
    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) throws MapperNotFoundException {
        if (mappers == null || targetClazzName == null) {
            return null;
        }
        return mappers.get(targetClazzName);
    }
}
