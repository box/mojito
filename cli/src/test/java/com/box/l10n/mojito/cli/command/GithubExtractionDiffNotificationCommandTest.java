package com.box.l10n.mojito.cli.command;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import org.junit.Test;
import org.mockito.Mockito;

public class GithubExtractionDiffNotificationCommandTest extends CLITestBase {

  @Test
  public void getMessageInfo() {
    GithubExtractionDiffNotificationCommand githubExtractionDiffNotificationCommand =
        new GithubExtractionDiffNotificationCommand();
    assertEquals(
        ":information_source: 0 strings removed and 1 string added (from 10 to 11)",
        githubExtractionDiffNotificationCommand.getMessage(
            ExtractionDiffStatistics.builder().added(1).removed(0).base(10).current(11).build()));
  }

  @Test
  public void getMessageWarning() {
    GithubExtractionDiffNotificationCommand githubExtractionDiffNotificationCommand =
        new GithubExtractionDiffNotificationCommand();
    assertEquals(
        ":warning: 10 strings removed and 8 strings added (from 20 to 18)",
        githubExtractionDiffNotificationCommand.getMessage(
            ExtractionDiffStatistics.builder().added(8).removed(10).base(20).current(18).build()));
  }

  @Test
  public void getMessageError() {
    GithubExtractionDiffNotificationCommand githubExtractionDiffNotificationCommand =
        new GithubExtractionDiffNotificationCommand();
    assertEquals(
        ":stop_sign: 200 strings removed and 0 strings added (from 500 to 300)",
        githubExtractionDiffNotificationCommand.getMessage(
            ExtractionDiffStatistics.builder()
                .added(0)
                .removed(200)
                .base(500)
                .current(300)
                .build()));
  }

  @Test
  public void withTemplate() {
    GithubExtractionDiffNotificationCommand githubExtractionDiffNotificationCommand =
        new GithubExtractionDiffNotificationCommand();
    githubExtractionDiffNotificationCommand.messageTemplate =
        "{baseMessage}. Check [[https://build.org/1234|build]].";
    assertEquals(
        ":stop_sign: 200 strings removed and 0 strings added (from 500 to 300). Check [[https://build.org/1234|build]].",
        githubExtractionDiffNotificationCommand.getMessage(
            ExtractionDiffStatistics.builder()
                .added(0)
                .removed(200)
                .base(500)
                .current(300)
                .build()));
  }

  @Test
  public void shouldSendNotification() {
    GithubExtractionDiffNotificationCommand githubExtractionDiffNotificationCommand =
        new GithubExtractionDiffNotificationCommand();
    assertTrue(
        githubExtractionDiffNotificationCommand.shouldSendNotification(
            ExtractionDiffStatistics.builder()
                .added(0)
                .removed(200)
                .base(500)
                .current(300)
                .build()));
  }

  @Test
  public void shouldNotSendNotification() {
    GithubExtractionDiffNotificationCommand githubExtractionDiffNotificationCommand =
        new GithubExtractionDiffNotificationCommand();
    assertFalse(
        githubExtractionDiffNotificationCommand.shouldSendNotification(
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
    GithubExtractionDiffNotificationCommand command =
        l10nJCommander.getCommand(GithubExtractionDiffNotificationCommand.class);
    GithubClients githubClientsFactoryMock = Mockito.mock(GithubClients.class);
    GithubClient githubClientMock = Mockito.mock(GithubClient.class);
    command.githubClientsFactory = githubClientsFactoryMock;
    Mockito.when(githubClientsFactoryMock.getClient(isA(String.class)))
        .thenReturn(githubClientMock);

    l10nJCommander.run(
        "github-extraction-diff-notif",
        "-pr",
        "1",
        "-go",
        "owner",
        "-gr",
        "testRepo",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "-mt",
        "{baseMessage} in pr: ${PR_NUMBER}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.");

    Mockito.verify(githubClientMock, Mockito.times(1))
        .addCommentToPR(
            "testRepo",
            1,
            ":warning: 5 strings removed and 2 strings added (from 10 to 7) in pr: ${PR_NUMBER}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.");
    assertTrue(
        outputCapture
            .toString()
            .contains(
                ":warning: 5 strings removed and 2 strings added (from 10 to 7) in pr: ${PR_NUMBER}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details."));
    checkExpectedGeneratedResources();
  }

  @Test
  public void noNotifications() throws Exception {

    L10nJCommander l10nJCommander = getL10nJCommander();
    GithubExtractionDiffNotificationCommand command =
        l10nJCommander.getCommand(GithubExtractionDiffNotificationCommand.class);
    GithubClients githubClientsFactoryMock = Mockito.mock(GithubClients.class);
    GithubClient githubClientMock = Mockito.mock(GithubClient.class);
    command.githubClientsFactory = githubClientsFactoryMock;
    Mockito.when(githubClientsFactoryMock.getClient(isA(String.class)))
        .thenReturn(githubClientMock);
    command.extractionDiffService = Mockito.mock(ExtractionDiffService.class);
    Mockito.when(command.extractionDiffService.computeExtractionDiffStatistics(any()))
        .thenReturn(ExtractionDiffStatistics.builder().build());

    l10nJCommander.run(
        "github-extraction-diff-notif",
        "-pr",
        "1",
        "-go",
        "owner",
        "-gr",
        "testRepo",
        "-i",
        getTargetTestDir("extractions").getAbsolutePath(),
        "-o",
        getTargetTestDir("extraction-diffs").getAbsolutePath(),
        "-c",
        "source2",
        "-b",
        "source1",
        "-mt",
        "{baseMessage} in pr: ${PR_NUMBER}. Check [[https://build.org/${BUILD_NUMBER}|build]] for extraction details.",
        "-oid",
        "{objectId}");

    Mockito.verify(githubClientMock, Mockito.never())
        .addCommentToPR(anyString(), anyInt(), anyString());
    assertTrue(outputCapture.toString().contains("No need to send notification"));
  }
}
