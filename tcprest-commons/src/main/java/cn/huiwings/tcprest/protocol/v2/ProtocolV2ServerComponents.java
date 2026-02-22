package cn.huiwings.tcprest.protocol.v2;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import cn.huiwings.tcprest.codec.v2.ProtocolV2Codec;
import cn.huiwings.tcprest.invoker.v2.ProtocolV2Invoker;
import cn.huiwings.tcprest.mapper.Mapper;
import cn.huiwings.tcprest.parser.RequestParser;
import cn.huiwings.tcprest.parser.v2.ProtocolV2Parser;
import cn.huiwings.tcprest.security.SecurityConfig;

import java.util.Map;

/**
 * Holds and creates Protocol V2 server-side components (parser, invoker, codec).
 * Centralizes V2 component creation and security config so the server does not
 * depend on concrete V2 implementation types.
 *
 * @since 1.1.0
 */
public final class ProtocolV2ServerComponents {

    private final RequestParser parser;
    private final ProtocolV2Invoker invoker;
    private final ProtocolCodec codec;

    private ProtocolV2ServerComponents(RequestParser parser, ProtocolV2Invoker invoker, ProtocolCodec codec) {
        this.parser = parser;
        this.invoker = invoker;
        this.codec = codec;
    }

    /**
     * Create V2 parser, invoker, and codec; apply security config to parser and codec if non-null.
     */
    public static ProtocolV2ServerComponents create(Map<String, Mapper> mappers, SecurityConfig securityConfig) {
        RequestParser p = new ProtocolV2Parser(mappers);
        ProtocolV2Invoker inv = new ProtocolV2Invoker();
        ProtocolCodec c = new ProtocolV2Codec(mappers);
        if (securityConfig != null) {
            ((ProtocolV2Parser) p).setSecurityConfig(securityConfig);
            ((ProtocolV2Codec) c).setSecurityConfig(securityConfig);
        }
        return new ProtocolV2ServerComponents(p, inv, c);
    }

    public RequestParser getParser() {
        return parser;
    }

    public ProtocolV2Invoker getInvoker() {
        return invoker;
    }

    public ProtocolCodec getCodec() {
        return codec;
    }

    /**
     * Apply security config to parser and codec (used when config is set after component creation).
     */
    public void setSecurityConfig(SecurityConfig securityConfig) {
        if (securityConfig != null) {
            ((ProtocolV2Parser) parser).setSecurityConfig(securityConfig);
            ((ProtocolV2Codec) codec).setSecurityConfig(securityConfig);
        }
    }

    /**
     * Encode a successful response.
     */
    public String encodeResponse(Object result, StatusCode status) {
        return ((ProtocolV2Codec) codec).encodeResponse(result, status);
    }

    /**
     * Encode an exception as a V2 error response.
     */
    public String encodeException(Throwable error, StatusCode status) {
        return ((ProtocolV2Codec) codec).encodeException(error, status);
    }
}
