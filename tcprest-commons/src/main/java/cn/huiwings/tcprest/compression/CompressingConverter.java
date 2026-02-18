package cn.huiwings.tcprest.compression;

import cn.huiwings.tcprest.conveter.Converter;
import cn.huiwings.tcprest.exception.MapperNotFoundException;
import cn.huiwings.tcprest.logger.Logger;
import cn.huiwings.tcprest.logger.LoggerFactory;
import cn.huiwings.tcprest.mapper.Mapper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Decorator for Converter that adds optional GZIP compression support.
 * Uses JDK built-in compression (zero external dependencies).
 *
 * This wrapper applies compression to the final encoded message,
 * and decompresses received messages before passing to the underlying converter.
 *
 * @author Weinan Li
 */
public class CompressingConverter implements Converter {

    private final Converter delegate;
    private final CompressionConfig config;
    private final Logger logger = LoggerFactory.getDefaultLogger();

    public CompressingConverter(Converter delegate, CompressionConfig config) {
        if (delegate == null) {
            throw new IllegalArgumentException("Delegate converter cannot be null");
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
    public String encode(Class clazz, Method method, Object[] params, Map<String, Mapper> mappers)
            throws MapperNotFoundException {

        // First, use delegate to encode
        String encoded = delegate.encode(clazz, method, params, mappers);

        // Then apply compression if enabled
        if (config.isEnabled()) {
            try {
                String compressed = CompressionUtil.compress(encoded, config);
                if (CompressionUtil.isCompressed(compressed)) {
                    double ratio = CompressionUtil.getCompressionRatio(encoded, compressed);
                    logger.debug("Compressed message: " + encoded.length() + " -> " +
                            (compressed.length() - 2) + " bytes (saved " +
                            String.format("%.1f", ratio) + "%)");
                }
                return compressed;
            } catch (IOException e) {
                logger.error("Compression failed, sending uncompressed: " + e.getMessage());
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
    public Object[] decode(Method targetMethod, String paramsToken, Map<String, Mapper> mappers)
            throws MapperNotFoundException {

        // First decompress if needed
        String decompressed = paramsToken;
        if (paramsToken != null) {
            try {
                decompressed = CompressionUtil.decompress(paramsToken);
                if (CompressionUtil.isCompressed(paramsToken)) {
                    logger.debug("Decompressed message: " + paramsToken.length() + " -> " +
                            decompressed.length() + " bytes");
                }
            } catch (IOException e) {
                logger.error("Decompression failed, trying as-is: " + e.getMessage());
                // If decompression fails, try using original (might be legacy format)
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
    public Mapper getMapper(Map<String, Mapper> mappers, Class targetClazz) throws MapperNotFoundException {
        return delegate.getMapper(mappers, targetClazz);
    }

    @Override
    public Mapper getMapper(Map<String, Mapper> mappers, String targetClazzName) throws MapperNotFoundException {
        return delegate.getMapper(mappers, targetClazzName);
    }

    public CompressionConfig getConfig() {
        return config;
    }
}
