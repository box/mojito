package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryException;

public abstract class AbstractExtractionDiffNotificationCommand extends Command {

  public ExtractionDiffStatistics getExtractionDiffStatistics(
      ExtractionDiffService extractionDiffService,
      String inputDirectory,
      String currentExtractionName,
      String baseExtractionName,
      String outputDirectory,
      String extractionDiffName) {
    ExtractionPaths baseExtractionPaths = new ExtractionPaths(inputDirectory, baseExtractionName);
    ExtractionPaths currentExtractionPaths =
        new ExtractionPaths(inputDirectory, currentExtractionName);
    ExtractionDiffPaths extractionDiffPaths =
        ExtractionDiffPaths.builder()
            .outputDirectory(outputDirectory)
            .diffExtractionName(extractionDiffName)
            .baseExtractorPaths(baseExtractionPaths)
            .currentExtractorPaths(currentExtractionPaths)
            .build();

    ExtractionDiffStatistics extractionDiffStatistics = null;
    try {
      extractionDiffStatistics =
          extractionDiffService.computeExtractionDiffStatistics(extractionDiffPaths);
    } catch (MissingExtractionDirectoryException missingExtractionDirectoryException) {
      throw new CommandException(
          "Can't compute extraction diff statistics", missingExtractionDirectoryException);
    }

    return extractionDiffStatistics;
  }

  public boolean shouldSendNotification(ExtractionDiffStatistics extractionDiffStatistics) {
    return extractionDiffStatistics.getRemoved() > 0 || extractionDiffStatistics.getAdded() > 0;
  }
}
