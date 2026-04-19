package com.ai.assistant.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Component
public class CompressionUtil {

    public byte[] compress(String data) {
        if (data == null || data.isEmpty()) {
            return new byte[0];
        }
        try {
            return Snappy.compress(data);
        } catch (IOException e) {
            log.error("Compression failed: {}", e.getMessage());
            return data.getBytes();
        }
    }

    public String decompress(byte[] compressedData) {
        if (compressedData == null || compressedData.length == 0) {
            return "";
        }
        try {
            return Snappy.uncompressString(compressedData);
        } catch (IOException e) {
            log.error("Decompression failed: {}", e.getMessage());
            return "";
        }
    }

    public String compressToBase64(String data) {
        byte[] compressed = compress(data);
        return Base64.getEncoder().encodeToString(compressed);
    }

    public String decompressFromBase64(String base64Data) {
        if (base64Data == null || base64Data.isEmpty()) {
            return "";
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Data);
            return decompress(decoded);
        } catch (IllegalArgumentException e) {
            log.error("Base64 decode failed: {}", e.getMessage());
            return "";
        }
    }

    public double getCompressionRatio(String original, byte[] compressed) {
        if (original == null || original.isEmpty() || compressed == null) {
            return 1.0;
        }
        return (double) compressed.length / original.getBytes().length;
    }
}