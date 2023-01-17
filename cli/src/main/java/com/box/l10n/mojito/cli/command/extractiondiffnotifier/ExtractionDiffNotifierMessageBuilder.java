package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.thirdpartynotification.Icons;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.ibm.icu.text.MessageFormat;

public class ExtractionDiffNotifierMessageBuilder {

  public static final String BASE_MESSAGE_TEMPLATE =
      "{icon} {removedCount, plural, one{# string removed} other{# strings removed}} and {addedCount, plural, one{# string added} other{# strings added}} (from {totalBase} to {totalCurrent})";
  public static final MessageFormat BASE_MESSAGE_MESSAGE_FORMAT =
      new MessageFormat(BASE_MESSAGE_TEMPLATE);

  String messageTemplate;

  /**
   * @param messageTemplate can contain placeholders: {baseMessage}, {icon}, {addedCount},
   *     {removedCount}, {totalBase} and {totalCurrent}
   */
  public ExtractionDiffNotifierMessageBuilder(String messageTemplate) {
    this.messageTemplate = Preconditions.checkNotNull(messageTemplate);
  }

  public String getMessage(ExtractionDiffStatistics extractionDiffStatistics) {

    Preconditions.checkNotNull(extractionDiffStatistics);

    MessageFormat messageFormat = new MessageFormat(messageTemplate);

    ImmutableMap<String, Object> messageParamMap =
        new Builder<String, Object>()
            .putAll(getBaseMessageMap(extractionDiffStatistics))
            .put("baseMessage", getBaseMessage(extractionDiffStatistics))
            .build();

    return messageFormat.format(messageParamMap);
  }

  String getBaseMessage(ExtractionDiffStatistics extractionDiffStatistics) {
    return BASE_MESSAGE_MESSAGE_FORMAT.format(getBaseMessageMap(extractionDiffStatistics));
  }

  private ImmutableMap<String, Object> getBaseMessageMap(
      ExtractionDiffStatistics extractionDiffStatistics) {
    ImmutableMap<String, Object> messageParamMap =
        ImmutableMap.<String, Object>builder()
            .put("icon", getIcon(extractionDiffStatistics).toString())
            .put("addedCount", extractionDiffStatistics.getAdded())
            .put("removedCount", extractionDiffStatistics.getRemoved())
            .put("totalBase", extractionDiffStatistics.getBase())
            .put("totalCurrent", extractionDiffStatistics.getCurrent())
            .build();
    return messageParamMap;
  }

  private Icons getIcon(ExtractionDiffStatistics extractionDiffStatistics) {
    Icons icons;
    switch (getMessageSeverityLevel(extractionDiffStatistics)) {
      case WARNING:
        icons = Icons.WARNING;
        break;
      case ERROR:
        icons = Icons.STOP;
        break;
      default:
        icons = Icons.INFO;
    }
    return icons;
  }

  public enum MessageSeverityLevel {
    INFO,
    WARNING,
    ERROR
  }

  public MessageSeverityLevel getMessageSeverityLevel(
      ExtractionDiffStatistics extractionDiffStatistics) {
    MessageSeverityLevel messageSeverityLevel = MessageSeverityLevel.INFO;

    if (extractionDiffStatistics.getAdded() - extractionDiffStatistics.getRemoved() < 0) {
      messageSeverityLevel = MessageSeverityLevel.WARNING;
    }

    if (extractionDiffStatistics.getRemoved() > 20) {
      messageSeverityLevel = MessageSeverityLevel.ERROR;
    }
    return messageSeverityLevel;
  }
}
