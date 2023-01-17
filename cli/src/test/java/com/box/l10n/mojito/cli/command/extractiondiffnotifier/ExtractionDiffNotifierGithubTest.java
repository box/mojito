package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.github.GithubClient;
import org.junit.Test;

public class ExtractionDiffNotifierGithubTest {

  @Test
  public void sendDiffStatistics() {

    GithubClient mockGithubClient = mock(GithubClient.class);

    String repository = "repository";
    int prNumber = 1;

    ExtractionDiffNotifierGithub extractionDiffNotifierGithub =
        new ExtractionDiffNotifierGithub(
            new ExtractionDiffNotifierMessageBuilder("{baseMessage}"),
            mockGithubClient,
            repository,
            prNumber);

    final String msg =
        extractionDiffNotifierGithub.sendDiffStatistics(ExtractionDiffStatistics.builder().build());

    assertThat(msg).isEqualTo("ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
    verify(mockGithubClient)
        .addCommentToPR(
            repository, prNumber, "ℹ️ 0 strings removed and 0 strings added (from 0 to 0)");
  }
}
