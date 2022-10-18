package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionService;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import java.util.ArrayList;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** @author jaurambault */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"extract"},
    commandDescription = "Perform a local extraction of assets")
public class ExtractionCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ExtractionCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_DIRECTORY_DESCRIPTION)
  String sourceDirectoryParam;

  @Parameter(
      names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT},
      variableArity = true,
      required = false,
      description = Param.FILE_TYPE_DESCRIPTION,
      converter = FileTypeConverter.class)
  List<FileType> fileTypes;

  @Parameter(
      names = {Param.FILTER_OPTIONS_LONG, Param.FILTER_OPTIONS_SHORT},
      variableArity = true,
      required = false,
      description = Param.FILTER_OPTIONS_DESCRIPTION)
  List<String> filterOptionsParam;

  @Parameter(
      names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_LOCALE_DESCRIPTION)
  String sourceLocale;

  @Parameter(
      names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_REGEX_DESCRIPTION)
  String sourcePathFilterRegex;

  @Parameter(
      names = {Param.DIR_PATH_INCLUDE_PATTERNS_LONG},
      variableArity = true,
      required = false,
      description = Param.DIR_PATH_INCLUDE_PATTERNS_DESCRIPTION)
  List<String> directoriesIncludePatterns = null;

  @Parameter(
      names = {Param.DIR_PATH_EXCLUDE_PATTERNS_LONG},
      variableArity = true,
      required = false,
      description = Param.DIR_PATH_EXCLUDE_PATTERNS_DESCRIPTION)
  List<String> directoriesExcludePatterns = null;

  @Parameter(
      names = {Param.EXTRACTION_NAME_LONG, Param.EXTRACTION_NAME_SHORT},
      arity = 1,
      required = true,
      description = Param.EXTRACTION_NAME_DESCRIPTION)
  String extractionName;

  @Parameter(
      names = {Param.EXTRACTION_OUTPUT_LONG, Param.EXTRACTION_OUTPUT_SHORT},
      arity = 1,
      required = false,
      description = Param.EXTRACTION_OUTPUT_DESCRIPTION)
  String outputDirectoryParam = ExtractionPaths.DEFAULT_OUTPUT_DIRECTORY;

  @Autowired CommandHelper commandHelper;

  @Autowired ExtractionService extractionService;

  CommandDirectories commandDirectories;

  @Override
  public void execute() throws CommandException {

    commandDirectories = new CommandDirectories(sourceDirectoryParam);

    consoleWriter
        .newLine()
        .a("Perform local extraction, name: ")
        .fg(Ansi.Color.CYAN)
        .a(extractionName)
        .println();

    ExtractionPaths extractionPaths = new ExtractionPaths(outputDirectoryParam, extractionName);
    extractionService.recreateExtractionDirectory(extractionPaths);

    ArrayList<FileMatch> sourceFileMatches =
        commandHelper.getSourceFileMatches(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns);

    for (FileMatch sourceFileMatch : sourceFileMatches) {
      consoleWriter.a("Extracting: ").fg(Color.CYAN).a(sourceFileMatch.getSourcePath()).println();
      List<String> filterOptions =
          commandHelper.getFilterOptionsOrDefaults(
              sourceFileMatch.getFileType(), filterOptionsParam);
      extractionService.fileMatchToAssetExtractionAndSaveToJsonFile(
          extractionPaths,
          filterOptions,
          sourceFileMatch.getFileType().getFilterConfigIdOverride(),
          sourceFileMatch);
    }

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }
}
