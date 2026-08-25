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

/** Updates name and/or description of an existing repo type. */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-update"},
    commandDescription = "Updates a repo type")
public class RepoTypeUpdateCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeUpdateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired CommandHelper commandHelper;

  @Autowired RepoTypeClient repoTypeClient;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

  @Parameter(
      names = {Param.REPO_TYPE_NEW_NAME_LONG, Param.REPO_TYPE_NEW_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_NEW_NAME_DESCRIPTION)
  String newNameParam;

  @Parameter(
      names = {Param.REPO_TYPE_DESCRIPTION_LONG, Param.REPO_TYPE_DESCRIPTION_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_DESCRIPTION_DESCRIPTION)
  String descriptionParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Update repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    if (newNameParam == null && descriptionParam == null) {
      throw new CommandException(
          "Must provide at least one of the following options: --new-name, --description");
    }

    RepoType existing = commandHelper.findRepoTypeByName(nameParam);

    try {
      RepoType toUpdate = new RepoType();
      toUpdate.setName(newNameParam);
      toUpdate.setDescription(descriptionParam);
      // Leave integrity checkers unchanged (DTO defaults to empty set, which would clear them)
      toUpdate.setIntegrityCheckers(null);

      RepoType updated = repoTypeClient.updateRepoType(existing.getId(), toUpdate);
      consoleWriter
          .newLine()
          .a("updated --> repo type id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(updated.getId())
          .println();
    } catch (HttpClientErrorException ex) {
      throw CommandHelper.repoTypeClientError(ex);
    }
  }
}
