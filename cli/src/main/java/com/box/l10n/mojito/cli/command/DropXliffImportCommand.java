package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.Repository;
import java.nio.file.Path;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to import an XLIFF originating from a Drop but in an independent way. It can be used to
 * import modified XLIFF for drops that are not in the system anymore or when the normal drop import
 * logic is too heavy (eg. no need to import all the files).
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"drop-xliff-import"},
    commandDescription = "Import standalone XLIFFs")
public class DropXliffImportCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropXliffImportCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_DIRECTORY_DESCRIPTION)
  String sourceDirectoryParam;

  @Parameter(
      names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT},
      arity = 1,
      required = false,
      description = Param.TARGET_DIRECTORY_DESCRIPTION)
  String targetDirectoryParam;

  @Parameter(
      names = {Param.DROP_IMPORT_STATUS},
      required = false,
      description = Param.DROP_IMPORT_STATUS_DESCRIPTION,
      converter = ImportDropConfigStatusConverter.class)
  ImportDropConfig.Status importStatusParam = null;

  @Parameter(
      names = {"--import-by-md5"},
      required = false,
      description = "To import using MD5 (only option if translation kit not available)")
  Boolean importByMD5 = false;

  @Autowired CommandHelper commandHelper;

  @Autowired Console console;

  @Autowired DropClient dropClient;

  Repository repository;

  CommandDirectories commandDirectories;

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Import localized XLIFFs to repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    repository = commandHelper.findRepositoryByName(repositoryParam);

    commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

    importXliffs();

    consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
  }

  void importXliffs() throws CommandException {

    List<Path> xliffPaths = commandDirectories.listFilesWithExtensionInSourceDirectory("xliff");

    for (Path xliffPath : xliffPaths) {
      importXliff(xliffPath);
    }
  }

  void importXliff(Path xliffPath) throws CommandException {

    consoleWriter
        .a(" - Uploading: ")
        .fg(Ansi.Color.CYAN)
        .a(commandDirectories.relativizeWithSourceDirectory(xliffPath).toString())
        .print();

    String xliffContent = commandHelper.getFileContent(xliffPath);

    String importedXliff =
        dropClient.importXiff(xliffContent, repository.getId(), !importByMD5, importStatusParam);

    Path outputPath =
        commandDirectories.resolveWithTargetDirectoryAndCreateParentDirectories(xliffPath);

    commandHelper.writeFileContent(importedXliff, outputPath);

    consoleWriter.a(" ");

    if (mustBeReviewed(importedXliff)) {
      consoleWriter.fg(Ansi.Color.RED).a("[MUST REVIEW]");
    } else {
      consoleWriter.fg(Ansi.Color.GREEN).a("[OK]");
    }

    consoleWriter.println();
  }

  /**
   * Indicates if an import must be reviewed by looking at comments in the XLIFF in a very
   * simplistic fashion.
   *
   * @param xliffContent
   * @return true if the import must be reviewed
   */
  boolean mustBeReviewed(String xliffContent) {
    return xliffContent.contains("MUST REVIEW");
  }
}
