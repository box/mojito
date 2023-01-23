package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifier;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierGithub;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierMessageBuilder;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierPhabricator;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifierSlack;
import com.box.l10n.mojito.cli.command.extractiondiffnotifier.ExtractionDiffNotifiers;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.github.GithubClient;
import com.box.l10n.mojito.github.GithubClients;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.slack.SlackClient;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to compute extraction diff statistics and send notifications. Supported notifiers are
 * Github, Phabricator, Slack.
 *
 * <p>A notification is sent if the Extraction contains any added or removed text units. The level
 * (icon: info, warning, danger) of the notification will depend on the number of removed text
 * units.
 *
 * <p>Supersede {@link PhabricatorExtractionDiffNotificationCommand }
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"extraction-diff-notif"},
    commandDescription = "Compute extraction diff statistics and optionally notify Phabricator")
public class ExtractionDiffNotificationCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractionDiffNotificationCommand.class);

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriterAnsiCodeEnabledFalse;

  @Autowired(required = false)
  DifferentialRevision differentialRevision;

  @Autowired GithubClients githubClients;

  @Autowired(required = false)
  SlackClient slackClient;

  @Autowired ExtractionDiffService extractionDiffService;

  @Autowired ExtractionDiffNotifiers extractionDiffNotifiers;

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
      description = ExtractionDiffCommand.EXCTRACTION_DIFF_NAME_DESCRIPTION)
  String extractionDiffName = null;

  @Parameter(
      names = {"--output-directory", "-o"},
      arity = 1,
      description = ExtractionDiffCommand.OUTPUT_DIRECTORY_DESCRIPTION)
  String outputDirectoryParam = ExtractionDiffPaths.DEFAULT_OUTPUT_DIRECTORY;

  @Parameter(
      names = {"--input-directory", "-i"},
      arity = 1,
      description = ExtractionDiffCommand.INPUT_DIRECTORY_DESCRIPTION)
  String inputDirectoryParam = ExtractionPaths.DEFAULT_OUTPUT_DIRECTORY;

  @Parameter(
      names = "--console-message-template",
      arity = 1,
      description = "The template to format the message wrote in the console")
  String consoleMessageTemplate = "{baseMessage}";

  @Parameter(
      names = "--github-owner",
      arity = 1,
      description =
          "The owner to send Github notifications (assume GitClients are configured). If set, it will setup the Github notifier.")
  String githubOwner;

  @Parameter(
      names = "--github-message-template",
      arity = 1,
      description = "The template to format the message sent to Github")
  String githubMessageTemplate = "{baseMessage}";

  @Parameter(
      names = "--github-repository",
      arity = 1,
      description = "The Github repository used to send the notification")
  String githubRepository;

  @Parameter(
      names = "--github-pr-number",
      arity = 1,
      description = "The Github Pull Request number used to send the notification")
  int githubPrNumber;

  @Parameter(
      names = "--slack-username",
      arity = 1,
      description =
          "To username, recepient of the Slack message (assume SlackClient is configured). If set, it will setup the Slack notifier.")
  String slackUsername;

  @Parameter(
      names = "--slack-user-email-pattern",
      arity = 1,
      description = "The user email pattern used to send the Slack notification")
  String slackUserEmailPattern;

  @Parameter(
      names = "--slack-use-direct-message",
      arity = 1,
      description = "To send a direct Slack message")
  boolean slackUseDirectMessage;

  @Parameter(
      names = "--slack-message-template",
      arity = 1,
      description = "The template to format the message sent to Slack")
  String slackMessageTemplate = "{baseMessage}";

  @Parameter(
      names = "--phabricator-object-identifier",
      arity = 1,
      description =
          "The Phabricator 'object identifier' where to send notifications. If set, it will setup the Phabricator notifier (requires a Phabricator client to be configured).")
  String phabricatorObjectIdentifier;

  @Parameter(
      names = "--phabricator-message-template",
      arity = 1,
      description = "The template to format the message sent to Phabricator")
  String phabricatorMessageTemplate = "{baseMessage}";

  @Parameter(
      names = "--notifier-ids",
      variableArity = true,
      description = "List of notifier ids to use (if notifiers are configured externaly)")
  List<String> notifierIds = new ArrayList<>();

  Set<ExtractionDiffNotifier> extractionDiffNotifierInstances;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {

    initNotifiers();

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
      consoleWriterAnsiCodeEnabledFalse
          .a(
              new ExtractionDiffNotifierMessageBuilder(consoleMessageTemplate)
                  .getMessage(extractionDiffStatistics))
          .println();

      for (ExtractionDiffNotifier extractionDiffNotifier : extractionDiffNotifierInstances) {
        String message = extractionDiffNotifier.sendDiffStatistics(extractionDiffStatistics);
      }
    } else {
      consoleWriterAnsiCodeEnabledFalse.a("No need to send notification").println();
    }
  }

  boolean shouldSendNotification(ExtractionDiffStatistics extractionDiffStatistics) {
    return extractionDiffStatistics.getRemoved() > 0 || extractionDiffStatistics.getAdded() > 0;
  }

  private void initNotifiers() {
    extractionDiffNotifierInstances = new HashSet<>();
    if (githubOwner != null) {
      extractionDiffNotifierInstances.add(createGihubNotifier());
    }

    if (phabricatorObjectIdentifier != null) {
      extractionDiffNotifierInstances.add(createPhabricatorNotifier());
    }

    if (slackUsername != null) {
      extractionDiffNotifierInstances.add(createSlackNotifier());
    }

    notifierIds.forEach(
        notifierId -> {
          ExtractionDiffNotifier byId = extractionDiffNotifiers.getById(notifierId);
          if (byId == null) {
            throw new CommandException("No notifier is configured for id: " + byId);
          }
          extractionDiffNotifierInstances.add(byId);
        });
  }

  private ExtractionDiffNotifierSlack createSlackNotifier() {
    return new ExtractionDiffNotifierSlack(
        slackClient,
        slackUserEmailPattern,
        slackUseDirectMessage,
        new ExtractionDiffNotifierMessageBuilder(slackMessageTemplate),
        slackUsername);
  }

  private ExtractionDiffNotifierPhabricator createPhabricatorNotifier() {
    Preconditions.checkNotNull(phabricatorObjectIdentifier);
    PhabricatorPreconditions.checkNotNull(differentialRevision);

    return new ExtractionDiffNotifierPhabricator(
        new ExtractionDiffNotifierMessageBuilder(phabricatorMessageTemplate),
        differentialRevision,
        phabricatorObjectIdentifier);
  }

  private ExtractionDiffNotifierGithub createGihubNotifier() {
    Preconditions.checkNotNull(githubOwner);

    GithubClient githubClient = githubClients.getClient(githubOwner);

    if (githubClient == null) {
      throw new CommandException("Github client missing for owner: " + githubOwner);
    }

    return new ExtractionDiffNotifierGithub(
        new ExtractionDiffNotifierMessageBuilder(githubMessageTemplate),
        githubClient,
        githubRepository,
        githubPrNumber);
  }
}
