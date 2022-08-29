package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.springframework.stereotype.Component;

/**
 * Checks if the original content was compliant with the encoding used to transform it by looking
 * for the replacement character (codepoint U+FFFD).
 *
 * <p>If the character U+FFFD is present, the input content is considered as invalid.
 *
 * <p>TODO(P1) Note this approach is limited if processing files that contain explicitly this
 * character. Note sure that is valid use case though.
 *
 * @author wyau
 */
@Component
public class EncodingIntegrityChecker implements DocumentIntegrityChecker {

  private static final String REPLACEMENT_CHARACTER = "\ufffd";

  @Override
  public boolean supportsExtension(String documentExtension) {
    return true;
  }

  @Override
  public void check(String content) throws IntegrityCheckException {

    if (content.contains(REPLACEMENT_CHARACTER)) {
      throw new EncodingIntegrityCheckerException(
          "Input does not have a valid encoding (it should be UTF-8)");
    }
  }
}
