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

  private static final String EMPTY = "";

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

  /**
   * Creates new instance of an empty in-memory RawDocument that can be fed to the {@link
   * net.sf.okapi.common.pipelinedriver.PipelineDriver#addBatchItem} when using an {@link
   * net.sf.okapi.common.filters.IFilter} that generates pipeline events from non-document locations
   * (e.g. {@link com.box.l10n.mojito.service.tm.TMExportFilter}
   */
  public static RawDocument createFakeDocument(LocaleId sourceLocale, LocaleId targetLocale) {
    return new RawDocument(EMPTY, sourceLocale, targetLocale);
  }
}
