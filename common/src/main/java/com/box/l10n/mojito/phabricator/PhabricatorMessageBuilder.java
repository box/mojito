package com.box.l10n.mojito.phabricator;

import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;

public class PhabricatorMessageBuilder {

  public String getFormattedPhabricatorMessage(
      String messageTemplate, String messageKey, String message) {
    MessageFormat messageFormatForTemplate = new MessageFormat(messageTemplate);
    return messageFormatForTemplate.format(ImmutableMap.of(messageKey, message));
  }

  public String getBaseMessage(ImmutableMap<String, Object> arguments, String message) {
    MessageFormat messageFormat = new MessageFormat(message);
    return messageFormat.format(arguments);
  }
}
