package cn.huiwings.tcprest.compression;

/**
 * Configuration for data compression in TcpRest protocol.
 * Compression uses JDK built-in GZIP (zero external dependencies).
 *
 * @author Weinan Li
 */
public class CompressionConfig {

    /**
     * Default maximum decompressed size in bytes (10MB). Used to mitigate zip-bomb / DoS risk.
     */
    public static final int DEFAULT_MAX_DECOMPRESSED_SIZE = 10 * 1024 * 1024;

    /**
     * Enable/disable compression. Default: false (disabled for backward compatibility)
     */
    private boolean enabled = false;

    /**
     * Minimum size (in bytes) for compression to be applied.
     * Messages smaller than this threshold are not compressed.
     * Default: 1024 bytes (1KB)
     */
    private int compressionThreshold = 1024;

    /**
     * Compression level (0-9).
     * 0 = no compression, 1 = fastest, 9 = best compression
     * Default: 6 (balanced)
     */
    private int compressionLevel = 6;

    /**
     * Maximum decompressed size in bytes. When positive, decompression will throw if output exceeds this limit (zip-bomb protection).
     * 0 means no limit (use with care).
     * Default: {@value #DEFAULT_MAX_DECOMPRESSED_SIZE} (10MB)
     */
    private int maxDecompressedSize = DEFAULT_MAX_DECOMPRESSED_SIZE;

    public CompressionConfig() {
    }

    public CompressionConfig(boolean enabled) {
        this.enabled = enabled;
    }

    public CompressionConfig(boolean enabled, int compressionThreshold, int compressionLevel) {
        this.enabled = enabled;
        this.compressionThreshold = compressionThreshold;
        setCompressionLevel(compressionLevel);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getCompressionThreshold() {
        return compressionThreshold;
    }

    public void setCompressionThreshold(int compressionThreshold) {
        if (compressionThreshold < 0) {
            throw new IllegalArgumentException("Compression threshold must be non-negative");
        }
        this.compressionThreshold = compressionThreshold;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("Compression level must be between 0 and 9");
        }
        this.compressionLevel = compressionLevel;
    }

    /**
     * Maximum decompressed size in bytes (0 = no limit).
     *
     * @return max bytes, or 0 for no limit
     */
    public int getMaxDecompressedSize() {
        return maxDecompressedSize;
    }

    /**
     * Set maximum decompressed size (zip-bomb protection).
     *
     * @param maxDecompressedSize max bytes; 0 to disable limit
     */
    public void setMaxDecompressedSize(int maxDecompressedSize) {
        if (maxDecompressedSize < 0) {
            throw new IllegalArgumentException("Max decompressed size must be non-negative");
        }
        this.maxDecompressedSize = maxDecompressedSize;
    }

    /**
     * Check if message should be compressed based on size threshold
     */
    public boolean shouldCompress(int messageSize) {
        return enabled && messageSize >= compressionThreshold;
    }

    @Override
    public String toString() {
        return "CompressionConfig{" +
                "enabled=" + enabled +
                ", threshold=" + compressionThreshold +
                ", level=" + compressionLevel +
                ", maxDecompressedSize=" + maxDecompressedSize +
                '}';
    }
}
