package com.box.l10n.mojito.service.branch.notification;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.branch-notification.notifiers")
public class BranchNotificationMessageSendersConfigurationProperties {

  Map<String, NoopConfigurationProperties> noop = new HashMap<>();
  Map<String, SlackConfigurationProperties> slack = new HashMap<>();

  Map<String, PhabricatorConfigurationProperties> phabricator = new HashMap<>();

  Map<String, GithubConfigurationProperties> github = new HashMap<>();

  public Map<String, NoopConfigurationProperties> getNoop() {
    return noop;
  }

  public void setNoop(Map<String, NoopConfigurationProperties> noop) {
    this.noop = noop;
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

  public Map<String, GithubConfigurationProperties> getGithub() {
    return github;
  }

  public void setGithub(Map<String, GithubConfigurationProperties> github) {
    this.github = github;
  }

  static class NoopConfigurationProperties {

    boolean enabled;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  static class SlackConfigurationProperties {

    String slackClientId;

    MessageBuilderConfigurationProperties messages = new MessageBuilderConfigurationProperties();

    String userEmailPattern;

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

    MessageBuilderConfigurationProperties messages = new MessageBuilderConfigurationProperties();

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

    public MessageBuilderConfigurationProperties getMessages() {
      return messages;
    }

    public void setMessages(MessageBuilderConfigurationProperties messages) {
      this.messages = messages;
    }

    public static class MessageBuilderConfigurationProperties {

      String newNotificationMsgFormat = "{message}{link}\\n\\n{strings}";

      String updatedNotificationMsgFormat = "{message}{link}\\n\\n{strings}";

      String newStrings =
          "We received your strings! "
              + "Please **add screenshots** as soon as possible and **wait for translations** before releasing. ";

      String updatedStrings =
          "Your branch was updated with new strings! "
              + "Please **add screenshots** as soon as possible and **wait for translations** before releasing. ";

      String translationsReady = "Translations are ready!!";

      String screenshotsMissing = "Please provide screenshots to help localization team";

      String noMoreStrings = "The branch was updated and there are no more strings to translate.";

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

  static class GithubConfigurationProperties {
    String owner;

    MessageBuilderConfigurationProperties messages = new MessageBuilderConfigurationProperties();

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public MessageBuilderConfigurationProperties getMessages() {
      return messages;
    }

    public static class MessageBuilderConfigurationProperties {

      String newNotificationMsgFormat = "{message}{link}\\n\\n{strings}";

      String updatedNotificationMsgFormat = "{message}{link}\\n\\n{strings}";

      String newStrings =
          "We received your strings! "
              + "Please **add screenshots** as soon as possible and **wait for translations** before releasing. ";

      String updatedStrings =
          "Your branch was updated with new strings! "
              + "Please **add screenshots** as soon as possible and **wait for translations** before releasing. ";

      String translationsReady = "Translations are ready!!";

      String screenshotsMissing = "Please provide screenshots to help localization team";

      String noMoreStrings = "The branch was updated and there are no more strings to translate.";

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
}
