package com.box.l10n.mojito.service.branch.notification.phabricator;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.service.branch.BranchUrlBuilder;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;

public class BranchNotificationMessageBuilderPhabricator {

  /** logger */
  static Logger logger = getLogger(BranchNotificationMessageBuilderPhabricator.class);

  BranchUrlBuilder branchUrlBuilder;

  String newNotificationMsgFormat;

  String updatedNotificationMsgFormat;

  String newNotificationMsg;

  String updatedNotificationMsg;

  String noMoreStringsMsg;

  String translationsReadyMsg;

  String screenshotsMissingMsg;

  public BranchNotificationMessageBuilderPhabricator(
      BranchUrlBuilder branchUrlBuilder,
      String newNotificationMsgFormat,
      String updatedNotificationMsgFormat,
      String newNotificationMsg,
      String updatedNotificationMsg,
      String noMoreStringsMsg,
      String translationsReadyMsg,
      String screenshotsMissingMsg) {
    this.branchUrlBuilder = branchUrlBuilder;
    this.newNotificationMsgFormat = newNotificationMsgFormat;
    this.updatedNotificationMsgFormat = updatedNotificationMsgFormat;
    this.newNotificationMsg = newNotificationMsg;
    this.updatedNotificationMsg = updatedNotificationMsg;
    this.noMoreStringsMsg = noMoreStringsMsg;
    this.translationsReadyMsg = translationsReadyMsg;
    this.screenshotsMissingMsg = screenshotsMissingMsg;
  }

  public String getNewMessage(String branchName, List<String> sourceStrings) {
    MessageFormat messageFormat = new MessageFormat(newNotificationMsgFormat);
    ImmutableMap<String, Object> messageParamMap =
        ImmutableMap.<String, Object>builder()
            .put("message", newNotificationMsg)
            .put("link", getLinkGoToMojito(branchName))
            .put("strings", getFormattedSourceStrings(sourceStrings))
            .build();
    return messageFormat.format(messageParamMap);
  }

  public String getUpdatedMessage(String branchName, List<String> sourceStrings) {

    String msg = null;

    MessageFormat messageFormat = new MessageFormat(updatedNotificationMsgFormat);
    ImmutableMap<String, Object> messageParamMap;
    if (sourceStrings.isEmpty()) {
      messageParamMap =
          ImmutableMap.<String, Object>builder().put("message", noMoreStringsMsg).build();
    } else {
      messageParamMap =
          ImmutableMap.<String, Object>builder()
              .put("message", updatedNotificationMsg)
              .put("link", getLinkGoToMojito(branchName))
              .put("strings", getFormattedSourceStrings(sourceStrings))
              .build();
    }
    return messageFormat.format(messageParamMap);
  }

  public String getNoMoreStringsMessage() {
    return noMoreStringsMsg;
  }

  public String getTranslatedMessage() {
    return translationsReadyMsg;
  }

  public String getScreenshotMissingMessage() {
    return screenshotsMissingMsg;
  }

  String getFormattedSourceStrings(List<String> sourceStrings) {
    return "**Strings:**\n"
        + sourceStrings.stream().map(t -> " - " + t).collect(Collectors.joining("\n"));
  }

  String getLinkGoToMojito(String branchName) {
    return "[→ Go to Mojito](" + branchUrlBuilder.getBranchDashboardUrl(branchName) + ")";
  }
}
