package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class PluralIntegrityCheckerRelaxer {

  /**
   * This is very ad hoc!
   *
   * <p>There are cases where the plural form doesn’t retain the number. Right now, those are
   * wrongly marked as rejected. Ideally, we should clearly identify which placeholder might be
   * missing from the string, but that’s another level of complexity. Also, use the class name to
   * target certain integrity checks to keep this hack constrained.
   */
  public boolean shouldRelaxIntegrityCheck(
      String source, String target, String pluralForm, TextUnitIntegrityChecker textUnitChecker) {

    boolean shouldRelax = false;

    if (pluralForm != null && !"other".equals(pluralForm)) {
      if (textUnitChecker instanceof PrintfLikeIntegrityChecker
          || textUnitChecker instanceof PrintfLikeIgnorePercentageAfterBracketsIntegrityChecker
          || textUnitChecker instanceof PrintfLikeVariableTypeIntegrityChecker
          || textUnitChecker instanceof SimplePrintfLikeIntegrityChecker) {

        RegexIntegrityChecker regexIntegrityChecker = (RegexIntegrityChecker) textUnitChecker;

        Set<String> sourcePlaceholders = regexIntegrityChecker.getPlaceholders(source);
        Set<String> targetPlaceholders = regexIntegrityChecker.getPlaceholders(target);

        if (sourcePlaceholders.size() - targetPlaceholders.size() <= 1) {
          shouldRelax = true;
        }
      }
    }

    return shouldRelax;
  }
}
