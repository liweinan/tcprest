package cn.huiwings.tcprest.test.compression;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.testng.Assert.*;

/**
 * Tests for CompressionUtil
 */
public class CompressionUtilTest {

    @Test
    public void testCompressAndDecompressSmallData() throws IOException {
        String original = "Hello, World!";
        CompressionConfig config = new CompressionConfig(true, 0, 6); // Compress everything

        String compressed = CompressionUtil.compress(original, config);
        assertNotNull(compressed);
        assertTrue(compressed.startsWith("0|") || compressed.startsWith("1|"), "Should have compression prefix");

        String decompressed = CompressionUtil.decompress(compressed);
        assertEquals(decompressed, original, "Decompressed data should match original");
    }

    @Test
    public void testCompressLargeData() throws IOException {
        // Create large repetitive string (compresses well)
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("This is a test string that repeats many times. ");
        }
        String original = sb.toString();

        CompressionConfig config = new CompressionConfig(true, 100, 9); // Best compression

        String compressed = CompressionUtil.compress(original, config);
        assertTrue(CompressionUtil.isCompressed(compressed), "Should be compressed");

        String decompressed = CompressionUtil.decompress(compressed);
        assertEquals(decompressed, original, "Decompressed data should match original");

        double ratio = CompressionUtil.getCompressionRatio(original, compressed);
        assertTrue(ratio > 50, "Should achieve >50% compression on repetitive data, got: " + ratio + "%");
    }

    @Test
    public void testCompressionThreshold() throws IOException {
        String smallData = "Small";
        CompressionConfig config = new CompressionConfig(true, 1024, 6); // 1KB threshold

        String result = CompressionUtil.compress(smallData, config);
        assertFalse(CompressionUtil.isCompressed(result), "Small data should not be compressed");
        assertEquals(CompressionUtil.decompress(result), smallData);
    }

    @Test
    public void testDecompressUncompressedData() throws IOException {
        String original = "Uncompressed data";
        String prefixed = "0|" + original;

        String decompressed = CompressionUtil.decompress(prefixed);
        assertEquals(decompressed, original);
    }

    @Test
    public void testDecompressLegacyDataWithoutPrefix() throws IOException {
        String legacy = "Legacy data without prefix";

        String decompressed = CompressionUtil.decompress(legacy);
        assertEquals(decompressed, legacy, "Should return as-is for legacy format");
    }

    @Test
    public void testIsCompressed() throws IOException {
        CompressionConfig config = new CompressionConfig(true, 0, 6);

        String largeData = "X".repeat(5000); // Large enough to compress
        String compressed = CompressionUtil.compress(largeData, config);

        if (CompressionUtil.isCompressed(compressed)) {
            assertTrue(compressed.startsWith("1|"));
        } else {
            assertTrue(compressed.startsWith("0|"));
        }
    }

    @Test
    public void testCompressionRatio() throws IOException {
        String original = "Test ".repeat(1000);
        CompressionConfig config = new CompressionConfig(true, 0, 9);

        String compressed = CompressionUtil.compress(original, config);

        if (CompressionUtil.isCompressed(compressed)) {
            double ratio = CompressionUtil.getCompressionRatio(original, compressed);
            assertTrue(ratio > 0, "Compression ratio should be positive");
            assertTrue(ratio < 100, "Compression ratio should be less than 100%");
        }
    }

    @Test
    public void testNullHandling() throws IOException {
        assertNull(CompressionUtil.compress(null, new CompressionConfig(true)));
        assertNull(CompressionUtil.decompress(null));
    }

    @Test
    public void testEmptyString() throws IOException {
        String empty = "";
        CompressionConfig config = new CompressionConfig(true, 0, 6);

        String compressed = CompressionUtil.compress(empty, config);
        String decompressed = CompressionUtil.decompress(compressed);
        assertEquals(decompressed, empty);
    }

    @Test
    public void testDifferentCompressionLevels() throws IOException {
        String data = "Test data ".repeat(500);

        for (int level = 1; level <= 9; level++) {
            CompressionConfig config = new CompressionConfig(true, 0, level);
            String compressed = CompressionUtil.compress(data, config);
            String decompressed = CompressionUtil.decompress(compressed);
            assertEquals(decompressed, data, "Level " + level + " should work");
        }
    }

    @Test
    public void testUnicodeData() throws IOException {
        String unicode = "Hello 世界 🌍 مرحبا мир";
        CompressionConfig config = new CompressionConfig(true, 0, 6);

        String compressed = CompressionUtil.compress(unicode, config);
        String decompressed = CompressionUtil.decompress(compressed);
        assertEquals(decompressed, unicode, "Unicode should be preserved");
    }
}
