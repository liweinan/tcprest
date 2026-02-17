package cn.huiwings.tcprest.extractor.v2;

import cn.huiwings.tcprest.protocol.v2.ProtocolV2Constants;
import cn.huiwings.tcprest.protocol.v2.TypeSignatureUtil;
import cn.huiwings.tcprest.server.Context;

import java.lang.reflect.Method;
import java.util.Base64;

/**
 * Protocol v2 extractor for parsing v2 format requests.
 *
 * <p>This extractor parses the enhanced v2 request format that includes
 * method signatures, enabling precise method selection for overloaded methods.</p>
 *
 * <p><b>Request Format:</b></p>
 * <pre>
 * V2|0|ClassName/methodName(TYPE_SIGNATURE)(PARAMS)
 *
 * Examples:
 * V2|0|Calculator/add(II)({{MQ==}}:::{{Mg==}})
 * V2|0|Service/process(Ljava/lang/String;Z)({{aGVsbG8=}}:::{{dHJ1ZQ==}})
 * </pre>
 *
 * @since 1.1.0
 */
public class ProtocolV2Extractor {

    /**
     * Extract context from v2 request.
     *
     * <p>Parses the request to extract:</p>
     * <ul>
     *   <li>Class name</li>
     *   <li>Method name</li>
     *   <li>Method signature (type signature)</li>
     *   <li>Parameters</li>
     * </ul>
     *
     * <p>Uses the type signature to find the exact method, solving the
     * overloading problem.</p>
     *
     * @param request the v2 request string
     * @return Context object with extracted information
     * @throws Exception if parsing fails or method cannot be found
     */
    public Context extract(String request) throws Exception {
        if (request == null || request.isEmpty()) {
            throw new IllegalArgumentException("Request cannot be null or empty");
        }

        if (!request.startsWith(ProtocolV2Constants.PREFIX)) {
            throw new IllegalArgumentException("Not a v2 request: " + request);
        }

        // Parse request parts: V2|0|ClassName/methodName(SIGNATURE)(PARAMS)
        String[] parts = request.split("\\" + ProtocolV2Constants.SEPARATOR, 3);
        if (parts.length < ProtocolV2Constants.MIN_REQUEST_PARTS) {
            throw new IllegalArgumentException("Invalid v2 request format: " + request);
        }

        String methodCall = parts[ProtocolV2Constants.REQUEST_METHOD_CALL_INDEX];

        // Parse class name and method signature
        int slashIndex = methodCall.indexOf(ProtocolV2Constants.CLASS_METHOD_SEPARATOR);
        if (slashIndex == -1) {
            throw new IllegalArgumentException("Missing class/method separator: " + methodCall);
        }

        String className = methodCall.substring(0, slashIndex);
        String methodPart = methodCall.substring(slashIndex + 1);

        // Parse method name and signature
        // Format: methodName(SIGNATURE)(PARAMS)
        int firstParenIndex = methodPart.indexOf('(');
        if (firstParenIndex == -1) {
            throw new IllegalArgumentException("Missing method signature: " + methodPart);
        }

        String methodName = methodPart.substring(0, firstParenIndex);

        // Find the signature (between first '(' and first ')')
        int signatureEnd = methodPart.indexOf(')', firstParenIndex);
        if (signatureEnd == -1) {
            throw new IllegalArgumentException("Malformed method signature: " + methodPart);
        }

        String signature = methodPart.substring(firstParenIndex, signatureEnd + 1);

        // Find parameters (between second '(' and second ')')
        int paramsStart = methodPart.indexOf('(', signatureEnd);
        int paramsEnd = methodPart.lastIndexOf(')');

        if (paramsStart == -1 || paramsEnd == -1 || paramsEnd <= paramsStart) {
            throw new IllegalArgumentException("Malformed parameters: " + methodPart);
        }

        String paramsStr = methodPart.substring(paramsStart + 1, paramsEnd);

        // Load class
        Class<?> clazz = Class.forName(className);

        // Find method by signature
        Method method = TypeSignatureUtil.findMethodBySignature(clazz, methodName, signature);

        // Parse parameters
        Object[] params = parseParameters(paramsStr, method.getParameterTypes());

        // Create and return context
        Context context = new Context();
        context.setTargetClass(clazz);
        context.setTargetMethod(method);
        context.setParams(params);

        return context;
    }

    /**
     * Parse parameter string into object array.
     *
     * <p>Format: {{base64_1}}:::{{base64_2}}:::{{base64_3}}</p>
     *
     * @param paramsStr the parameters string
     * @param paramTypes the expected parameter types
     * @return array of parameter objects
     * @throws Exception if parsing fails
     */
    private Object[] parseParameters(String paramsStr, Class<?>[] paramTypes) throws Exception {
        if (paramsStr == null || paramsStr.isEmpty()) {
            return new Object[0];
        }

        // Split by parameter separator
        String[] paramParts = paramsStr.split(ProtocolV2Constants.PARAM_SEPARATOR);

        if (paramParts.length != paramTypes.length) {
            throw new IllegalArgumentException(
                "Parameter count mismatch: expected " + paramTypes.length +
                ", got " + paramParts.length
            );
        }

        Object[] params = new Object[paramTypes.length];

        for (int i = 0; i < paramParts.length; i++) {
            params[i] = parseParameter(paramParts[i], paramTypes[i]);
        }

        return params;
    }

    /**
     * Parse a single parameter.
     *
     * <p>Format: {{base64_value}}</p>
     *
     * @param paramStr the parameter string
     * @param paramType the expected parameter type
     * @return parsed parameter object
     * @throws Exception if parsing fails
     */
    private Object parseParameter(String paramStr, Class<?> paramType) throws Exception {
        // Remove wrapper
        if (!paramStr.startsWith(ProtocolV2Constants.PARAM_WRAPPER_START) ||
            !paramStr.endsWith(ProtocolV2Constants.PARAM_WRAPPER_END)) {
            throw new IllegalArgumentException("Invalid parameter format: " + paramStr);
        }

        String base64 = paramStr.substring(
            ProtocolV2Constants.PARAM_WRAPPER_START.length(),
            paramStr.length() - ProtocolV2Constants.PARAM_WRAPPER_END.length()
        );

        // Handle NULL marker
        if ("NULL".equals(base64)) {
            return null;
        }

        // Handle empty base64 (empty string)
        if (base64.isEmpty()) {
            if (paramType == String.class) {
                return "";
            }
            // For non-String types, empty means null (for backward compat)
            return null;
        }

        // Decode from Base64
        String decoded = new String(Base64.getDecoder().decode(base64));

        // Convert to expected type
        return convertToType(decoded, paramType);
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
