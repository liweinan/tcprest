package cn.huiwings.tcprest.compression;

/**
 * Configuration for data compression in TcpRest protocol.
 * Compression uses JDK built-in GZIP (zero external dependencies).
 *
 * @author Weinan Li
 */
public class CompressionConfig {

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
                '}';
    }
}
