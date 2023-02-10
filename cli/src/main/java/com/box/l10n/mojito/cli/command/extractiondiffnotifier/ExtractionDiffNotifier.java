package com.box.l10n.mojito.cli.command.extractiondiffnotifier;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;

public interface ExtractionDiffNotifier {

  /** Sends diff statistic notification and return the text body of the message sent */
  String sendDiffStatistics(ExtractionDiffStatistics extractionDiffStatistics);

  /** Handles no changes notifications */
  void sendNoChangesNotification();
}
