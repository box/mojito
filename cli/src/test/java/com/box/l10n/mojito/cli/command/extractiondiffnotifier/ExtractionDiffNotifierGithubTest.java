package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static com.box.l10n.mojito.github.PRLabel.SKIP_TRANSLATIONS_REQUIRED;
import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_READY;
import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_REQUIRED;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.github.GithubClient;
import org.junit.Test;

public class ExtractionDiffNotifierGithubTest {
  private static final String MESSAGE_REGEX =
      ".*[\\d]+ string[s]{0,1} removed and [\\d]+ string[s]{0,1} added.*";

  @Test
  public void sendDiffStatistics() {

    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(false);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(ExtractionDiffStatistics.builder().build());

    assertThat(msg).isEqualTo("ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
    verify(mockGithubClient)
        .updateOrAddCommentToPR(
            repository,
            prNumber,
            "ℹ️ 0 strings removed and 0 strings added (from 0 to 0)",
            MESSAGE_REGEX);
    verify(mockGithubClient, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void testLabelAppliedWhenStringsAdded() {
    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(false);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(
            ExtractionDiffStatistics.builder().added(1).build());

    verify(mockGithubClient, times(1))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void testLabelNotAppliedWhenTranslationReadyLabelPresent() {
    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_READY.toString()))
        .thenReturn(true);

    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(
            ExtractionDiffStatistics.builder().added(1).build());

    verify(mockGithubClient, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void sendNoChangesNotification() {
    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber,
            MESSAGE_REGEX);

    extractionDiffNotifierGithub.sendNoChangesNotification();

    verify(mockGithubClient, times(1))
        .removeLabelFromPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void clearTranslationsRequiredLabelIfStringsOnlyRemoved() {
    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber,
            MESSAGE_REGEX);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(
            ExtractionDiffStatistics.builder().removed(1).build());

    verify(mockGithubClient, times(1))
        .removeLabelFromPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }

  @Test
  public void testSendDiffStatisticsWhenSkipTranslationsRequiredWasApplied() {
    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(false);
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, SKIP_TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);
    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber,
            MESSAGE_REGEX);

    extractionDiffNotifierGithub.sendDiffStatistics(
        ExtractionDiffStatistics.builder().added(1).build());

    verify(mockGithubClient, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());

    mockGithubClient = mock(GithubClient.class);

    when(mockGithubClient.isLabelAppliedToPR(repository, 1, TRANSLATIONS_READY.toString()))
        .thenReturn(true);
    when(mockGithubClient.isLabelAppliedToPR(repository, 1, SKIP_TRANSLATIONS_REQUIRED.toString()))
        .thenReturn(true);

    extractionDiffNotifierGithub.sendDiffStatistics(
        ExtractionDiffStatistics.builder().added(1).build());

    verify(mockGithubClient, times(0))
        .addLabelToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
  }
}
