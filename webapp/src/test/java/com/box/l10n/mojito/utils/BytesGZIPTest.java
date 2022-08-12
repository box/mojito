package com.box.l10n.mojito.utils;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.*;

public class BytesGZIPTest {

    static final String STR = "some string to compress";
    static final byte[] DECOMPRESSED = STR.getBytes(StandardCharsets.UTF_8);
    static final byte[] COMPRESSED = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 43, -50, -49, 77, 85, 40, 46, 41, -54,
            -52, 75, 87, 40, -55, 87, 72, -50, -49, 45, 40, 74, 45, 46, 6, 0, -35, -34, 126, -63, 23, 0, 0, 0};

    static final byte[] COMPRESSED_JDK17 = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, -1, 43, -50, -49, 77, 85, 40, 46,
            41, -54, -52, 75, 87, 40, -55, 87, 72, -50, -49, 45, 40, 74, 45, 46, 6, 0, -35, -34, 126, -63, 23, 0, 0, 0};

    @Test
    public void compress() {
        byte[] compress = BytesGZIP.compress(DECOMPRESSED);
        // While decompression is guaranteed to give us the original input, compression can change with different
        // implementations over time. TODO: remove compression test or assert only that the compressed byte array
        // differs from the input?
        assertTrue(Arrays.equals(COMPRESSED, compress) || Arrays.equals(COMPRESSED_JDK17, compress));
    }

    @Test
    public void decompress() {
        byte[] decompress = BytesGZIP.decompress(COMPRESSED);
        assertArrayEquals(DECOMPRESSED, decompress);
    }

    @Test
    public void decompressOrOriginalValid() {
        byte[] decompress = BytesGZIP.decompressOrOriginal(COMPRESSED);
        assertArrayEquals(DECOMPRESSED, decompress);
    }

    @Test
    public void decompressOrOriginalInvalid() {
        byte[] invalid = new byte[]{};
        byte[] decompress = BytesGZIP.decompressOrOriginal(invalid);
        assertEquals(invalid, decompress);
    }

}