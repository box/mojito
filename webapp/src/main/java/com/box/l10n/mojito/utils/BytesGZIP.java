package com.box.l10n.mojito.utils;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * In memory GZip compression for byte arrays
 */
public class BytesGZIP {

    static final Logger logger = LoggerFactory.getLogger(BytesGZIP.class);

    public static byte[] compress(byte[] bytes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream out = new GZIPOutputStream(baos);
            out.write(bytes, 0, bytes.length);
            out.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        try {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(bytes));
            return ByteStreams.toByteArray(gis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static byte[] decompressOrOriginal(byte[] bytes) {

        byte[] decompressed = bytes;

        if (hasGZipMagicNumber(bytes)) {
            try {
                decompressed = BytesGZIP.decompress(bytes);
            } catch (UncheckedIOException e) {
                logger.debug("Not able to decompress, return original. Allows to accept content with GZip header that " +
                        "is not actual GZip content. Drawback is that it could hide other IOException", e);
            }
        }

        return decompressed;
    }

    /**
     * See {@link GZIPInputStream#GZIP_MAGIC}
     */
    static boolean hasGZipMagicNumber(byte[] bytes) {
        return bytes.length > 1 && bytes[0] == 0x1f && bytes[1] == (byte) 0x8b;
    }
}
