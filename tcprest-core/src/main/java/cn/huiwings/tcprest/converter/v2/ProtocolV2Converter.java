package cn.huiwings.tcprest.converter.v2;

import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.StatusCode;
import cn.huiwings.tcprest.protocol.v2.TypeSignatureUtil;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;

import java.lang.reflect.Method;
import java.util.Base64;

/**
 * Security-Enhanced Protocol v2 Converter.
 *
 * <p>This converter adds method signatures to requests and status codes to responses,
 * enabling method overloading support and exception propagation.</p>
 *
 * <p><b>Secure Request Format (2026-02-18):</b></p>
 * <pre>
 * V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|{{base64(PARAMS)}}|CHK:value
 *
 * Example:
 * V2|0|Q2FsY3VsYXRvci9hZGQoSUkp|e3tNUT09fX06Ojp7e01nPT19fQ|CHK:a1b2c3d4
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
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>All metadata (class/method/signature) Base64-encoded</li>
 *   <li>All parameters Base64-encoded</li>
 *   <li>Optional checksum verification (CRC32/HMAC)</li>
 *   <li>Optional class whitelist validation</li>
 * </ul>
 *
 * @since 1.1.0
 */
public class ProtocolV2Converter {

    private static final String COMPRESSION_DISABLED = "0";
    private SecurityConfig securityConfig;

    /**
     * Create converter with default security (no checksum, no whitelist).
     */
    public ProtocolV2Converter() {
        this.securityConfig = new SecurityConfig();
    }

    /**
     * Create converter with custom security configuration.
     *
     * @param securityConfig security configuration
     */
    public ProtocolV2Converter(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
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
     * Encode request with method signature support (secure format).
     *
     * <p>New secure format: V2|0|{{base64(ClassName/methodName(TYPE_SIGNATURE))}}|{{base64(PARAMS)}}|CHK:value</p>
     *
     * @param clazz the interface class
     * @param method the method to invoke
     * @param params the method parameters
     * @return encoded request string
     */
    public String encode(Class clazz, Method method, Object[] params) {
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

        // Step 4: Build parameters string
        StringBuilder paramsBuilder = new StringBuilder();
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    paramsBuilder.append(ProtocolV2Constants.PARAM_SEPARATOR);
                }
                paramsBuilder.append(ProtocolV2Constants.PARAM_WRAPPER_START);
                paramsBuilder.append(encodeParam(params[i]));
                paramsBuilder.append(ProtocolV2Constants.PARAM_WRAPPER_END);
            }
        }
        String paramsString = paramsBuilder.toString();

        // Step 5: Encode metadata and params using Base64
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String paramsBase64 = ProtocolSecurity.encodeComponent(paramsString);

        // Step 6: Build protocol message: V2|0|META|PARAMS
        String message = ProtocolV2Constants.PREFIX + COMPRESSION_DISABLED +
                        ProtocolV2Constants.SEPARATOR + metaBase64 +
                        ProtocolV2Constants.SEPARATOR + paramsBase64;

        // Step 7: Add checksum if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += ProtocolV2Constants.SEPARATOR + checksum;
        }

        return message;
    }

    /**
     * Encode a single parameter to Base64.
     *
     * @param param the parameter value
     * @return Base64-encoded parameter
     */
    private String encodeParam(Object param) {
        if (param == null) {
            return "NULL"; // Special marker for null
        }

        // Convert to string representation
        String paramStr;
        if (param.getClass().isArray()) {
            // Handle arrays properly
            paramStr = arrayToString(param);
        } else {
            paramStr = param.toString();
        }
        return Base64.getEncoder().encodeToString(paramStr.getBytes());
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
     * Decode response with status code handling (secure format).
     *
     * <p>New secure format: V2|0|STATUS|{{base64(BODY)}}|CHK:value</p>
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
     * Decode successful response body.
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

        // Decode from Base64
        String decoded;
        if (body.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) &&
            body.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
            String base64 = body.substring(2, body.length() - 2);
            decoded = new String(Base64.getDecoder().decode(base64));
        } else {
            decoded = body;
        }

        // Convert to expected type
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
     * Encode response with status code (secure format).
     *
     * <p>New secure format: V2|0|STATUS|{{base64(BODY)}}|CHK:value</p>
     *
     * @param result the result object
     * @param status the status code
     * @return encoded response string
     */
    public String encodeResponse(Object result, StatusCode status) {
        // Step 1: Encode body
        String bodyString = encodeBodyToString(result);

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
     * Encode body to string (already includes {{base64}} format).
     *
     * @param obj the object to encode
     * @return encoded body string
     */
    private String encodeBodyToString(Object obj) {
        if (obj == null) {
            return "null";
        }

        if (obj instanceof NullObj) {
            return "NullObj";
        }

        String value = obj.toString();
        String base64 = Base64.getEncoder().encodeToString(value.getBytes());
        return ProtocolV2Constants.PARAM_WRAPPER_START + base64 + ProtocolV2Constants.PARAM_WRAPPER_END;
    }

    /**
     * Encode exception response (secure format).
     *
     * <p>New secure format: V2|0|STATUS|{{base64(exception)}}|CHK:value</p>
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
}
