package com.box.l10n.mojito.cli.command;

import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_SHORT;
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
import com.box.l10n.mojito.cli.model.RepositoryRepository;
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
    commandNames = {"branch-view", "bw"},
    commandDescription = "View Branches")
public class BranchViewCommand extends Command {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(BranchViewCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired RepositoryWsApiProxy repositoryClient;

  @Autowired CommandHelper commandHelper;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--deleted", "-d"},
      arity = 1,
      description = "To show deleted branches")
  Boolean deleted = null;

  @Parameter(
      names = {BRANCH_TRANSLATED_LONG, BRANCH_TRANSLATED_SHORT},
      arity = 1,
      description = BRANCH_TRANSLATED_DESCRIPTION)
  Boolean translated = null;

  @Parameter(
      names = {Param.BRANCH_NAME_REGEX_LONG, Param.BRANCH_NAME_REGEX_SHORT},
      arity = 1,
      description = Param.BRANCH_NAME_REGEX_DESCRIPTION)
  String branchNameRegex = null;

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

  @Override
  public void execute() throws CommandException {
    consoleWriter
        .newLine()
        .a("Branches in repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println();
    RepositoryRepository repository = commandHelper.findRepositoryByName(repositoryParam);

    List<BranchBranchSummary> branches =
        repositoryClient.getBranchesOfRepository(
            repository.getId(),
            null,
            branchNameRegex,
            deleted,
            translated,
            includeNullBranch,
            ofNullable(commandHelper.getLastWeekDateIfTrue(beforeLastWeek))
                .map(ZonedDateTime::toOffsetDateTime)
                .orElse(null));

    for (BranchBranchSummary branch : branches) {
      consoleWriter
          .newLine()
          .a(" - ")
          .fg(Ansi.Color.CYAN)
          .a(branch.getName())
          .reset()
          .a(" (" + branch.getId() + ") ");
      if (branch.isDeleted()) {
        consoleWriter.fg(Ansi.Color.MAGENTA).a(" deleted").reset();
      }
    }

    consoleWriter.println(2);
  }
}
