package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import com.box.l10n.mojito.common.notification.SlackMessageBuilder;
import com.box.l10n.mojito.slack.SlackClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Throws IntegrityCheckerException if the target has a span tag translation instruction.
 *
 * @author mattwilshire
 */
public class SpanTranslateTagIntegrityChecker extends RegexIntegrityChecker {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(SpanTranslateTagIntegrityChecker.class);

  @Override
  public String getRegex() {
    return "<.*span translate[^>]*>";
  }

  @Override
  public void check(String sourceContent, String targetContent) throws IntegrityCheckException {
    if (!matchesRegex(targetContent)) return;

    SpanTranslateTagIntegrityCheckerException spanTranslateException =
        new SpanTranslateTagIntegrityCheckerException(
            "Span tag with translation instruction found in translated string.");
    /*
      Integrity Check Notifier
    */
    if (getIntegrityCheckNotifier() != null) {
      try {
        getIntegrityCheckNotifier()
            .sendWarning(
                new SlackMessageBuilder(getIntegrityCheckNotifier().getConfiguration())
                    .warnTagIntegrity(
                        spanTranslateException,
                        sourceContent,
                        targetContent,
                        this.getRepository(),
                        this.getTextUnitId()));
      } catch (SlackClientException e) {
        logger.error("Error sending Slack warning message", e);
      }
    }
    throw spanTranslateException;
  }

  boolean matchesRegex(String targetContent) {
    return getPattern().matcher(targetContent).find();
  }
}
