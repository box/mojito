package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.github.GithubClient;
import com.google.common.base.Preconditions;

public class ExtractionDiffNotifierGithub implements ExtractionDiffNotifier {

  ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder;

  GithubClient githubClient;

  String repository;

  String messageTemplate;

  int prNumber;

  public ExtractionDiffNotifierGithub(
      ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder,
      GithubClient githubClient,
      String repository,
      int prNumber) {
    this.extractionDiffNotifierMessageBuilder =
        Preconditions.checkNotNull(extractionDiffNotifierMessageBuilder);
    this.githubClient = Preconditions.checkNotNull(githubClient);
    this.repository = Preconditions.checkNotNull(repository);
    this.prNumber = Preconditions.checkNotNull(prNumber);
  }

  @Override
  public String sendDiffStatistics(ExtractionDiffStatistics extractionDiffStatistics) {

    String message = extractionDiffNotifierMessageBuilder.getMessage(extractionDiffStatistics);
    githubClient.addCommentToPR(repository, prNumber, message);
    return message;
  }
}
