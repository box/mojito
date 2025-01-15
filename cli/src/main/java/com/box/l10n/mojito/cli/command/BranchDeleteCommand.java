package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_SHORT;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_SHORT;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_NULL_BRANCH_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_NULL_BRANCH_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_NULL_BRANCH_SHORT;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_TRANSLATED_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_TRANSLATED_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_TRANSLATED_SHORT;
import static java.util.Optional.ofNullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.RepositoryWsApiProxy;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.BranchBranchSummary;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.entity.Repository;
import java.time.ZonedDateTime;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"branch-delete", "bd"},
    commandDescription = "Delete branches")
public class BranchDeleteCommand extends Command {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(BranchDeleteCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired RepositoryWsApiProxy repositoryClient;

  @Autowired AssetClient assetClient;

  @Autowired CommandHelper commandHelper;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.BRANCH_NAME_LONG, Param.BRANCH_NAME_SHORT},
      arity = 1,
      description = Param.BRANCH_NAME_DESCRIPTION)
  String branchName = null;

  @Parameter(
      names = {Param.BRANCH_NAME_REGEX_LONG, Param.BRANCH_NAME_REGEX_SHORT},
      arity = 1,
      description = Param.BRANCH_NAME_REGEX_DESCRIPTION)
  String branchNameRegex = null;

  @Parameter(
      names = {BRANCH_TRANSLATED_LONG, BRANCH_TRANSLATED_SHORT},
      arity = 1,
      description = BRANCH_TRANSLATED_DESCRIPTION)
  Boolean translated = null;

  @Parameter(
      names = {BRANCH_NULL_BRANCH_LONG, BRANCH_NULL_BRANCH_SHORT},
      arity = 0,
      description = BRANCH_NULL_BRANCH_DESCRIPTION)
  boolean includeNullBranch = false;

  @Parameter(
      names = {BRANCH_CREATED_BEFORE_LAST_WEEK_LONG, BRANCH_CREATED_BEFORE_LAST_WEEK_SHORT},
      arity = 0,
      description = BRANCH_CREATED_BEFORE_LAST_WEEK_DESCRIPTION)
  boolean beforeLastWeek;

  @Parameter(
      names = {BRANCH_CREATED_BEFORE, BRANCH_CREATED_BEFORE_SHORT},
      arity = 1,
      description = BRANCH_CREATED_BEFORE_DESCRIPTION)
  String createdBefore = null;

  @Override
  public void execute() throws CommandException {
    consoleWriter
        .newLine()
        .a("Delete branches from repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println();
    Repository repository = commandHelper.findRepositoryByName(repositoryParam);

    ZonedDateTime createdBeforeDateTime =
        this.commandHelper.getLastWeekDateIfTrue(this.beforeLastWeek);
    if (this.createdBefore != null) {
      if (createdBeforeDateTime == null) {
        createdBeforeDateTime = CommandHelper.getCreatedBeforeDateTime(this.createdBefore);
      } else {
        throw new CommandException(
            String.format(
                "Please, pick only one of these parameters: %s or %s",
                BRANCH_CREATED_BEFORE_LAST_WEEK_LONG, BRANCH_CREATED_BEFORE));
      }
    }

    List<BranchBranchSummary> branches =
        repositoryClient.getBranchesOfRepository(
            repository.getId(),
            branchName,
            branchNameRegex,
            false,
            translated,
            includeNullBranch,
            ofNullable(createdBeforeDateTime).map(ZonedDateTime::toOffsetDateTime).orElse(null));

    for (BranchBranchSummary branch : branches) {
      consoleWriter
          .newLine()
          .a("Delete branch: ")
          .fg(Ansi.Color.CYAN)
          .a(branch.getName())
          .println();
      PollableTask pollableTask = repositoryClient.deleteBranch(repository.getId(), branch.getId());
      commandHelper.waitForPollableTask(pollableTask.getId());
    }

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }
}
