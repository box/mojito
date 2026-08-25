package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.RepoTypeClient;
import com.box.l10n.mojito.rest.entity.RepoType;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/** Deletes an existing repo type by name. */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-delete"},
    commandDescription = "Deletes a repo type")
public class RepoTypeDeleteCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeDeleteCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired CommandHelper commandHelper;

  @Autowired RepoTypeClient repoTypeClient;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Delete repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    RepoType existing = commandHelper.findRepoTypeByName(nameParam);
    try {
      repoTypeClient.deleteRepoType(existing.getId());
    } catch (HttpClientErrorException ex) {
      throw CommandHelper.repoTypeClientError(ex);
    }

    consoleWriter
        .newLine()
        .a("deleted --> repo type name: ")
        .fg(Ansi.Color.MAGENTA)
        .a(nameParam)
        .println();
  }
}
