package com.box.l10n.mojito.okapi;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import net.sf.okapi.common.LocaleId;

/**
 * When creating {@link RawDocument} from a string the URI is not set and setter is not available.
 *
 * @author jaurambault
 */
public class RawDocument extends net.sf.okapi.common.resource.RawDocument {

  public static String EMPTY = "";

  public RawDocument(CharSequence inputCharSequence, LocaleId sourceLocale) {
    this(inputCharSequence, sourceLocale, LocaleId.EMPTY);
  }

  public RawDocument(CharSequence inputCharSequence, LocaleId sourceLocale, LocaleId targetLocale) {
    super(
        new ByteArrayInputStream(inputCharSequence.toString().getBytes(StandardCharsets.UTF_8)),
        StandardCharsets.UTF_8.name().toLowerCase(),
        sourceLocale,
        targetLocale);
  }
}
