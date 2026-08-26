package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.entity.RepoType;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Views id, name, and description of an existing repo type. */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-view"},
    commandDescription = "View a repo type")
public class RepoTypeViewCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeViewCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired CommandHelper commandHelper;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("View repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    RepoType repoType = commandHelper.findRepoTypeByName(nameParam);
    String description = repoType.getDescription() != null ? repoType.getDescription() : "";

    consoleWriter
        .newLine()
        .a("Repo type id --> ")
        .fg(Ansi.Color.MAGENTA)
        .a(repoType.getId())
        .println();
    consoleWriter.a("Name --> ").fg(Ansi.Color.MAGENTA).a(repoType.getName()).println();
    consoleWriter.a("Description --> ").fg(Ansi.Color.MAGENTA).a(description).println();
    consoleWriter.println();
  }
}
