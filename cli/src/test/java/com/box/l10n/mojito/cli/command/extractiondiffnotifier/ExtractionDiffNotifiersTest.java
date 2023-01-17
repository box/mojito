package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.SlackClients;
import org.junit.Test;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class ExtractionDiffNotifiersTest {

  @Test
  public void slack() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.slack.slack-1.slackClientId=slackClientId1",
            "l10n.extraction-diff.notifiers.slack.slack-1.userEmailPattern={0}@mojito.org");

    final SlackClients slackClients = mock(SlackClients.class);
    final SlackClient slackClient = mock(SlackClient.class);
    when(slackClients.getById("slackClientId1")).thenReturn(slackClient);

    ExtractionDiffNotifiers extractionDiffNotifiers =
        new ExtractionDiffNotifiers(config, null, slackClients);
    assertThat(extractionDiffNotifiers.getById("slack-1")).isNotNull();

    verify(slackClients, times(1)).getById("slackClientId1");
  }

  @Test
  public void slackWithMessageOverride() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.slack.slack-1.slackClientId=slackClientId1",
            "l10n.extraction-diff.notifiers.slack.slack-1.userEmailPattern={0}@mojito.org",
            "l10n.extraction-diff.notifiers.slack.slack-1.message-template=Slack -- {baseMessage}");

    final SlackClients slackClients = mock(SlackClients.class);
    final SlackClient slackClient = mock(SlackClient.class);
    when(slackClients.getById("slackClientId1")).thenReturn(slackClient);

    ExtractionDiffNotifiers extractionDiffNotifiers =
        new ExtractionDiffNotifiers(config, null, slackClients);
    assertThat(extractionDiffNotifiers.getById("slack-1")).isNotNull();

    verify(slackClients, times(1)).getById("slackClientId1");
  }

  @Test
  public void slackInvalidId() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.slack.badid-1.slackClientId=slackClientId1");
    assertThatThrownBy(() -> new ExtractionDiffNotifiers(config, null, null))
        .hasMessage("name must start with prefix: slack-");
  }

  @Test
  public void phabricator() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.phabricator.phabricator-1.url=https://phabricator.pinadmin.com",
            "l10n.extraction-diff.notifiers.phabricator.phabricator-1.token=token-value",
            "l10n.extraction-diff.notifiers.phabricator.phabricator-1.object-identifier=oidtest");
    ExtractionDiffNotifiers extractionDiffNotifiers =
        new ExtractionDiffNotifiers(config, null, null);
    assertThat(extractionDiffNotifiers.getById("phabricator-1")).isNotNull();
  }

  @Test
  public void phabricatorInvalidId() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.phabricator.badid-1.url=false");
    assertThatThrownBy(() -> new ExtractionDiffNotifiers(config, null, null))
        .hasMessage("name must start with prefix: phabricator-");
  }

  @Test
  public void github() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.github.github-1.owner=owner1",
            "l10n.extraction-diff.notifiers.github.github-1.repository=testrepository",
            "l10n.extraction-diff.notifiers.github.github-1.pr-number=123");
    GithubClients githubClients = mock(GithubClients.class);
    when(githubClients.getClient("owner1")).thenReturn(mock(GithubClient.class));
    ExtractionDiffNotifiers extractionDiffNotifiers =
        new ExtractionDiffNotifiers(config, githubClients, null);
    assertThat(extractionDiffNotifiers.getById("github-1")).isNotNull();
    verify(githubClients, times(1)).getClient("owner1");
  }

  @Test
  public void githubInvalidId() {
    ExtractionDiffNotifiersConfigurationProperties config =
        getTestExtractionDiffNotifiersConfigurationProperties(
            "l10n.extraction-diff.notifiers.github.badid-1.owner=owner1");
    assertThatThrownBy(() -> new ExtractionDiffNotifiers(config, null, null))
        .hasMessage("name must start with prefix: github-");
  }

  private static ExtractionDiffNotifiersConfigurationProperties
      getTestExtractionDiffNotifiersConfigurationProperties(String... pairs) {
    AnnotationConfigApplicationContext annotationConfigApplicationContext =
        new AnnotationConfigApplicationContext();
    annotationConfigApplicationContext.register(
        ExtractionDiffNotifiersConfigurationProperties.class,
        ConfigurationPropertiesAutoConfiguration.class);
    TestPropertyValues.of(pairs).applyTo(annotationConfigApplicationContext);
    annotationConfigApplicationContext.refresh();
    ExtractionDiffNotifiersConfigurationProperties config =
        annotationConfigApplicationContext.getBean(
            ExtractionDiffNotifiersConfigurationProperties.class);
    return config;
  }
}
