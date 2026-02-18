package cn.huiwings.tcprest.extractor;

import cn.huiwings.tcprest.converter.Converter;
import cn.huiwings.tcprest.converter.DefaultConverter;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.exception.ParseException;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.protocol.TcpRestProtocol;
import cn.huiwings.tcprest.security.ProtocolSecurity;
import cn.huiwings.tcprest.security.SecurityConfig;
import cn.huiwings.tcprest.server.Context;
import cn.huiwings.tcprest.server.TcpRestServer;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Security-Enhanced Default Extractor for V1 protocol.
 *
 * <p><b>New Protocol Format (2026-02-18):</b></p>
 * <pre>
 * Request: 0|{{base64(ClassName/methodName)}}|{{base64(params)}}|CHK:value
 * </pre>
 *
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>Base64-encoded metadata prevents injection attacks</li>
 *   <li>Optional checksum verification (CRC32/HMAC)</li>
 *   <li>Optional class whitelist validation</li>
 * </ul>
 *
 * @author Weinan Li
 * @date Jul 30 2012
 * @deprecated Use Protocol V2 ({@link cn.huiwings.tcprest.extractor.v2.ProtocolV2Extractor}) instead.
 *             V1 is maintained for backward compatibility only. V2 provides method overloading,
 *             better exception handling, and intelligent mapper support with auto-serialization.
 */
@Deprecated
public class DefaultExtractor implements Extractor {

    private Logger logger = LoggerFactory.getDefaultLogger();
    private TcpRestServer tcpRestServer;
    private final Converter converter;
    private SecurityConfig securityConfig;

    public DefaultExtractor(TcpRestServer server) {
        this.tcpRestServer = server;
        this.securityConfig = new SecurityConfig(); // Default: no security
        this.converter = new DefaultConverter(securityConfig);
    }

    /**
     * Sets security configuration.
     *
     * @param securityConfig security config
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        this.securityConfig = securityConfig != null ? securityConfig : new SecurityConfig();
        if (converter instanceof DefaultConverter) {
            ((DefaultConverter) converter).setSecurityConfig(this.securityConfig);
        }
    }

    /**
     * Extracts request into Context.
     *
     * <p>Format: {@code 0|META|PARAMS|CHK:value}</p>
     *
     * @param request protocol request string
     * @return populated Context
     * @throws ClassNotFoundException if class not found
     * @throws NoSuchMethodException if method not found
     * @throws ParseException if request format invalid
     * @throws MapperNotFoundException if mapper not found
     */
    public Context extract(String request) throws ClassNotFoundException, NoSuchMethodException, ParseException, MapperNotFoundException {
        if (request == null) {
            throw new ParseException("***DefaultExtractor: cannot parse null request");
        }

        // Remove line breaks
        request = request.replaceAll("(\\r|\\n)", "").trim();

        logger.debug("***DefaultExtractor - parsing request: " + request);

        // Step 1: Split checksum if present
        String[] parts = ProtocolSecurity.splitChecksum(request);
        String messageWithoutChecksum = parts[0];
        String checksum = parts[1];

        // Step 2: Verify checksum if enabled
        if (!checksum.isEmpty()) {
            if (!ProtocolSecurity.verifyChecksum(messageWithoutChecksum, checksum, securityConfig)) {
                throw new cn.huiwings.tcprest.exception.SecurityException(
                    "Checksum verification failed - message may have been tampered with"
                );
            }
            logger.debug("***DefaultExtractor - checksum verified");
        }

        // Step 3: Split components by |
        String[] components = messageWithoutChecksum.split("\\" + TcpRestProtocol.COMPONENT_SEPARATOR, -1);

        if (components.length < 3) {
            throw new ParseException("***DefaultExtractor: invalid protocol format, expected: 0|META|PARAMS, got: " + request);
        }

        String compressionFlag = components[0];
        String metaBase64 = components[1];
        String paramsBase64 = components[2];

        logger.debug("***DefaultExtractor - compression: " + compressionFlag);
        logger.debug("***DefaultExtractor - metaBase64: " + metaBase64);
        logger.debug("***DefaultExtractor - paramsBase64: " + paramsBase64);

        // Step 4: Decode metadata (ClassName/methodName)
        String meta = ProtocolSecurity.decodeComponent(metaBase64);
        logger.debug("***DefaultExtractor - decoded meta: " + meta);

        // Step 5: Parse ClassName and methodName from meta
        int slashIndex = meta.indexOf('/');
        if (slashIndex < 1) {
            throw new ParseException("***DefaultExtractor: invalid metadata format, expected ClassName/methodName, got: " + meta);
        }

        String clazzName = meta.substring(0, slashIndex);
        String methodName = meta.substring(slashIndex + 1);

        // Remove trailing () if present for compatibility
        if (methodName.endsWith("()")) {
            methodName = methodName.substring(0, methodName.length() - 2);
        } else if (methodName.endsWith(")")) {
            // Strip parameters part if present (e.g., "method(params)" -> "method")
            int parenIndex = methodName.indexOf('(');
            if (parenIndex > 0) {
                methodName = methodName.substring(0, parenIndex);
            }
        }

        logger.debug("***DefaultExtractor - className: " + clazzName);
        logger.debug("***DefaultExtractor - methodName: " + methodName);

        // Step 6: Validate class name (security check)
        if (!ProtocolSecurity.isValidClassName(clazzName)) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Invalid class name format (possible injection attempt): " + clazzName
            );
        }

        // Step 7: Check class whitelist if enabled
        if (!securityConfig.isClassAllowed(clazzName)) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Class not in whitelist: " + clazzName
            );
        }

        // Step 8: Validate method name (security check)
        if (!ProtocolSecurity.isValidMethodName(methodName)) {
            throw new cn.huiwings.tcprest.exception.SecurityException(
                "Invalid method name format (possible injection attempt): " + methodName
            );
        }

        // Step 9: Find resource class (check both singleton and class resources)
        List<Class> classesToSearch = new ArrayList<Class>();

        for (Class clazz : tcpRestServer.getResourceClasses().values()) {
            classesToSearch.add(clazz);
        }

        for (Object instance : tcpRestServer.getSingletonResources().values()) {
            classesToSearch.add(instance.getClass());
        }

        // Search logic - support interface-based calls
        for (Class clazz : classesToSearch) {
            if (clazzName.equals(clazz.getCanonicalName())) {
                break; // Found exact match
            } else {
                // Check if it's an interface implemented by this class
                for (Class ifc : clazz.getInterfaces()) {
                    if (ifc.getCanonicalName().equals(clazzName)) {
                        logger.log("***DefaultExtractor - found implemented class: " + clazz.getCanonicalName());
                        clazzName = clazz.getCanonicalName();
                        break;
                    }
                }
            }
        }

        // Step 10: Create Context
        Context ctx = new Context();

        // Set target instance (singleton takes precedence)
        if (tcpRestServer.getSingletonResources().containsKey(clazzName)) {
            ctx.setTargetInstance(tcpRestServer.getSingletonResources().get(clazzName));
            ctx.setTargetClass(tcpRestServer.getSingletonResources().get(clazzName).getClass());
        } else if (tcpRestServer.getResourceClasses().containsKey(clazzName)) {
            ctx.setTargetClass(tcpRestServer.getResourceClasses().get(clazzName));
        }

        if (ctx.getTargetClass() == null) {
            throw new ClassNotFoundException("***DefaultExtractor - Can't find resource for: " + clazzName);
        }

        // Step 11: Find method
        // V1 protocol: stops at first name match (no overloading support)
        // V2 protocol uses type signatures for overloading
        logger.debug("target method name: " + methodName);
        for (Method mtd : ctx.getTargetClass().getDeclaredMethods()) {
            logger.debug("scanning method: " + mtd.getName());
            if (mtd.getName().equals(methodName.trim())) {
                logger.debug("found method: " + mtd.getName());
                ctx.setTargetMethod(mtd);
                break;
            }
        }

        if (ctx.getTargetMethod() == null) {
            throw new NoSuchMethodException("***DefaultExtractor - Can't find method: " + methodName);
        }

        // Step 12: Decode and parse parameters
        Object params[] = converter.decode(ctx.getTargetMethod(), paramsBase64, tcpRestServer.getMappers());

        ctx.setParams(params);
        ctx.setConverter(converter);
        return ctx;
    }
}
