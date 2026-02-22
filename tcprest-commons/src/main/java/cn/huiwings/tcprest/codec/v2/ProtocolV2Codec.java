package cn.huiwings.tcprest.codec.v2;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import cn.huiwings.tcprest.mapper.Mapper;
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

    // Array safety limits (prevent DoS attacks)
    private static final int MAX_ARRAY_DEPTH = 10;    // Maximum nesting depth for arrays
    private static final int MAX_ARRAY_SIZE = 100000;  // Maximum array length

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
     */
    @Override
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) {
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

        // Step 7: Add CHK then SIG if enabled (order: content|CHK:value|SIG:value)
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }
        String sigSegment = ProtocolSecurity.calculateSignature(message, securityConfig);
        if (!sigSegment.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + sigSegment;
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
            // Use getCanonicalName() to match MapperHelper.DEFAULT_MAPPERS keys
            String mapperKey = param.getClass().getCanonicalName();
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
            // RawTypeMapper returns standard Base64 - convert to URL-safe Base64
            String standardBase64 = rawMapper.objectToString(param);
            if (standardBase64 == null) {
                return "~";
            }
            // Convert standard Base64 to URL-safe: + → -, / → _, remove =
            return standardBase64.replace('+', '-').replace('/', '_').replace("=", "");
        }

        // Priority 3: Arrays
        if (param.getClass().isArray()) {
            paramStr = arrayToString(param);
            // Object arrays: arrayToString already returns Base64 from RawTypeMapper - do not double-encode
            Class<?> componentType = param.getClass().getComponentType();
            boolean primitiveOrStringArray = componentType == int.class || componentType == long.class
                || componentType == double.class || componentType == float.class
                || componentType == byte.class || componentType == short.class
                || componentType == boolean.class || componentType == char.class
                || componentType == String.class;
            if (!primitiveOrStringArray) {
                if (paramStr == null || paramStr.isEmpty()) {
                    return paramStr == null ? "~" : "";
                }
                return paramStr.replace('+', '-').replace('/', '_').replace("=", "");
            }
        } else {
            // Priority 4: Primitives and other types
            paramStr = param.toString();
        }

        // Handle null or empty string
        if (paramStr == null || paramStr.isEmpty()) {
            return "";
        }

        return Base64.getEncoder().encodeToString(paramStr.getBytes());
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

    /**
     * Convert array to string representation with safety checks.
     *
     * <p><b>Supported array types:</b></p>
     * <ul>
     *   <li>Primitive arrays (int[], long[], etc.) - uses Arrays.toString()</li>
     *   <li>String[] - uses Arrays.toString()</li>
     *   <li>Object arrays (User[], Person[], etc.) - uses Java serialization via RawTypeMapper</li>
     *   <li>Nested arrays (int[][], User[][], etc.) - uses Java serialization</li>
     * </ul>
     *
     * <p><b>Safety limits:</b></p>
     * <ul>
     *   <li>Maximum array size: {@value MAX_ARRAY_SIZE} elements</li>
     *   <li>Maximum nesting depth: {@value MAX_ARRAY_DEPTH} levels (checked during decode)</li>
     * </ul>
     *
     * @param array the array object
     * @return string representation
     * @throws IllegalArgumentException if array exceeds size limit
     */
    private String arrayToString(Object array) {
        Class<?> componentType = array.getClass().getComponentType();
        int length = java.lang.reflect.Array.getLength(array);

        // Safety check: array size limit
        if (length > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                "Array too large: " + length + " elements (max: " + MAX_ARRAY_SIZE + ")"
            );
        }

        // Primitive arrays - use Arrays.toString() for human-readable format
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
        } else if (componentType == String.class) {
            // String arrays - use Arrays.toString() for human-readable format
            return java.util.Arrays.toString((String[]) array);
        } else {
            // Object arrays (including nested arrays) - use Java serialization
            // This supports User[], Person[], int[][], User[][], etc.
            cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
            return rawMapper.objectToString(array);
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

        // Step 1: Parse trailing CHK and SIG segments
        ProtocolSecurity.TrailingSegments segments = ProtocolSecurity.parseTrailingSegments(response);

        // Step 2: Verify CHK if present
        if (!segments.getChkSegment().isEmpty()) {
            if (!ProtocolSecurity.verifyChecksum(segments.getContent(), segments.getChkSegment(), securityConfig)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Response checksum verification failed"
                );
            }
        }
        if (securityConfig.isChecksumEnabled() && segments.getChkSegment().isEmpty()) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Server requires checksum but response did not provide one"
            );
        }

        // Step 3: Verify SIG if required
        ProtocolSecurity.verifySignatureSegment(segments.getSignedPayload(), segments.getSigSegment(), securityConfig);

        // Step 4: Parse response parts: V2|0|STATUS|BODY
        String[] parts = segments.getContent().split("\\" + ProtocolV2Constants.SEPARATOR, 4);
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
                throw decodeException(bodyEncoded, false);

            default:
                throw new IllegalStateException("Unknown status code: " + status);
        }
    }

    /**
     * Decode successful response body with intelligent type mapping.
     *
     * <p><b>Decoding Priority:</b></p>
     * <ol>
     *   <li><b>null:</b> return null</li>
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

        // Handle "null" string marker (V2 protocol uses string "null")
        if ("null".equals(body) || body.contains("null")) {
            return null;
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
            // Use getCanonicalName() to match MapperHelper.DEFAULT_MAPPERS keys
            Mapper mapper = mappers.get(expectedType.getCanonicalName());
            if (mapper != null) {
                // Convert URL-safe Base64 to standard first
                String standardBase64 = convertUrlSafeToStandard(base64Content);

                // RawTypeMapper expects Base64 string directly, other mappers expect decoded string
                if (mapper instanceof cn.huiwings.tcprest.mapper.RawTypeMapper) {
                    // RawTypeMapper handles Base64 decoding internally
                    return mapper.stringToObject(standardBase64);
                } else {
                    // Other mappers expect decoded string
                    String decoded = new String(Base64.getDecoder().decode(standardBase64));
                    return mapper.stringToObject(decoded);
                }
            }
        }

        // Priority 2: Auto Deserialization for Serializable types
        if (expectedType != null &&
            java.io.Serializable.class.isAssignableFrom(expectedType) &&
            expectedType != String.class &&
            !expectedType.isArray() &&
            !isWrapperType(expectedType)) {
            // Convert URL-safe Base64 back to standard Base64
            String standardBase64 = convertUrlSafeToStandard(base64Content);
            // RawTypeMapper expects standard Base64 string
            cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
            return rawMapper.stringToObject(standardBase64);
        }

        // Priority 3: Decode from URL-safe Base64
        String standardBase64 = convertUrlSafeToStandard(base64Content);
        String decoded = new String(Base64.getDecoder().decode(standardBase64));

        // Priority 4: Convert to expected type
        return convertToType(decoded, expectedType);
    }

    /**
     * Decode exception from response body.
     *
     * <p>Attempts to recreate the original exception type thrown by the server.
     * If the exception class exists on the client classpath and has a constructor
     * accepting a String message, it will be instantiated. Otherwise, falls back
     * to RuntimeException with the original exception info in the message.</p>
     *
     * @param body the exception body (format: "ClassName: message")
     * @param isBusinessException true if business exception
     * @return decoded exception (original type if possible, RuntimeException otherwise)
     */
    private Exception decodeException(String body, boolean isBusinessException) {
        if (body == null || body.isEmpty()) {
            return isBusinessException ?
                    new RuntimeException("Unknown business exception") :
                    new RuntimeException("Unknown server error");
        }

        // Decode from URL-safe Base64 if wrapped
        String decoded;
        if (body.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) &&
            body.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
            String base64 = body.substring(2, body.length() - 2);
            // Convert URL-safe Base64 to standard first
            String standardBase64 = convertUrlSafeToStandard(base64);
            decoded = new String(Base64.getDecoder().decode(standardBase64));
        } else {
            decoded = body;
        }

        // Try to recreate original exception type
        return recreateException(decoded, isBusinessException);
    }

    /**
     * Attempt to recreate the original exception from the encoded string.
     *
     * <p><b>Strategy:</b></p>
     * <ol>
     *   <li>Parse exception class name and message</li>
     *   <li>Try to load exception class via Class.forName()</li>
     *   <li>Try to instantiate with reflection (String constructor or default)</li>
     *   <li>If fails, use appropriate fallback:
     *     <ul>
     *       <li>Business exception → {@link cn.huiwings.tcprest.exception.RemoteBusinessException}</li>
     *       <li>Server error → {@link cn.huiwings.tcprest.exception.RemoteServerException}</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * <p>Format: "fully.qualified.ClassName: message"</p>
     *
     * @param exceptionString encoded exception string
     * @param isBusinessException true if business exception
     * @return original exception type if possible, semantic fallback otherwise
     */
    private Exception recreateException(String exceptionString, boolean isBusinessException) {
        // Step 1: Parse exception class name and message
        int colonIndex = exceptionString.indexOf(": ");
        if (colonIndex == -1) {
            // No colon separator - use entire string as message
            return createFallbackException("Unknown", exceptionString, isBusinessException);
        }

        String className = exceptionString.substring(0, colonIndex);
        String message = exceptionString.substring(colonIndex + 2);

        // Step 2: Try to load and instantiate the exception class
        try {
            Class<?> exceptionClass = Class.forName(className);

            // Verify it's actually an Exception
            if (!Exception.class.isAssignableFrom(exceptionClass)) {
                // Not an exception class - use fallback
                return createFallbackException(className, message, isBusinessException);
            }

            // Try to find constructor with String parameter
            try {
                java.lang.reflect.Constructor<?> constructor =
                    exceptionClass.getConstructor(String.class);
                return (Exception) constructor.newInstance(message);
            } catch (NoSuchMethodException e) {
                // No String constructor, try default constructor
                try {
                    return (Exception) exceptionClass.getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    // Can't instantiate - fall through to fallback
                }
            }
        } catch (ClassNotFoundException e) {
            // Exception class not available on client side - this is expected
            // Use semantic fallback based on exception category
        } catch (Exception e) {
            // Any other error during instantiation - use fallback
        }

        // Step 3: Fallback - use appropriate remote exception wrapper
        return createFallbackException(className, message, isBusinessException);
    }

    /**
     * Create fallback exception when original type cannot be reconstructed.
     *
     * <p>Uses semantic exception types to preserve error category information:</p>
     * <ul>
     *   <li><b>Business exceptions:</b> Wrapped in {@link cn.huiwings.tcprest.exception.RemoteBusinessException}
     *     <br>Example: Server threw OrderValidationException (client doesn't have it)
     *     <br>→ RemoteBusinessException("com.example.OrderValidationException", "Order amount exceeds limit")
     *     <br>Client can handle it as a business error and retry with corrected input
     *   </li>
     *   <li><b>Server errors:</b> Wrapped in {@link cn.huiwings.tcprest.exception.RemoteServerException}
     *     <br>Example: Server threw CustomDatabaseException (client doesn't have it)
     *     <br>→ RemoteServerException("com.example.CustomDatabaseException", "Connection pool exhausted")
     *     <br>Client knows it's a server-side problem, logs/alerts but doesn't retry
     *   </li>
     * </ul>
     *
     * @param className original exception class name
     * @param message exception message
     * @param isBusinessException whether this is a business exception
     * @return RemoteBusinessException or RemoteServerException
     */
    private Exception createFallbackException(String className, String message, boolean isBusinessException) {
        if (isBusinessException) {
            // Server threw a BusinessException subclass, but client doesn't have it
            // Wrap in RemoteBusinessException so client can still handle it as business error
            return new cn.huiwings.tcprest.exception.RemoteBusinessException(className, message);
        } else {
            // Server threw an unexpected exception (NullPointerException, CustomException, etc.)
            // but client doesn't have the class
            // Wrap in RemoteServerException so client knows it's a server-side error
            return new cn.huiwings.tcprest.exception.RemoteServerException(className, message);
        }
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

        if (expectedType == boolean.class || expectedType == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (expectedType == char.class || expectedType == Character.class) {
            return value.isEmpty() ? '\0' : value.charAt(0);
        }
        try {
            if (expectedType == int.class || expectedType == Integer.class) {
                return Integer.parseInt(value);
            } else if (expectedType == double.class || expectedType == Double.class) {
                return Double.parseDouble(value);
            } else if (expectedType == long.class || expectedType == Long.class) {
                return Long.parseLong(value);
            } else if (expectedType == float.class || expectedType == Float.class) {
                return Float.parseFloat(value);
            } else if (expectedType == byte.class || expectedType == Byte.class) {
                return Byte.parseByte(value);
            } else if (expectedType == short.class || expectedType == Short.class) {
                return Short.parseShort(value);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format for " + expectedType.getSimpleName() + ": '" + value + "'", e);
        }

        // For other types, return string representation
        return value;
    }

    /**
     * Parse array from string representation with safety checks.
     *
     * <p><b>Supported formats:</b></p>
     * <ul>
     *   <li>Primitive/String arrays: "[1, 2, 3]" or "[a, b, c]"</li>
     *   <li>Object arrays: Base64-encoded serialized data (via RawTypeMapper)</li>
     * </ul>
     *
     * <p><b>Safety limits:</b></p>
     * <ul>
     *   <li>Maximum array size: {@value MAX_ARRAY_SIZE} elements</li>
     *   <li>Maximum nesting depth: {@value MAX_ARRAY_DEPTH} levels</li>
     * </ul>
     *
     * @param value the string value
     * @param componentType the array component type
     * @return parsed array
     * @throws IllegalArgumentException if array format is invalid or exceeds limits
     */
    private Object parseArray(String value, Class<?> componentType) {
        return parseArray(value, componentType, 0);
    }

    /**
     * Parse array with depth tracking (internal method).
     *
     * @param value the string value
     * @param componentType the array component type
     * @param depth current nesting depth
     * @return parsed array
     */
    private Object parseArray(String value, Class<?> componentType, int depth) {
        // Safety check: nesting depth limit
        if (depth > MAX_ARRAY_DEPTH) {
            throw new IllegalArgumentException(
                "Array nesting too deep: " + depth + " levels (max: " + MAX_ARRAY_DEPTH + ")"
            );
        }

        // Check if this is a serialized Object array (Base64 data, not "[...]" format)
        // Object arrays are serialized by RawTypeMapper in arrayToString()
        if (componentType != int.class && componentType != long.class &&
            componentType != double.class && componentType != float.class &&
            componentType != byte.class && componentType != short.class &&
            componentType != boolean.class && componentType != char.class &&
            componentType != String.class) {

            // This is an Object array (User[], Person[], int[][], etc.)
            // It was serialized by RawTypeMapper, so deserialize it
            cn.huiwings.tcprest.mapper.RawTypeMapper rawMapper = new cn.huiwings.tcprest.mapper.RawTypeMapper();
            Object array = rawMapper.stringToObject(value);

            // Safety check: verify array size after deserialization
            if (array != null && array.getClass().isArray()) {
                int length = java.lang.reflect.Array.getLength(array);
                if (length > MAX_ARRAY_SIZE) {
                    throw new IllegalArgumentException(
                        "Array too large: " + length + " elements (max: " + MAX_ARRAY_SIZE + ")"
                    );
                }
            }

            return array;
        }

        // Parse primitive or String arrays from "[...]" format
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

        // Safety check: array size limit
        if (parts.length > MAX_ARRAY_SIZE) {
            throw new IllegalArgumentException(
                "Array too large: " + parts.length + " elements (max: " + MAX_ARRAY_SIZE + ")"
            );
        }

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

        // Should not reach here given the initial check, but as fallback
        throw new IllegalArgumentException("Unsupported array component type: " + componentType);
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

        // Step 3: Add CHK then SIG if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }
        String sigSegment = ProtocolSecurity.calculateSignature(message, securityConfig);
        if (!sigSegment.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + sigSegment;
        }

        return message;
    }

    /**
     * Encode body to string with intelligent type mapping.
     *
     * <p><b>Encoding Priority:</b></p>
     * <ol>
     *   <li><b>null:</b> return "null"</li>
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

        String value;

        // Priority 1: User-defined Mapper
        if (mappers != null) {
            // Use getCanonicalName() to match MapperHelper.DEFAULT_MAPPERS keys
            Mapper mapper = mappers.get(obj.getClass().getCanonicalName());
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
            // Convert standard Base64 to URL-safe Base64
            String urlSafeBase64 = base64Serialized.replace('+', '-').replace('/', '_').replace("=", "");
            // Wrap with {{}}
            return ProtocolV2Constants.PARAM_WRAPPER_START + urlSafeBase64 + ProtocolV2Constants.PARAM_WRAPPER_END;
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
        // Step 1: Encode exception details with full class name for client-side reconstruction
        // Format: "FullyQualifiedClassName: message"
        String exceptionStr = exception.getClass().getName() + ": " +
                             (exception.getMessage() != null ? exception.getMessage() : "");
        String base64 = Base64.getEncoder().encodeToString(exceptionStr.getBytes());
        String bodyString = ProtocolV2Constants.PARAM_WRAPPER_START + base64 + ProtocolV2Constants.PARAM_WRAPPER_END;

        // Step 2: Build protocol message: V2|0|STATUS|BODY
        String message = ProtocolV2Constants.PREFIX + COMPRESSION_DISABLED +
                        ProtocolV2Constants.SEPARATOR + status.getCode() +
                        ProtocolV2Constants.SEPARATOR + bodyString;

        // Step 3: Add CHK then SIG if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }
        String sigSegment = ProtocolSecurity.calculateSignature(message, securityConfig);
        if (!sigSegment.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + sigSegment;
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
     */
    @Override
    public Object[] decode(Method targetMethod, String paramToken, Map<String, Mapper> mappers) {
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
    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) {
        if (mappers == null || targetClazz == null) {
            return null;
        }
        // Use getCanonicalName() to match MapperHelper.DEFAULT_MAPPERS keys
        return mappers.get(targetClazz.getCanonicalName());
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
    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) {
        if (mappers == null || targetClazzName == null) {
            return null;
        }
        return mappers.get(targetClazzName);
    }
}
