package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.HttpClientErrorExceptionHelper;
import com.box.l10n.mojito.rest.client.HttpClientErrorJson;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.VirtualAsset;
import com.box.l10n.mojito.rest.client.VirtualAssetClient;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"virtual-asset-create"},
    commandDescription = "Create an asset (with virtual content)")
public class VirtualAssetCreateCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(VirtualAssetCreateCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--path", "-p"},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String pathParam;

  @Autowired VirtualAssetClient virtualAssetClient;

  @Autowired RepositoryClient repositoryClient;

  @Autowired CommandHelper commandHelper;

  @Autowired HttpClientErrorExceptionHelper httpClientErrorExceptionHelper;

  CommandDirectories commandDirectories;

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Create a virtual asset: ")
        .fg(Ansi.Color.CYAN)
        .a(pathParam)
        .reset()
        .a(" in repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    Repository repository = commandHelper.findRepositoryByName(repositoryParam);

    VirtualAsset virtualAsset = new VirtualAsset();
    virtualAsset.setPath(pathParam);
    virtualAsset.setRepositoryId(repository.getId());
    virtualAsset.setDeleted(Boolean.FALSE);

    try {
      consoleWriter.a(" - Create virtual asset: ").fg(Ansi.Color.CYAN).a(pathParam).println();
      virtualAsset = virtualAssetClient.createOrUpdate(virtualAsset);
      consoleWriter.a(" --> asset id: ").fg(Ansi.Color.MAGENTA).a(virtualAsset.getId()).println();
      consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    } catch (HttpClientErrorException hcee) {
      HttpClientErrorJson toHttpClientErrorJson =
          httpClientErrorExceptionHelper.toHttpClientErrorJson(hcee);
      consoleWriter.println().fg(Ansi.Color.RED).a(toHttpClientErrorJson.getMessage()).println(2);
    }
  }
}
