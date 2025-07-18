package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import java.util.regex.Pattern;

public class MiscAiTranslateIntegrityChecker extends AbstractTextUnitIntegrityChecker {

  private static final Pattern HASHTAG_PATTERN =
      Pattern.compile("^#\\S+$", Pattern.UNICODE_CHARACTER_CLASS);

  ForbidsControlCharIntegrityChecker forbidsControlCharIntegrityChecker =
      new ForbidsControlCharIntegrityChecker();

  @Override
  public void check(String sourceContent, String targetContent) throws IntegrityCheckException {
    checkSingleHashtag(sourceContent, targetContent);
    forbidsControlCharIntegrityChecker.check(sourceContent, targetContent);
  }

  /** Checks that if the source is a single hashtag, the target is also a valid hashtag. */
  void checkSingleHashtag(String source, String target) throws IntegrityCheckException {
    String src = source == null ? "" : source.trim();
    String tgt = target == null ? "" : target.trim();

    if (HASHTAG_PATTERN.matcher(src).matches()) {
      if (!HASHTAG_PATTERN.matcher(tgt).matches()) {
        throw new IntegrityCheckException(
            "Source is a single hashtag, but the target is not a valid hashtag: "
                + tgt
                + ". Target should start with '#' and not contain spaces or be empty.");
      }
    }
  }
}
