package com.box.l10n.mojito.okapi;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.sf.okapi.common.LocaleId;
import org.springframework.util.ReflectionUtils;

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

    Field inputURIField = ReflectionUtils.findField(RawDocument.class, "inputURI");
    ReflectionUtils.makeAccessible(inputURIField);

    try {
      URI fakeUri = new URI("/some/file/path/to/be/read/from/db");
      ReflectionUtils.setField(inputURIField, this, fakeUri);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns a fake output URI to be used where the Okapi class were modify to work with streams
   * instead of files.
   *
   * @return a fake output URI form streams
   */
  public static URI getFakeOutputURIForStream() {
    return new File("fakeOuputURIForStream-" + UUID.randomUUID()).toURI();
  }
}
