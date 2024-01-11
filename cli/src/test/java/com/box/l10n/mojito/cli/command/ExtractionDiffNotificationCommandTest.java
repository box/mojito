package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierMessageBuilder.getStringsListAsFormattedString;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierGithub;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierSlack;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifiers;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.slack.SlackClient;
import com.box.l10n.mojito.slack.request.Message;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class ExtractionDiffNotificationCommandTest extends CLITestBase {

  @Test
  public void shouldSendNotification() {
    ExtractionDiffNotificationCommand extractionDiffNotificationCommand =
        new ExtractionDiffNotificationCommand();

    assertTrue(
        extractionDiffNotificationCommand.shouldSendNotification(
            ExtractionDiffStatistics.builder()
                .added(0)
                .removed(200)
                .base(500)
                .current(300)
                .build()));
  }

  @Test
  public void shouldNotSendNotification() {
    ExtractionDiffNotificationCommand extractionDiffNotificationCommand =
        new ExtractionDiffNotificationCommand();

    assertFalse(
        extractionDiffNotificationCommand.shouldSendNotification(
            ExtractionDiffStatistics.builder().added(0).removed(0).base(500).current(300).build()));
  }

  @Test
  public void sendNotification() throws Exception {

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source1").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source1",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract",
            "-s",
            getInputResourcesTestDir("source2").getAbsolutePath(),
            "-o",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-n",
            "source2",
            "-fo",
            "sometestoption=value1");

    getL10nJCommander()
        .run(
            "extract-diff",
            "-i",
            getTargetTestDir("extractions").getAbsolutePath(),
            "-o",
            getTargetTestDir("extraction-diffs").getAbsolutePath(),
            "-c",
            "source2",
            "-b",
            "source1");

    L10nJCommander l10nJCommander = getL10nJCommander();
    ExtractionDiffNotificationCommand command =
        l10nJCommander.getCommand(ExtractionDiffNotificationCommand.class);

    DifferentialRevision mockDifferentialRevision = mock(DifferentialRevision.class);
    command.differentialRevision = mockDifferentialRevision;

    SlackClient mockSlackClient = mock(SlackClient.class);
    command.slackClient = mockSlackClient;

    command.githubClients = mock(GithubClients.class);
    GithubClient mockGithubClient = mock(GithubClient.class);
    when(command.githubClients.getClient("testowner1")).thenReturn(mockGithubClient);

    command.extractionDiffNotifiers = mock(ExtractionDiffNotifiers.class);
    ExtractionDiffNotifierSlack mockExtractionDiffNotifierSlack1 =
        mock(ExtractionDiffNotifierSlack.class);
    ExtractionDiffNotifierGithub mockExtractionDiffNotifierGithub1 =
        mock(ExtractionDiffNotifierGithub.class);
    when(command.extractionDiffNotifiers.getById("slack-1"))
        .thenReturn(mockExtractionDiffNotifierSlack1);
    when(command.extractionDiffNotifiers.getById("github-1"))
        .thenReturn(mockExtractionDiffNotifierGithub1);

    l10nJCommander.run(
        "extraction-diff-notif",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "--phabricator-message-template",
        "{baseMessage} in diff: ${DIFF_ID}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.",
        "--phabricator-object-identifier",
        "{objectId}",
        "--slack-username",
        "testusername",
        "--slack-user-email-pattern",
        "{0}@mojito.org",
        "--github-owner",
        "testowner1",
        "--github-repository",
        "testrepository",
        "--github-pr-number",
        "123",
        "--github-message-template",
        "Github -- {baseMessage}",
        "--notifier-ids",
        "slack-1",
        "github-1",
        "--console-message-template",
        "Console -- {baseMessage}");

    String stringsRemoved =
        getStringsListAsFormattedString(
            List.of("1 hour", "1 month", "1 day", "1 hour", "1 month"), "Strings removed:");
    String stringsAdded =
        getStringsListAsFormattedString(List.of("1 hour update", "1 day update"), "Strings added:");

    verify(mockDifferentialRevision, times(1))
        .addComment(
            "{objectId}",
            "⚠️ 5 strings removed and 2 strings added (from 10 to 7)"
                + stringsRemoved
                + stringsAdded
                + " in diff: ${DIFF_ID}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.");
    assertTrue(
        outputCapture
            .toString()
            .contains(
                "Console -- ⚠️ 5 strings removed and 2 strings added (from 10 to 7)"
                    + stringsRemoved
                    + stringsAdded));
    checkExpectedGeneratedResources();

    ArgumentCaptor<Message> slackMessageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
    verify(mockSlackClient).sendInstantMessage(slackMessageArgumentCaptor.capture());
    Assertions.assertThat(slackMessageArgumentCaptor.getValue().getAttachments().get(0).getText())
        .isEqualTo(
            "⚠️ 5 strings removed and 2 strings added (from 10 to 7)"
                + stringsRemoved
                + stringsAdded);

    //  GitHub's notification does not contain the added/removed strings
    verify(mockGithubClient)
        .addCommentToPR(
            "testrepository",
            123,
            "Github -- ⚠️ 5 strings removed and 2 strings added (from 10 to 7)");

    verify(mockExtractionDiffNotifierSlack1).sendDiffStatistics(any());
    verify(mockExtractionDiffNotifierGithub1).sendDiffStatistics(any());
  }

  @Test
  public void noNotifications() throws Exception {

    L10nJCommander l10nJCommander = getL10nJCommander();
    ExtractionDiffNotificationCommand command =
        l10nJCommander.getCommand(ExtractionDiffNotificationCommand.class);

    DifferentialRevision mock = mock(DifferentialRevision.class);
    command.differentialRevision = mock;
    command.extractionDiffService = mock(ExtractionDiffService.class);
    Mockito.when(command.extractionDiffService.computeExtractionDiffStatistics(any()))
        .thenReturn(ExtractionDiffStatistics.builder().build());

    l10nJCommander.run(
        "extraction-diff-notif",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1");

    verify(mock, Mockito.never()).addComment(anyString(), anyString());
    assertTrue(outputCapture.toString().contains("No need to send notification"));
  }
}
