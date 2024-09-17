package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.extraction-diff.notifiers")
public class ExtractionDiffNotifiersConfigurationProperties {

  Map<String, GithubConfigurationProperties> githubs = new HashMap<>();

  Map<String, SlackConfigurationProperties> slacks = new HashMap<>();

  Map<String, PhabricatorConfigurationProperties> phabricators = new HashMap<>();

  public Map<String, GithubConfigurationProperties> getGithub() {
    return githubs;
  }

  public Map<String, SlackConfigurationProperties> getSlack() {
    return slacks;
  }

  public Map<String, PhabricatorConfigurationProperties> getPhabricator() {
    return phabricators;
  }

  static class GithubConfigurationProperties {

    String owner;

    String repository;

    String messageTemplate = "{baseMessage}";

    String messageRegex = ".*[\\d]+ string[s]{0,1} removed and [\\d]+ string[s]{0,1} added.*";

    int prNumber;

    public String getOwner() {
      return owner;
    }

    public void setOwner(String owner) {
      this.owner = owner;
    }

    public String getRepository() {
      return repository;
    }

    public void setRepository(String repository) {
      this.repository = repository;
    }

    public String getMessageTemplate() {
      return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
      this.messageTemplate = messageTemplate;
    }

    public String getMessageRegex() {
      return messageRegex;
    }

    public void setMessageRegex(String messageRegex) {
      this.messageRegex = messageRegex;
    }

    public int getPrNumber() {
      return prNumber;
    }

    public void setPrNumber(int prNumber) {
      this.prNumber = prNumber;
    }
  }

  static class SlackConfigurationProperties {

    String slackClientId;

    String userEmailPattern;

    boolean useDirectMessage = false;

    String messageTemplate = "{baseMessage}";

    String username;

    public String getSlackClientId() {
      return slackClientId;
    }

    public void setSlackClientId(String slackClientId) {
      this.slackClientId = slackClientId;
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

    public String getMessageTemplate() {
      return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
      this.messageTemplate = messageTemplate;
    }

    public String getUsername() {
      return username;
    }

    public void setUsername(String username) {
      this.username = username;
    }
  }

  static class PhabricatorConfigurationProperties {

    String url;

    String token;

    String messageTemplate = "{baseMessage}";

    String objectIdentifier;

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

    public String getMessageTemplate() {
      return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
      this.messageTemplate = messageTemplate;
    }

    public String getObjectIdentifier() {
      return objectIdentifier;
    }

    public void setObjectIdentifier(String objectIdentifier) {
      this.objectIdentifier = objectIdentifier;
    }
  }
}
