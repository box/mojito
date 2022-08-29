package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to delete a repository
 *
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-delete"},
    commandDescription = "Deletes a repository")
public class RepoDeleteCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoDeleteCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String nameParam;

  @Autowired RepositoryClient repositoryClient;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Delete repository: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    try {
      repositoryClient.deleteRepositoryByName(nameParam);
      consoleWriter
          .newLine()
          .a("deleted --> repository name: ")
          .fg(Ansi.Color.MAGENTA)
          .a(nameParam)
          .println();
    } catch (RepositoryNotFoundException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
