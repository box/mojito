package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.AssetClient;
import com.box.l10n.mojito.apiclient.exception.PollableTaskException;
import com.box.l10n.mojito.apiclient.model.AssetAssetSummary;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.apiclient.model.XliffExportBody;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.google.common.base.MoreObjects;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to exportAssetAsXLIFF a repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"tm-export"},
    commandDescription = "Export a repository TM into XLIFF files")
public class TMExportCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TMExportCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.EXPORT_LOCALES_LONG, Param.EXPORT_LOCALES_SHORT},
      arity = 1,
      required = false,
      description = Param.EXPORT_LOCALES_DESCRIPTION)
  List<String> bcp47tagsParam;

  @Parameter(
      names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT},
      arity = 1,
      required = false,
      description = Param.TARGET_DIRECTORY_DESCRIPTION)
  String targetDirectoryParam;

  @Parameter(
      names = {"--target-basename"},
      arity = 1,
      required = false,
      description = "basename of the generated xliff")
  String targetBasenameParam;

  @Autowired CommandHelper commandHelper;

  @Autowired AssetClient assetClient;

  CommandDirectories commandDirectories;

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Export TM for repository: ")
        .fg(Color.CYAN)
        .a(repositoryParam)
        .println(2);

    commandDirectories = new CommandDirectories(null, targetDirectoryParam);

    logger.debug("Initialize targetBasename (use repository if no target bases name is specified)");
    targetBasenameParam = MoreObjects.firstNonNull(targetBasenameParam, repositoryParam);

    RepositoryRepository repository = commandHelper.findRepositoryByName(repositoryParam);

    List<AssetAssetSummary> assets =
        assetClient.getAssets(repository.getId(), null, null, null, null);

    List<RepositoryLocaleRepository> repositoryLocales = repository.getRepositoryLocales();

    long assetNumber = 0;

    for (AssetAssetSummary asset : assets) {
      assetNumber++;

      consoleWriter.newLine().a("Asset: ").fg(Color.CYAN).a(asset.getPath()).println();

      for (RepositoryLocaleRepository repositoryLocale : repositoryLocales) {

        String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();

        if (bcp47tagsParam == null || bcp47tagsParam.contains(bcp47Tag)) {
          consoleWriter.a("Exporting: ").fg(Color.CYAN).a(bcp47Tag).print();

          XliffExportBody exportBody = new XliffExportBody();
          XliffExportBody xliffExport =
              assetClient.xliffExportAsync(exportBody, bcp47Tag, asset.getId());
          Long pollableTaskId = xliffExport.getPollableTask().getId();

          try {
            commandHelper.waitForPollableTask(pollableTaskId);
          } catch (PollableTaskException e) {
            throw new CommandException(e.getMessage(), e.getCause());
          }

          Path exportFile = getExportFile(repositoryLocale, assetNumber);
          String export =
              assetClient.xliffExport(asset.getId(), xliffExport.getTmXliffId()).getContent();
          commandHelper.writeFileContent(export, exportFile);

          consoleWriter.a(" --> ").fg(Color.MAGENTA).a(exportFile.toString()).println();
        }
      }
    }

    consoleWriter.fg(Color.GREEN).newLine().a("Finished").println(2);
  }

  /**
   * Gets the file that will contain the exported content for a given locale tag and asset number.
   *
   * @param repositoryLocale contains the locale that needs to be exported
   * @param assetNumber the asset number
   * @return the export file
   */
  private Path getExportFile(RepositoryLocaleRepository repositoryLocale, long assetNumber)
      throws CommandException {
    String filename;

    if (repositoryLocale.getParentLocale() == null) {
      filename = targetBasenameParam + "-" + assetNumber + ".xliff";
    } else {
      filename =
          targetBasenameParam
              + "-"
              + assetNumber
              + "_"
              + repositoryLocale.getLocale().getBcp47Tag()
              + ".xliff";
    }

    return commandDirectories.resolveWithTargetDirectoryAndCreateParentDirectories(
        Paths.get(filename));
  }
}
