package com.box.l10n.mojito.common;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

/**
 * Utils to work with Streams.
 *
 * @author jaurambault
 */
public class StreamUtil {

  /**
   * Transforms a {@link ByteArrayOutputStream} into a {@link String} using UTF8 charset.
   *
   * <p>This function wraps {@link UnsupportedEncodingException} into a {@link RuntimeException} as
   * UTF8 is expected to be supported.
   *
   * @param baos stream to be converted
   * @return The {@link ByteArrayOutputStream} as {@link String}
   */
  public static String getUTF8OutputStreamAsString(ByteArrayOutputStream baos) {
    try {
      return baos.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException uee) {
      throw new RuntimeException(uee);
    }
  }
}
