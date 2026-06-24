package com.sifa.core_sifa.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class GzipUtil {

    private GzipUtil() {}

    public static byte[] compress(byte[] data) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(data);
        }
        return baos.toByteArray();
    }

    public static String decompress(byte[] data) throws IOException {
        try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(data));
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            gzis.transferTo(baos);
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    public static boolean isValidGzip(byte[] data) {
        if (data == null || data.length < 2) return false;
        return (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B;
    }
}
