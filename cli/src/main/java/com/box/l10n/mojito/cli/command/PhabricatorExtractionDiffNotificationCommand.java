package com.box.l10n.mojito.cli.command;

import static com.google.common.base.Strings.nullToEmpty;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.extraction.AbstractExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.phabricator.PhabricatorMessageBuilder;
import com.box.l10n.mojito.thirdpartynotification.phabricator.PhabricatorIcon;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to compute extraction diff statistics and optionally notify Phabricator.
 *
 * <p>A notification is sent if the Extraction contains any added or removed text units. The level
 * (icon: info, warning, danger) of the notification will depend on the number of removed text
 * units.
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"phab-extraction-diff-notif"},
    commandDescription = "Compute extraction diff statistics and optionally notify Phabricator")
public class PhabricatorExtractionDiffNotificationCommand extends Command {
  /** logger */
  static Logger logger =
      LoggerFactory.getLogger(PhabricatorExtractionDiffNotificationCommand.class);

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriterAnsiCodeEnabledFalse;

  @Autowired(required = false)
  DifferentialRevision differentialRevision;

  @Autowired(required = false)
  PhabricatorMessageBuilder phabricatorMessageBuilder;

  @Autowired ExtractionDiffService extractionDiffService;

  @Parameter(
      names = {"--object-id", "-oid"},
      arity = 1,
      required = true,
      description = "the object id of the revision")
  String objectId;

  @Parameter(
      names = {"--message-template", "-mt"},
      arity = 1,
      required = false,
      description =
          "Optional message template to customize the notification message. eg. '{baseName}. Check [[https://build.org/1234|build]].' ")
  String messageTemplate = "{baseMessage}";

  @Parameter(
      names = {"--current", "-c"},
      arity = 1,
      required = true,
      description = ExtractionDiffCommand.CURRENT_EXTRACTION_NAME_DESCRIPTION)
  String currentExtractionName;

  @Parameter(
      names = {"--base", "-b"},
      arity = 1,
      required = true,
      description = ExtractionDiffCommand.BASE_EXTRACTION_NAME_DESCRIPTION)
  String baseExtractionName;

  @Parameter(
      names = {"--name", "-n"},
      arity = 1,
      required = false,
      description = ExtractionDiffCommand.EXCTRACTION_DIFF_NAME_DESCRIPTION)
  String extractionDiffName = null;

  @Parameter(
      names = {"--output-directory", "-o"},
      arity = 1,
      required = false,
      description = ExtractionDiffCommand.OUTPUT_DIRECTORY_DESCRIPTION)
  String outputDirectoryParam = ExtractionDiffPaths.DEFAULT_OUTPUT_DIRECTORY;

  @Parameter(
      names = {"--input-directory", "-i"},
      arity = 1,
      required = false,
      description = ExtractionDiffCommand.INPUT_DIRECTORY_DESCRIPTION)
  String inputDirectoryParam = ExtractionPaths.DEFAULT_OUTPUT_DIRECTORY;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {
    PhabricatorPreconditions.checkNotNull(differentialRevision);

    ExtractionPaths baseExtractionPaths =
        new ExtractionPaths(inputDirectoryParam, baseExtractionName);
    ExtractionPaths currentExtractionPaths =
        new ExtractionPaths(inputDirectoryParam, currentExtractionName);
    ExtractionDiffPaths extractionDiffPaths =
        ExtractionDiffPaths.builder()
            .outputDirectory(outputDirectoryParam)
            .diffExtractionName(extractionDiffName)
            .baseExtractorPaths(baseExtractionPaths)
            .currentExtractorPaths(currentExtractionPaths)
            .build();

    ExtractionDiffStatistics extractionDiffStatistics = null;
    try {
      extractionDiffStatistics =
          extractionDiffService.computeExtractionDiffStatistics(extractionDiffPaths);
    } catch (MissingExtractionDirectoryExcpetion missingExtractionDirectoryExcpetion) {
      throw new CommandException(
          "Can't compute extraction diff statistics", missingExtractionDirectoryExcpetion);
    }

    if (shouldSendNotification(extractionDiffStatistics)) {
      String message = getMessage(extractionDiffStatistics);
      consoleWriterAnsiCodeEnabledFalse.a(message).println();
      differentialRevision.addComment(objectId, message);
    } else {
      consoleWriterAnsiCodeEnabledFalse.a("No need to send notification").println();
    }
  }

  boolean shouldSendNotification(ExtractionDiffStatistics extractionDiffStatistics) {
    return extractionDiffStatistics.getRemoved() > 0 || extractionDiffStatistics.getAdded() > 0;
  }

  String getMessage(AbstractExtractionDiffStatistics extractionDiffStatistics) {

    ImmutableMap<String, Object> messageParamMap =
        ImmutableMap.<String, Object>builder()
            .put(
                "icon",
                getIcon(extractionDiffStatistics.getAdded(), extractionDiffStatistics.getRemoved())
                    .toString())
            .put("addedCount", extractionDiffStatistics.getAdded())
            .put("removedCount", extractionDiffStatistics.getRemoved())
            .put("totalBase", extractionDiffStatistics.getBase())
            .put("totalCurrent", extractionDiffStatistics.getCurrent())
            .put("objectId", nullToEmpty(objectId))
            .build();
    String msg =
        "{icon} {removedCount, plural, one{# string removed} other{# strings removed}} and {addedCount, plural, one{# string added} other{# strings added}} (from {totalBase} to {totalCurrent})";
    return phabricatorMessageBuilder.getFormattedPhabricatorMessage(
        messageTemplate,
        "baseMessage",
        phabricatorMessageBuilder.getBaseMessage(messageParamMap, msg));
  }

  private PhabricatorIcon getIcon(int addedCount, int removedCount) {
    PhabricatorIcon icon = PhabricatorIcon.INFO;

    if (addedCount - removedCount < 0) {
      icon = PhabricatorIcon.WARNING;
    }

    if (removedCount > 20) {
      icon = PhabricatorIcon.STOP;
    }
    return icon;
  }
}
