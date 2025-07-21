package com.box.l10n.mojito.service.oaitranslate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiTranslateTargetAutoFix {

  static Logger logger = LoggerFactory.getLogger(AiTranslateTargetAutoFix.class);

  public static String fixTarget(String source, String target) {
    target = WhitespaceUtils.restoreLeadingAndTrailingWhitespace(source, target);
    target = fixCorruptedNonBreakingSpace(target);
    return target;
  }

  /**
   * For now, only handle the corrupted non-breaking space: "\u0000a0" becomes "\u00a0". If we see
   * more cases in the future, generalize this logic. The corruption is: "\u0000" is wrongly
   * inserted, and the next two characters are the hex code for the intended character (as in
   * "\\u00{char-code}").
   */
  static String fixCorruptedNonBreakingSpace(String target) {

    String res = null;

    if (target != null) {
      res = target.replace("\u0000a0", "\u00a0");
      if (!target.equals(res)) {
        logger.info(
            "Replaced corrupted character '\\u0000a0' with non-breaking space in: [{}]", target);
      }
    }

    return res;
  }
}
