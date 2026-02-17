package cn.huiwings.tcprest.converter.v2;

import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.StatusCode;
import cn.huiwings.tcprest.protocol.v2.TypeSignatureUtil;

import java.lang.reflect.Method;
import java.util.Base64;

/**
 * Protocol v2 converter (standalone, not implementing v1 Converter interface).
 *
 * <p>This converter adds method signatures to requests and status codes to responses,
 * enabling method overloading support and exception propagation.</p>
 *
 * <p><b>Request Format:</b></p>
 * <pre>
 * V2|0|ClassName/methodName(TYPE_SIGNATURE)(PARAMS)
 *
 * Example:
 * V2|0|Calculator/add(II)({{MQ==}}:::{{Mg==}})
 * </pre>
 *
 * <p><b>Response Format:</b></p>
 * <pre>
 * V2|0|STATUS|BODY
 *
 * Examples:
 * V2|0|0|{{base64_result}}           # Success
 * V2|0|1|{{base64_error_message}}    # Business exception
 * V2|0|2|{{base64_error_message}}    # Server error
 * </pre>
 *
 * @since 1.1.0
 */
public class ProtocolV2Converter {

    private static final String COMPRESSION_DISABLED = "0";

    /**
     * Encode request with method signature support.
     *
     * <p>Format: V2|0|ClassName/methodName(TYPE_SIGNATURE)(PARAMS)</p>
     *
     * @param clazz the interface class
     * @param method the method to invoke
     * @param params the method parameters
     * @return encoded request string
     */
    public String encode(Class clazz, Method method, Object[] params) {
        StringBuilder sb = new StringBuilder();

        // Add protocol prefix: V2|0|
        sb.append(ProtocolV2Constants.PREFIX);
        sb.append(COMPRESSION_DISABLED);
        sb.append(ProtocolV2Constants.SEPARATOR);

        // Add class name
        sb.append(clazz.getName());
        sb.append(ProtocolV2Constants.CLASS_METHOD_SEPARATOR);

        // Add method name
        sb.append(method.getName());

        // Add method signature
        String signature = TypeSignatureUtil.getMethodSignature(method);
        sb.append(signature);

        // Add parameters
        sb.append(ProtocolV2Constants.PARAMS_START);
        if (params != null && params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                if (i > 0) {
                    sb.append(ProtocolV2Constants.PARAM_SEPARATOR);
                }
                sb.append(ProtocolV2Constants.PARAM_WRAPPER_START);
                sb.append(encodeParam(params[i]));
                sb.append(ProtocolV2Constants.PARAM_WRAPPER_END);
            }
        }
        sb.append(ProtocolV2Constants.PARAMS_END);

        return sb.toString();
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
     * Decode response with status code handling.
     *
     * <p>Format: V2|0|STATUS|BODY</p>
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

        // Parse response parts: V2|0|STATUS|BODY
        String[] parts = response.split("\\" + ProtocolV2Constants.SEPARATOR, 4);
        if (parts.length < ProtocolV2Constants.MIN_RESPONSE_PARTS) {
            throw new IllegalArgumentException("Invalid v2 response format: " + response);
        }

        String statusStr = parts[ProtocolV2Constants.RESPONSE_STATUS_INDEX];
        String body = parts[ProtocolV2Constants.RESPONSE_BODY_INDEX];

        StatusCode status = StatusCode.fromString(statusStr);

        // Handle different status codes
        switch (status) {
            case SUCCESS:
                return decodeSuccessBody(body, expectedType);

            case BUSINESS_EXCEPTION:
                throw decodeException(body, true);

            case SERVER_ERROR:
                throw decodeException(body, false);

            case PROTOCOL_ERROR:
                throw new RuntimeException("Protocol error: " + body);

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
     * Encode response with status code.
     *
     * <p>Format: V2|0|STATUS|BODY</p>
     *
     * @param result the result object
     * @param status the status code
     * @return encoded response string
     */
    public String encodeResponse(Object result, StatusCode status) {
        StringBuilder sb = new StringBuilder();

        // Add protocol prefix: V2|0|
        sb.append(ProtocolV2Constants.PREFIX);
        sb.append(COMPRESSION_DISABLED);
        sb.append(ProtocolV2Constants.SEPARATOR);

        // Add status code
        sb.append(status.getCode());
        sb.append(ProtocolV2Constants.SEPARATOR);

        // Add body
        sb.append(encodeBody(result));

        return sb.toString();
    }

    /**
     * Encode response body.
     *
     * @param obj the object to encode
     * @return encoded body string
     */
    private String encodeBody(Object obj) {
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
     * Encode exception response.
     *
     * @param exception the exception
     * @param status the status code (BUSINESS_EXCEPTION or SERVER_ERROR)
     * @return encoded exception response
     */
    public String encodeException(Throwable exception, StatusCode status) {
        StringBuilder sb = new StringBuilder();

        // Add protocol prefix: V2|0|
        sb.append(ProtocolV2Constants.PREFIX);
        sb.append(COMPRESSION_DISABLED);
        sb.append(ProtocolV2Constants.SEPARATOR);

        // Add status code
        sb.append(status.getCode());
        sb.append(ProtocolV2Constants.SEPARATOR);

        // Add exception details
        String exceptionStr = exception.getClass().getSimpleName() + ": " +
                             (exception.getMessage() != null ? exception.getMessage() : "");
        String base64 = Base64.getEncoder().encodeToString(exceptionStr.getBytes());
        sb.append(ProtocolV2Constants.PARAM_WRAPPER_START);
        sb.append(base64);
        sb.append(ProtocolV2Constants.PARAM_WRAPPER_END);

        return sb.toString();
    }
}
