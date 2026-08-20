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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/** Creates a repo type with a name and optional description. */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-type-create"},
    commandDescription = "Creates a repo type")
public class RepoTypeCreateCommand extends Command {

  static Logger logger = LoggerFactory.getLogger(RepoTypeCreateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Autowired RepoTypeClient repoTypeClient;

  @Parameter(
      names = {Param.REPO_TYPE_NAME_LONG, Param.REPO_TYPE_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.REPO_TYPE_NAME_DESCRIPTION)
  String nameParam;

  @Parameter(
      names = {Param.REPO_TYPE_DESCRIPTION_LONG, Param.REPO_TYPE_DESCRIPTION_SHORT},
      arity = 1,
      required = false,
      description = Param.REPO_TYPE_DESCRIPTION_DESCRIPTION)
  String descriptionParam;

  @Override
  protected void execute() throws CommandException {
    consoleWriter.a("Create repo type: ").fg(Ansi.Color.CYAN).a(nameParam).println();

    try {
      RepoType toCreate = new RepoType();
      toCreate.setName(nameParam);
      toCreate.setDescription(descriptionParam);

      RepoType created = repoTypeClient.createRepoType(toCreate);
      consoleWriter
          .newLine()
          .a("created --> repo type id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(created.getId())
          .println();
    } catch (HttpClientErrorException ex) {
      if (ex.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new CommandException("Repo type with name [" + nameParam + "] already exists", ex);
      }
      throw ex;
    }
  }
}
