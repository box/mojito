package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepositoryAiReviewClient;
import com.box.l10n.mojito.rest.entity.PollableTask;
import java.util.List;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to machine review strings in a repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repository-ai-review"},
    commandDescription = "Ai review translated strings in a repository")
public class RepositoryAiReviewCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryAiReviewCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT},
      variableArity = true,
      description =
          "List of locales (bcp47 tags) to review, if not provided review all locales in the repository")
  List<String> locales;

  @Parameter(
      names = {"--source-text-max-count"},
      arity = 1,
      description =
          "Text unit variant max count per locale sent to review (this param is used to avoid "
              + "sending too many strings to MT)")
  int sourceTextMaxCount = 100;

  @Parameter(
      names = {"--text-unit-ids"},
      arity = 1,
      description = "The list of TmTextUnitIds to review")
  List<Long> textUnitIds;

  @Parameter(
      names = {"--use-batch"},
      arity = 1,
      description = "To use the batch API or not")
  boolean useBatch = false;

  @Autowired CommandHelper commandHelper;

  @Autowired RepositoryAiReviewClient repositoryAiReviewClient;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Ai review repository: ")
        .fg(Color.CYAN)
        .a(repositoryParam)
        .reset()
        .a(" for locales: ")
        .fg(Color.CYAN)
        .a(locales == null ? "<all>" : locales.stream().collect(Collectors.joining(", ", "[", "]")))
        .println(2);

    RepositoryAiReviewClient.ProtoAiReviewResponse protoAiTranslateResponse =
        repositoryAiReviewClient.reviewRepository(
            new RepositoryAiReviewClient.ProtoAiReviewRequest(
                repositoryParam, locales, sourceTextMaxCount, textUnitIds, useBatch));

    PollableTask pollableTask = protoAiTranslateResponse.pollableTask();
    commandHelper.waitForPollableTask(pollableTask.getId());
  }
}
