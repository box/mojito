package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.XliffFileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.entity.Repository;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"tm-import"},
    commandDescription = "Import assets into the TMS")
public class TMImportCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TMImportCommand.class);

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
      names = {"--origin-override"},
      arity = 1,
      required = false,
      description = "To override the origin attribute in the XLIFF")
  String originOverrideParam;

  @Parameter(
      names = {"--update-tm", "-u"},
      required = false,
      description =
          "To update the TM. By default the import assumes that the TM is empty (optimized for faster import)")
  Boolean updateTMParam = false;

  @Parameter(
      names = {"--skip-source"},
      required = false,
      description = "Skip the source file import")
  Boolean skipSourceImportParam = false;

  @Autowired AssetClient assetClient;

  @Autowired RepositoryClient repositoryClient;

  @Autowired CommandHelper commandHelper;

  Repository repository;

  CommandDirectories commandDirectories;

  List<Map<String, Object>> imported;

  @Override
  public void execute() throws CommandException {

    if (!isJsonOutput()) {
      consoleWriter
          .newLine()
          .a("Start importing for TM: ")
          .fg(Ansi.Color.CYAN)
          .a(repositoryParam)
          .println(2);
    }

    repository = commandHelper.findRepositoryByName(repositoryParam);
    commandDirectories = new CommandDirectories(sourceDirectoryParam);
    imported = new ArrayList<>();

    FileType xliffFileType = new XliffFileType();

    if (!skipSourceImportParam) {
      for (FileMatch sourceFileMatch :
          commandHelper.getSourceFileMatches(
              commandDirectories, Arrays.asList(xliffFileType), null, null, null, null)) {
        doImportFileMatch(sourceFileMatch, "source");
      }
    }

    for (FileMatch targetFileMatch :
        commandHelper.getTargetFileMatches(
            commandDirectories, Arrays.asList(xliffFileType), null, null, null, null)) {
      doImportFileMatch(targetFileMatch, "target");
    }

    if (isJsonOutput()) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("repository", repositoryParam);
      data.put("updateTm", Boolean.TRUE.equals(updateTMParam));
      data.put("skipSource", Boolean.TRUE.equals(skipSourceImportParam));
      data.put("imported", imported);
      writeJsonSuccess(data);
    } else {
      consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }
  }

  /**
   * @param fileMatch
   * @param role source or target file role for JSON reporting
   * @throws CommandException
   */
  protected void doImportFileMatch(FileMatch fileMatch, String role) throws CommandException {
    try {
      Path relativeFilePath = commandDirectories.relativizeWithUserDirectory(fileMatch.getPath());

      if (!isJsonOutput()) {
        consoleWriter
            .a(" - Importing file: ")
            .fg(Ansi.Color.MAGENTA)
            .a(relativeFilePath.toString())
            .fg(Ansi.Color.YELLOW)
            .a(" Running")
            .println();
      }

      repositoryClient.importRepository(
          repository.getId(), getFileContent(fileMatch), updateTMParam);

      Map<String, Object> importedFile = new LinkedHashMap<>();
      importedFile.put("file", relativeFilePath.toString());
      importedFile.put("role", role);
      imported.add(importedFile);

      if (!isJsonOutput()) {
        consoleWriter.erasePreviouslyPrintedLines();
        consoleWriter
            .a(" - Importing file: ")
            .fg(Ansi.Color.MAGENTA)
            .a(relativeFilePath.toString())
            .fg(Ansi.Color.GREEN)
            .a(" Done")
            .println();
      }

    } catch (ResourceNotCreatedException e) {
      throw new CommandException(
          "Error importing ["
              + fileMatch.getPath().toString()
              + "] into repo ["
              + repositoryParam
              + "]",
          e);
    }
  }

  private String getFileContent(FileMatch fileMatch) throws CommandException {

    String fileContent = commandHelper.getFileContent(fileMatch.getPath());

    if (originOverrideParam != null) {
      fileContent =
          fileContent.replaceFirst(
              "file.*?original=\"(.*?)\"", "file original=\"" + originOverrideParam + "\"");
    }

    return fileContent;
  }
}
