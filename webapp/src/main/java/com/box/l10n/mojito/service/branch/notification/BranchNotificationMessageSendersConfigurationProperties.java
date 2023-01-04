package com.box.l10n.mojito.service.branch.notification;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties("l10n.branch-notification.notifiers")
public class BranchNotificationMessageSendersConfigurationProperties {

  Map<String, GithubConfigurationProperties> github = new HashMap<>();
  Map<String, SlackConfigurationProperties> slack = new HashMap<>();
  Map<String, PhabricatorConfigurationProperties> phabricator = new HashMap<>();
  Map<String, NoopConfigurationProperties> noop = new HashMap<>();

  public Map<String, GithubConfigurationProperties> getGithub() {
    return github;
  }

  public void setGithub(Map<String, GithubConfigurationProperties> github) {
    this.github = github;
  }

  public Map<String, SlackConfigurationProperties> getSlack() {
    return slack;
  }

  public void setSlack(Map<String, SlackConfigurationProperties> slack) {
    this.slack = slack;
  }

  public Map<String, PhabricatorConfigurationProperties> getPhabricator() {
    return phabricator;
  }

  public void setPhabricator(Map<String, PhabricatorConfigurationProperties> phabricator) {
    this.phabricator = phabricator;
  }

  public Map<String, NoopConfigurationProperties> getNoop() {
    return noop;
  }

  public void setNoop(Map<String, NoopConfigurationProperties> noop) {
    this.noop = noop;
  }

  // TODO(jean) not sure that works
  @Validated
  static class SlackConfigurationProperties {
    String slackClientId;
    MessageBuilderConfigurationProperties messages;

    // TODO(jean) this used to be mandatory, how do you make mandatory in @ConfigurationProperties
    @NonNull String userEmailPattern;
    boolean useDirectMessage = false;

    public String getSlackClientId() {
      return slackClientId;
    }

    public void setSlackClientId(String slackClientId) {
      this.slackClientId = slackClientId;
    }

    public MessageBuilderConfigurationProperties getMessages() {
      return messages;
    }

    public void setMessages(MessageBuilderConfigurationProperties messages) {
      this.messages = messages;
    }

    public String getUserEmailPattern() {
      return userEmailPattern;
    }

    public void setUserEmailPattern(String userEmailPattern) {
      this.userEmailPattern = userEmailPattern;
    }

    public boolean isUseDirectMessage() {
      return useDirectMessage;
    }

    public void setUseDirectMessage(boolean useDirectMessage) {
      this.useDirectMessage = useDirectMessage;
    }

    public static class MessageBuilderConfigurationProperties {

      String newStrings =
          "We received your strings! Please *add screenshots* as soon as possible and *wait for translations* before releasing.";

      String updatedStrings =
          "Your branch was updated with new strings! "
              + "Please *add screenshots* as soon as possible and *wait for translations* before releasing.";

      String translationsReady = "Translations are ready !! :party:";

      String screenshotsMissing =
          ":warning: Please provide screenshots to help localization team :warning:";

      String noMoreStrings = "The branch was updated and there are no more strings to translate.";

      public String getNewStrings() {
        return newStrings;
      }

      public void setNewStrings(String newStrings) {
        this.newStrings = newStrings;
      }

      public String getUpdatedStrings() {
        return updatedStrings;
      }

      public void setUpdatedStrings(String updatedStrings) {
        this.updatedStrings = updatedStrings;
      }

      public String getTranslationsReady() {
        return translationsReady;
      }

      public void setTranslationsReady(String translationsReady) {
        this.translationsReady = translationsReady;
      }

      public String getScreenshotsMissing() {
        return screenshotsMissing;
      }

      public void setScreenshotsMissing(String screenshotsMissing) {
        this.screenshotsMissing = screenshotsMissing;
      }

      public String getNoMoreStrings() {
        return noMoreStrings;
      }

      public void setNoMoreStrings(String noMoreStrings) {
        this.noMoreStrings = noMoreStrings;
      }
    }
  }

  static class PhabricatorConfigurationProperties {

    String url;
    String token;
    String reviewer;
    boolean blockingReview = true;

    MessageBuilderConfigurationProperties messageBuilderConfigurationProperties;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getToken() {
      return token;
    }

    public void setToken(String token) {
      this.token = token;
    }

    public String getReviewer() {
      return reviewer;
    }

    public void setReviewer(String reviewer) {
      this.reviewer = reviewer;
    }

    public boolean isBlockingReview() {
      return blockingReview;
    }

    public void setBlockingReview(boolean blockingReview) {
      this.blockingReview = blockingReview;
    }

    public MessageBuilderConfigurationProperties getMessageBuilderConfigurationProperties() {
      return messageBuilderConfigurationProperties;
    }

    public void setMessageBuilderConfigurationProperties(
        MessageBuilderConfigurationProperties messageBuilderConfigurationProperties) {
      this.messageBuilderConfigurationProperties = messageBuilderConfigurationProperties;
    }

    public static class MessageBuilderConfigurationProperties {

      String newNotificationMsgFormat = "{message}{link}\\n\\n{strings}";

      String updatedNotificationMsgFormat = "{message}{link}\\n\\n{strings}";

      String newNotificationMsg =
          "We received your strings! "
              + "Please **add screenshots** as soon as possible and **wait for translations** before releasing. ";

      String updatedNotificationMsg =
          "Your branch was updated with new strings! "
              + "Please **add screenshots** as soon as possible and **wait for translations** before releasing. ";

      String noMoreStringsMsg =
          "The branch was updated and there are no more strings to translate.";

      String translationsReadyMsg = "Translations are ready!!";

      String screenshotsMissingMsg = "Please provide screenshots to help localization team";

      public String getNewNotificationMsgFormat() {
        return newNotificationMsgFormat;
      }

      public void setNewNotificationMsgFormat(String newNotificationMsgFormat) {
        this.newNotificationMsgFormat = newNotificationMsgFormat;
      }

      public String getUpdatedNotificationMsgFormat() {
        return updatedNotificationMsgFormat;
      }

      public void setUpdatedNotificationMsgFormat(String updatedNotificationMsgFormat) {
        this.updatedNotificationMsgFormat = updatedNotificationMsgFormat;
      }

      public String getNewNotificationMsg() {
        return newNotificationMsg;
      }

      public void setNewNotificationMsg(String newNotificationMsg) {
        this.newNotificationMsg = newNotificationMsg;
      }

      public String getUpdatedNotificationMsg() {
        return updatedNotificationMsg;
      }

      public void setUpdatedNotificationMsg(String updatedNotificationMsg) {
        this.updatedNotificationMsg = updatedNotificationMsg;
      }

      public String getNoMoreStringsMsg() {
        return noMoreStringsMsg;
      }

      public void setNoMoreStringsMsg(String noMoreStringsMsg) {
        this.noMoreStringsMsg = noMoreStringsMsg;
      }

      public String getTranslationsReadyMsg() {
        return translationsReadyMsg;
      }

      public void setTranslationsReadyMsg(String translationsReadyMsg) {
        this.translationsReadyMsg = translationsReadyMsg;
      }

      public String getScreenshotsMissingMsg() {
        return screenshotsMissingMsg;
      }

      public void setScreenshotsMissingMsg(String screenshotsMissingMsg) {
        this.screenshotsMissingMsg = screenshotsMissingMsg;
      }
    }
  }

  static class GithubConfigurationProperties {

    // TODO(jean) it migth make more to look up by "id" vs "owner" to have all implementation
    // with the same logic, instead of having logical keys that are implementation specific
    /**
     * The owner will be used to lookup the right client to use in the {@link
     * com.box.l10n.mojito.github.GithubClientsFactory}
     */
    String owner;

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }
  }

  static class NoopConfigurationProperties {
    String attr1;

    public String getAttr1() {
      return attr1;
    }

    public void setAttr1(String attr1) {
      this.attr1 = attr1;
    }
  }
}
