package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IntegrityChecker that forbids ASCII control characters in strings, except for tab, newline, and
 * carriage return.
 */
public class ForbidsControlCharIntegrityChecker extends AbstractTextUnitIntegrityChecker {

  static Logger logger = LoggerFactory.getLogger(ForbidsControlCharIntegrityChecker.class);

  @Override
  public void check(String sourceContent, String targetContent) throws IntegrityCheckException {
    for (int i = 0; i < targetContent.length(); i++) {
      char ch = targetContent.charAt(i);
      if ((ch < 0x20) && (ch != '\t') && (ch != '\n') && (ch != '\r')) {
        String message =
            String.format(
                "Forbidden control character (U+%04X) at position %d in string (targetContent: %s): ...%s...",
                (int) ch, i, targetContent, getExcerpt(targetContent, i));
        logger.error(message);
        throw new IntegrityCheckException(message);
      }
    }
  }

  private String getExcerpt(String text, int pos) {
    int start = Math.max(0, pos - 10);
    int end = Math.min(text.length(), pos + 10);
    String excerpt = text.substring(start, end).replaceAll("[\\x00-\\x1F]", "[CTRL]");
    return excerpt;
  }
}
