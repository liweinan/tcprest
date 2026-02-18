package cn.huiwings.tcprest.codec;

import cn.huiwings.tcprest.commons.Base64;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.mapper.RawTypeMapper;
import cn.huiwings.tcprest.protocol.NullObj;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Security-Enhanced TcpRest Protocol Converter (V1).
 *
 * <p><b>New Protocol Format (2026-02-18):</b></p>
 * <pre>
 * Request:  0|COMP|{{base64(ClassName/methodName)}}|{{base64(params)}}|CHK:value
 * Response: 0|{{base64(result)}}|CHK:value
 * </pre>
 *
 * <p><b>Security Enhancements:</b></p>
 * <ul>
 *   <li>All variable content (class/method names, parameters) are Base64-encoded</li>
 *   <li>Prevents injection attacks (path traversal, delimiter injection)</li>
 *   <li>Optional integrity verification (CRC32/HMAC-SHA256)</li>
 *   <li>Optional class name whitelist validation</li>
 * </ul>
 *
 * <p><b>Breaking Change:</b> This version is NOT backward compatible with old protocol format.</p>
 *
 * @author Weinan Li
 * @date 07 31 2012
 * @deprecated Use Protocol V2 ({@link cn.huiwings.tcprest.converter.v2.ProtocolV2Converter}) instead.
 *             V1 is maintained for backward compatibility only. V2 provides method overloading,
 *             better exception handling, and intelligent mapper support with auto-serialization.
 */
@Deprecated
public class DefaultProtocolCodec implements ProtocolCodec {
    private Logger logger = LoggerFactory.getDefaultLogger();
    private SecurityConfig securityConfig;

    /**
     * Creates a converter with default security (no checksum, no whitelist).
     */
    public DefaultProtocolCodec() {
        this.securityConfig = new SecurityConfig();
    }

    /**
     * Creates a converter with custom security configuration.
     *
     * @param securityConfig security configuration
     */
    public DefaultProtocolCodec(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
    }

    /**
     * Sets security configuration.
     *
     * @param securityConfig security configuration
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
    }

    /**
     * Gets security configuration.
     *
     * @return security configuration
     */
    public SecurityConfig getSecurityConfig() {
        return securityConfig;
    }

    /**
     * Encodes a method call into secure protocol format.
     *
     * <p>Format: {@code 0|META|PARAMS|CHK:value}</p>
     * <ul>
     *   <li>META = base64(ClassName/methodName)</li>
     *   <li>PARAMS = base64(param1:::param2:::...)</li>
     *   <li>CHK:value = optional checksum</li>
     * </ul>
     *
     * @param clazz   calling class
     * @param method  calling method
     * @param params  parameters
     * @param mappers mappers
     * @return encoded protocol string
     */
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) throws MapperNotFoundException {
        // Step 1: Build metadata (ClassName/methodName)
        String className = clazz.getCanonicalName();
        String methodName = method.getName();
        String meta = className + "/" + methodName;

        logger.debug("***DefaultProtocolCodec - encoding meta: " + meta);

        // Step 2: Build parameters
        StringBuilder paramTokenBuffer = new StringBuilder();
        if (params != null && params.length > 0) {
            for (Object param : params) {
                if (param == null) {
                    param = new NullObj();
                }
                logger.log("***DefaultProtocolCodec - encode for class: " + param.getClass());

                Mapper mapper = getMapper(mappers, param.getClass());
                String paramStr = mapper.objectToString(param);

                // Encode individual parameter
                paramTokenBuffer.append(encodeParam(paramStr)).append(TcpRestProtocol.PARAM_SEPARATOR);
            }

            // Remove trailing separator
            paramTokenBuffer.setLength(paramTokenBuffer.length() - TcpRestProtocol.PARAM_SEPARATOR.length());
        }

        String paramsEncoded = paramTokenBuffer.toString();
        logger.debug("***DefaultProtocolCodec - encoded params: " + paramsEncoded);

        // Step 3: Encode metadata and params using Base64
        String metaBase64 = ProtocolSecurity.encodeComponent(meta);
        String paramsBase64 = ProtocolSecurity.encodeComponent(paramsEncoded);

        // Step 4: Build protocol message
        String message = "0" + TcpRestProtocol.COMPONENT_SEPARATOR +
                        metaBase64 + TcpRestProtocol.COMPONENT_SEPARATOR +
                        paramsBase64;

        // Step 5: Add checksum if enabled
        String checksum = ProtocolSecurity.calculateChecksum(message, securityConfig);
        if (!checksum.isEmpty()) {
            message += TcpRestProtocol.COMPONENT_SEPARATOR + checksum;
        }

        logger.debug("***DefaultProtocolCodec - final message: " + message);
        return message;
    }

    /**
     * Decodes parameters from secure protocol format.
     *
     * @param targetMethod target method
     * @param paramsBase64 base64-encoded params
     * @param mappers      mappers
     * @return decoded parameters
     */
    public Object[] decode(Method targetMethod, String paramsBase64, Map<String, Mapper> mappers) throws MapperNotFoundException {
        if (paramsBase64 == null || paramsBase64.trim().isEmpty()) {
            return null;
        }

        // Decode params block
        String paramsDecoded = ProtocolSecurity.decodeComponent(paramsBase64.trim());

        if (paramsDecoded.isEmpty()) {
            return null;
        }

        // Split parameters
        String[] rawParams = paramsDecoded.split(TcpRestProtocol.PARAM_SEPARATOR);
        List<Object> paramsHolder = new ArrayList<Object>();

        int i = 0;
        for (String rawParam : rawParams) {
            String thisParam = decodeParam(rawParam);
            logger.debug("param types: " + targetMethod.getParameterTypes()[i]);
            Mapper mapper = getMapper(mappers, targetMethod.getParameterTypes()[i]);

            Object param = mapper.stringToObject(thisParam);

            if (thisParam.equals(TcpRestProtocol.NULL)) {
                param = null;
            }

            paramsHolder.add(param);
            i++;
        }

        return paramsHolder.toArray();
    }

    /**
     * Encodes a single parameter.
     *
     * <p>Format: {@code {{base64}}}
     *
     * @param message parameter value
     * @return encoded parameter
     */
    public String encodeParam(String message) {
        return "{{" + Base64.encode(message.getBytes()) + "}}";
    }

    /**
     * Decodes a single parameter.
     *
     * @param message encoded parameter in format {{base64}}
     * @return decoded string, or empty string if invalid
     */
    public String decodeParam(String message) {
        logger.debug("decodeParam: " + message);

        // Handle empty or null messages
        if (message == null || message.isEmpty()) {
            return "";
        }

        // Check if message contains the expected markers
        int startIndex = message.indexOf("{{");
        int endIndex = message.lastIndexOf("}}");

        // If markers not found or in wrong order, return empty string
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            logger.warn("Invalid message format, expected {{...}}, got: " + message);
            return "";
        }

        // Extract and decode the content between {{ and }}
        String encoded = message.substring(startIndex + 2, endIndex);
        if (encoded.isEmpty()) {
            return "";
        }

        return new String(Base64.decode(encoded));
    }

    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) throws MapperNotFoundException {
        Mapper mapper = mappers.get(targetClazz.getCanonicalName());

        if (mapper == null) {
            // Try to find if the target param is serializable, if so we could use RawTypeMapper
            for (Class clazz : targetClazz.getInterfaces()) {
                if (clazz.equals(java.io.Serializable.class) || clazz.isArray()) {
                    mapper = new RawTypeMapper();
                    break;
                }
            }

            if (mapper == null)
                throw new MapperNotFoundException("***DefaultProtocolCodec - cannot find mapper for: " + targetClazz.getCanonicalName());
        }

        logger.debug("found mapper: " + mapper.getClass().getCanonicalName() + " for: " + targetClazz.getCanonicalName());
        return mapper;
    }

    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) throws MapperNotFoundException {
        try {
            Mapper mapper;
            // check for array type
            if (targetClazzName.endsWith("[]")) {
                mapper = new RawTypeMapper();
                return mapper;
            }
            mapper = mappers.get(targetClazzName);
            if (mapper != null) {
                logger.debug("found mapper: " + mapper.getClass().getCanonicalName() + " for: " + targetClazzName);
                return mapper;
            } else {
                return getMapper(mappers, Class.forName(targetClazzName));
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        throw new MapperNotFoundException("***DefaultProtocolCodec - cannot find mapper for: " + targetClazzName);
    }
}
