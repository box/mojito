package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.google.common.base.Preconditions;

public class ExtractionDiffNotifierPhabricator implements ExtractionDiffNotifier {

  ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder;

  DifferentialRevision differentialRevision;

  String objectIdentifier;

  public ExtractionDiffNotifierPhabricator(
      ExtractionDiffNotifierMessageBuilder extractionDiffNotifierMessageBuilder,
      DifferentialRevision differentialRevision,
      String objectIdentifier) {
    this.extractionDiffNotifierMessageBuilder =
        Preconditions.checkNotNull(extractionDiffNotifierMessageBuilder);
    this.differentialRevision = Preconditions.checkNotNull(differentialRevision);
    this.objectIdentifier = Preconditions.checkNotNull(objectIdentifier);
  }

  @Override
  public String sendDiffStatistics(ExtractionDiffStatistics extractionDiffStatistics) {
    String message = extractionDiffNotifierMessageBuilder.getMessage(extractionDiffStatistics);
    differentialRevision.addComment(objectIdentifier, message);
    return message;
  }

  @Override
  public void sendNoChangesNotification() {
    // no-op
  }
}
