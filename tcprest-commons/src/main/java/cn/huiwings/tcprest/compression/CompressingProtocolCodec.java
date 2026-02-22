package cn.huiwings.tcprest.compression;

import cn.huiwings.tcprest.codec.ProtocolCodec;
import java.util.logging.Logger;
import cn.huiwings.tcprest.mapper.Mapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Decorator for ProtocolCodec that adds optional GZIP compression support.
 * Uses JDK built-in compression (zero external dependencies).
 *
 * This wrapper applies compression to the final encoded message,
 * and decompresses received messages before passing to the underlying converter.
 *
 * @author Weinan Li
 */
public class CompressingProtocolCodec implements ProtocolCodec {

    private final ProtocolCodec delegate;
    private final CompressionConfig config;
    private final Logger logger = Logger.getLogger(CompressingProtocolCodec.class.getName());

    public CompressingProtocolCodec(ProtocolCodec delegate, CompressionConfig config) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate codec cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Compression config cannot be null");
        }
        this.delegate = delegate;
        this.config = config;
    }

    /**
     * Encode and optionally compress the message
     */
    @Override
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers) {

        // First, use delegate to encode
        String encoded = delegate.encode(clazz, method, params, mappers);

        // Then apply compression if enabled
        if (config.isEnabled()) {
            try {
                String compressed = CompressionUtil.compress(encoded, config);
                if (CompressionUtil.isCompressed(compressed)) {
                    double ratio = CompressionUtil.getCompressionRatio(encoded, compressed);
                    logger.fine("Compressed message: " + encoded.length() + " -> " +
                            (compressed.length() - 2) + " bytes (saved " +
                            String.format("%.1f", ratio) + "%)");
                }
                return compressed;
            } catch (IOException e) {
                logger.severe("Compression failed, sending uncompressed: " + e.getMessage());
                return "0|" + encoded; // Fallback to uncompressed with prefix
            }
        } else {
            // Compression disabled, but add prefix for protocol compatibility
            return "0|" + encoded;
        }
    }

    /**
     * Decompress and decode the message
     */
    @Override
    public Object[] decode(Method targetMethod, String paramsToken, Map<String, Mapper> mappers) {

        // First decompress if needed (with configurable max size for zip-bomb protection)
        String decompressed = paramsToken;
        if (paramsToken != null) {
            try {
                int maxSize = config.getMaxDecompressedSize();
                decompressed = maxSize > 0
                        ? CompressionUtil.decompress(paramsToken, maxSize)
                        : CompressionUtil.decompress(paramsToken);
                if (CompressionUtil.isCompressed(paramsToken)) {
                    logger.fine("Decompressed message: " + paramsToken.length() + " -> " +
                            decompressed.length() + " bytes");
                }
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("DECOMPRESSED_SIZE_EXCEEDED:")) {
                    logger.severe("Decompression rejected: " + e.getMessage());
                    throw new RuntimeException(e);
                }
                logger.severe("Decompression failed, trying as-is: " + e.getMessage());
                decompressed = paramsToken;
            }
        }

        // Then delegate to underlying converter
        return delegate.decode(targetMethod, decompressed, mappers);
    }

    @Override
    public String encodeParam(String message) {
        return delegate.encodeParam(message);
    }

    @Override
    public String decodeParam(String message) {
        return delegate.decodeParam(message);
    }

    @Override
    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) {
        return delegate.getMapper(mappers, targetClazz);
    }

    @Override
    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) {
        return delegate.getMapper(mappers, targetClazzName);
    }

    public CompressionConfig getConfig() {
        return config;
    }
}
