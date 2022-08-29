package com.box.l10n.mojito.cli.command.extractioncheck;

import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import java.util.List;
import java.util.stream.Stream;

public abstract class ExtractionCheckNotificationSender {

  public static String QUOTE_MARKER = "`";

  final String hardFailureMessage;
  final String messageTemplate;
  final String checksSkippedMessage;

  public ExtractionCheckNotificationSender(
      String messageTemplate, String hardFailureMessage, String checksSkippedMessage) {
    this.messageTemplate = messageTemplate;
    this.hardFailureMessage = hardFailureMessage;
    this.checksSkippedMessage = checksSkippedMessage;
  }

  public abstract void sendFailureNotification(List<CliCheckResult> failures, boolean hardFail);

  public abstract void sendChecksSkippedNotification();

  public abstract String replaceQuoteMarkers(String message);

  protected String getDoubleNewLines() {
    return System.lineSeparator() + System.lineSeparator();
  }

  protected Stream<CliCheckResult> getCheckerHardFailures(List<CliCheckResult> results) {
    return results.stream()
        .filter(result -> !result.isSuccessful())
        .filter(CliCheckResult::isHardFail);
  }

  protected String getFormattedNotificationMessage(
      String messageTemplate, String messageKey, String message) {
    MessageFormat messageFormatForTemplate = new MessageFormat(messageTemplate);
    return messageFormatForTemplate.format(ImmutableMap.of(messageKey, message));
  }

  protected String appendHardFailureMessage(boolean hardFail, StringBuilder sb) {
    return hardFail && !Strings.isNullOrEmpty(hardFailureMessage)
        ? sb + getDoubleNewLines() + hardFailureMessage
        : sb.toString();
  }

  protected boolean isNullOrEmpty(List<CliCheckResult> results) {
    return results == null || results.isEmpty();
  }
}
