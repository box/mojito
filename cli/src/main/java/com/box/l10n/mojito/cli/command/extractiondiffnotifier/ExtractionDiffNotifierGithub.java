package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_READY;
import static com.box.l10n.mojito.github.PRLabel.TRANSLATIONS_REQUIRED;
import static com.box.l10n.mojito.github.PRLabel.updatePRLabel;
import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.github.GithubClient;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import org.slf4j.Logger;

public class ExtractionDiffNotifierGithub implements ExtractionDiffNotifier {

  /** logger */
  static Logger logger = getLogger(ExtractionDiffNotifierGithub.class);

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

    // Removing added/removed strings from github notification
    extractionDiffStatistics =
        extractionDiffStatistics
            .withAddedStrings(new ArrayList<>())
            .withRemovedStrings(new ArrayList<>());

    String message = extractionDiffNotifierMessageBuilder.getMessage(extractionDiffStatistics);
    githubClient.addCommentToPR(repository, prNumber, message);

    if (extractionDiffStatistics.getAdded() > 0) {
      // For the initial string addition, the CLI will put the label, but for updates that creates
      // new strings and if the initial string set has been translated, the server will have to
      // change the label since this command won't have enough information to know the current
      // state.
      //
      // The "translation-required" label would eventually be set by the server, but we set it here
      // earlier here to be more reactive and not have to wait for the server to process the branch
      // (which can lag a bit)
      if (githubClient.isLabelAppliedToPR(repository, prNumber, TRANSLATIONS_READY.toString())) {
        logger.debug(
            "'translation-ready' is present, skip setting the 'translation-required' to avoid"
                + " being in an invalid state where the translations are actually ready");
      } else {
        logger.debug("Set 'translation-required' early to avoid lag in the server setting it");
        updatePRLabel(githubClient, repository, prNumber, TRANSLATIONS_REQUIRED);
      }
    } else if (extractionDiffStatistics.getRemoved() > 0
        && githubClient.isLabelAppliedToPR(
            repository, prNumber, TRANSLATIONS_REQUIRED.toString())) {
      logger.debug(
          "Remove 'translations-required' label if it exists since there are no new strings added");
      githubClient.removeLabelFromPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
    }
    return message;
  }

  @Override
  public void sendNoChangesNotification() {
    if (githubClient.isLabelAppliedToPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString())) {
      githubClient.removeLabelFromPR(repository, prNumber, TRANSLATIONS_REQUIRED.toString());
    }
  }
}
