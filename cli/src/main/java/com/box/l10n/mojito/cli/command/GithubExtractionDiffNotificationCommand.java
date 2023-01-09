package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.extraction.AbstractExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.thirdpartynotification.github.GithubIcon;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to compute extraction diff statistics and optionally notify Github.
 *
 * <p>A notification is sent if the Extraction contains any added or removed text units. The level
 * (icon: info, warning, danger) of the notification will depend on the number of removed text
 * units.
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"github-extraction-diff-notif"},
    commandDescription = "Compute extraction diff statistics and optionally notify Github")
public class GithubExtractionDiffNotificationCommand
    extends AbstractExtractionDiffNotificationCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(GithubExtractionDiffNotificationCommand.class);

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriterAnsiCodeEnabledFalse;

  @Autowired GithubClients githubClientsFactory;

  @Autowired ExtractionDiffService extractionDiffService;

  @Parameter(
      names = {"--pull-request-id", "-pr"},
      arity = 1,
      required = true,
      description = "the pull request number")
  Integer prNumber;

  @Parameter(
      names = {"--github-owner", "-go"},
      arity = 1,
      required = true,
      description = "the Github repository owner name")
  String owner;

  @Parameter(
      names = {"--github-repo", "-gr"},
      arity = 1,
      required = true,
      description = "the Github repository name")
  String repositoryName;

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
  protected void execute() throws CommandException {
    GithubClient github = githubClientsFactory.getClient(owner);
    ExtractionDiffStatistics extractionDiffStatistics =
        getExtractionDiffStatistics(
            extractionDiffService,
            inputDirectoryParam,
            currentExtractionName,
            baseExtractionName,
            outputDirectoryParam,
            extractionDiffName);
    if (shouldSendNotification(extractionDiffStatistics)) {
      String message = getMessage(extractionDiffStatistics);
      consoleWriterAnsiCodeEnabledFalse.a(message).println();
      github.addCommentToPR(repositoryName, prNumber, message);
    } else {
      consoleWriterAnsiCodeEnabledFalse.a("No need to send notification").println();
    }
  }

  @Override
  public boolean shouldShowInCommandList() {
    return false;
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
            .build();
    String msg =
        "{icon} {removedCount, plural, one{# string removed} other{# strings removed}} and {addedCount, plural, one{# string added} other{# strings added}} (from {totalBase} to {totalCurrent})";
    return getFormattedMessage("baseMessage", getBaseMessage(messageParamMap, msg));
  }

  private String getFormattedMessage(String messageKey, String message) {
    MessageFormat messageFormatForTemplate = new MessageFormat(messageTemplate);
    return messageFormatForTemplate.format(ImmutableMap.of(messageKey, message));
  }

  private String getBaseMessage(ImmutableMap<String, Object> arguments, String message) {
    MessageFormat messageFormat = new MessageFormat(message);
    return messageFormat.format(arguments);
  }

  private GithubIcon getIcon(int addedCount, int removedCount) {
    GithubIcon icon = GithubIcon.INFO;

    if (addedCount - removedCount < 0) {
      icon = GithubIcon.WARNING;
    }

    if (removedCount > 20) {
      icon = GithubIcon.STOP;
    }

    return icon;
  }
}
