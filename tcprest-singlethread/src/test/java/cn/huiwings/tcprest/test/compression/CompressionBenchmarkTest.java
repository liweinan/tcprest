package cn.huiwings.tcprest.test.compression;

import cn.huiwings.tcprest.compression.CompressionConfig;
import cn.huiwings.tcprest.compression.CompressionUtil;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Performance benchmark tests for compression.
 * These tests demonstrate compression effectiveness.
 */
public class CompressionBenchmarkTest {

    @Test
    public void benchmarkCompressionRatio() throws IOException {
        System.out.println("\n=== Compression Ratio Benchmark ===");

        // Test different data types
        String[] testData = {
                generateRepetitiveText(1000),
                generateRandomText(1000),
                generateJson(100),
                generateXml(100)
        };

        String[] dataTypes = {"Repetitive Text", "Random Text", "JSON", "XML"};

        CompressionConfig config = new CompressionConfig(true, 0, 6);

        for (int i = 0; i < testData.length; i++) {
            String original = testData[i];
            String compressed = CompressionUtil.compress(original, config);

            int originalSize = original.getBytes().length;
            int compressedSize = compressed.getBytes().length;
            double ratio = CompressionUtil.getCompressionRatio(original, compressed);

            System.out.printf("%-20s: %6d bytes -> %6d bytes (%.1f%% reduction)%n",
                    dataTypes[i], originalSize, compressedSize, ratio);
        }
    }

    @Test
    public void benchmarkCompressionSpeed() throws IOException {
        System.out.println("\n=== Compression Speed Benchmark ===");

        String largeData = generateRepetitiveText(5000);
        CompressionConfig config = new CompressionConfig(true, 0, 6);

        int iterations = 100;

        // Warm up
        for (int i = 0; i < 10; i++) {
            CompressionUtil.compress(largeData, config);
        }

        // Compression benchmark
        long compressStart = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            CompressionUtil.compress(largeData, config);
        }
        long compressTime = System.currentTimeMillis() - compressStart;

        // Decompression benchmark
        String compressed = CompressionUtil.compress(largeData, config);
        long decompressStart = System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            CompressionUtil.decompress(compressed);
        }
        long decompressTime = System.currentTimeMillis() - decompressStart;

        System.out.printf("Compression:   %d iterations in %d ms (%.2f ms/op)%n",
                iterations, compressTime, (double) compressTime / iterations);
        System.out.printf("Decompression: %d iterations in %d ms (%.2f ms/op)%n",
                iterations, decompressTime, (double) decompressTime / iterations);
    }

    @Test
    public void benchmarkDifferentCompressionLevels() throws IOException {
        System.out.println("\n=== Compression Level Comparison ===");

        String testData = generateRepetitiveText(2000);
        int originalSize = testData.getBytes().length;

        System.out.printf("Original size: %d bytes%n", originalSize);
        System.out.println("Level | Compressed Size | Ratio  | Time (ms)");
        System.out.println("------|-----------------|--------|----------");

        for (int level = 1; level <= 9; level++) {
            CompressionConfig config = new CompressionConfig(true, 0, level);

            long start = System.nanoTime();
            String compressed = CompressionUtil.compress(testData, config);
            long time = (System.nanoTime() - start) / 1_000_000; // Convert to ms

            int compressedSize = compressed.getBytes().length;
            double ratio = CompressionUtil.getCompressionRatio(testData, compressed);

            System.out.printf("  %d   | %7d bytes   | %5.1f%% | %4d ms%n",
                    level, compressedSize, ratio, time);
        }
    }

    @Test
    public void demonstrateCompressionBenefit() throws IOException {
        System.out.println("\n=== Compression Benefit Analysis ===");

        int[] messageSizes = {100, 500, 1000, 5000, 10000};
        CompressionConfig config = new CompressionConfig(true, 0, 6);

        System.out.println("Message Size | Without Compression | With Compression | Saved");
        System.out.println("-------------|---------------------|------------------|-------");

        for (int size : messageSizes) {
            String data = generateRepetitiveText(size / 50); // Rough size control
            int originalSize = data.getBytes().length;

            String compressed = CompressionUtil.compress(data, config);
            int compressedSize = compressed.getBytes().length;
            int saved = originalSize - compressedSize;
            double savedPercent = (double) saved / originalSize * 100;

            System.out.printf("%8d KB | %12d bytes | %11d bytes | %d bytes (%.1f%%)%n",
                    originalSize / 1024,
                    originalSize,
                    compressedSize,
                    saved,
                    savedPercent);
        }
    }

    // Helper methods to generate test data

    private String generateRepetitiveText(int lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            sb.append("This is a test line number ").append(i).append(". ");
            sb.append("Repetitive content compresses very well. ");
        }
        return sb.toString();
    }

    private String generateRandomText(int words) {
        StringBuilder sb = new StringBuilder();
        String chars = "abcdefghijklmnopqrstuvwxyz ";
        java.util.Random random = new java.util.Random(42); // Fixed seed for reproducibility

        for (int i = 0; i < words * 10; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private String generateJson(int objects) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < objects; i++) {
            if (i > 0) sb.append(",");
            sb.append("{");
            sb.append("\"id\":").append(i).append(",");
            sb.append("\"name\":\"User").append(i).append("\",");
            sb.append("\"email\":\"user").append(i).append("@example.com\",");
            sb.append("\"active\":true");
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String generateXml(int elements) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?><root>");
        for (int i = 0; i < elements; i++) {
            sb.append("<item id=\"").append(i).append("\">");
            sb.append("<name>Item ").append(i).append("</name>");
            sb.append("<description>Description for item ").append(i).append("</description>");
            sb.append("</item>");
        }
        sb.append("</root>");
        return sb.toString();
    }
}
