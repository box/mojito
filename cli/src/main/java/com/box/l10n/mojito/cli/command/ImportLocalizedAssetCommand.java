package com.box.l10n.mojito.cli.command;

import static java.util.Optional.ofNullable;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.cli.apiclient.AssetWsApiProxy;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.model.AssetAssetSummary;
import com.box.l10n.mojito.cli.model.ImportLocalizedAssetBody;
import com.box.l10n.mojito.cli.model.LocaleRepository;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"import"},
    commandDescription = "Import localized assets into the TMS")
public class ImportLocalizedAssetCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ImportLocalizedAssetCommand.class);

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
      names = {Param.REPOSITORY_LOCALES_MAPPING_LONG, Param.REPOSITORY_LOCALES_MAPPING_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_LOCALES_MAPPING_DESCRIPTION)
  String localeMappingParam;

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
      names = {"--status-equal-target"},
      required = false,
      description =
          "Status of the imported translation when the target is the same as "
              + "the parent (SKIPPED for no import). Applies only to fully translated locales",
      converter = ImportLocalizedAssetBodyStatusForEqualTargetConverter.class)
  ImportLocalizedAssetBody.StatusForEqualTargetEnum statusForEqualTarget =
      ImportLocalizedAssetBody.StatusForEqualTargetEnum.APPROVED;

  @Autowired AssetWsApiProxy assetClient;

  @Autowired CommandHelper commandHelper;

  @Autowired LocaleMappingHelper localeMappingHelper;

  RepositoryRepository repository;

  CommandDirectories commandDirectories;

  /** Contains a map of locale for generating localized file a locales defined in the repository. */
  Map<String, String> inverseLocaleMapping;

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Start importing localized files for repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    repository = commandHelper.findRepositoryByName(repositoryParam);
    commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);
    inverseLocaleMapping = localeMappingHelper.getInverseLocaleMapping(localeMappingParam);

    for (FileMatch sourceFileMatch :
        commandHelper.getSourceFileMatches(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns)) {
      for (LocaleRepository locale : getLocalesForImport()) {
        doImportFileMatch(sourceFileMatch, locale);
      }
    }

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }

  protected void doImportFileMatch(FileMatch fileMatch, LocaleRepository locale)
      throws CommandException {
    try {
      logger.info("Importing for locale: {}", locale.getBcp47Tag());
      Path targetPath = getTargetPath(fileMatch, locale);

      consoleWriter
          .a(" - Importing file: ")
          .fg(Ansi.Color.MAGENTA)
          .a(targetPath.toString())
          .println();

      AssetAssetSummary assetByPathAndRepositoryId =
          assetClient.getAssetByPathAndRepositoryId(fileMatch.getSourcePath(), repository.getId());

      ImportLocalizedAssetBody importLocalizedAssetBody = new ImportLocalizedAssetBody();
      importLocalizedAssetBody.setContent(commandHelper.getFileContent(targetPath));
      importLocalizedAssetBody.setStatusForEqualTarget(statusForEqualTarget);
      importLocalizedAssetBody.setFilterConfigIdOverride(
          ofNullable(fileMatch.getFileType().getFilterConfigIdOverride())
              .map(
                  filterConfigIdOverride ->
                      ImportLocalizedAssetBody.FilterConfigIdOverrideEnum.fromValue(
                          filterConfigIdOverride.name()))
              .orElse(null));
      importLocalizedAssetBody.setFilterOptions(
          commandHelper.getFilterOptionsOrDefaults(fileMatch.getFileType(), filterOptionsParam));
      ImportLocalizedAssetBody importLocalizedAssetForContent =
          assetClient.importLocalizedAsset(
              importLocalizedAssetBody, assetByPathAndRepositoryId.getId(), locale.getId());

      try {
        commandHelper.waitForPollableTask(importLocalizedAssetForContent.getPollableTask().getId());
      } catch (PollableTaskException e) {
        throw new CommandException(e.getMessage(), e.getCause());
      }
    } catch (AssetNotFoundException ex) {
      throw new CommandException(
          "No asset for file [" + fileMatch.getPath() + "] into repo [" + repositoryParam + "]",
          ex);
    }
  }

  public Collection<LocaleRepository> getLocalesForImport() {
    Collection<LocaleRepository> sortedRepositoryLocales =
        commandHelper.getSortedRepositoryLocales(repository).values();
    filterLocalesWithMapping(sortedRepositoryLocales);
    return sortedRepositoryLocales;
  }

  private void filterLocalesWithMapping(Collection<LocaleRepository> locales) {

    if (inverseLocaleMapping != null) {
      Iterator<LocaleRepository> iterator = locales.iterator();
      while (iterator.hasNext()) {
        LocaleRepository l = iterator.next();
        if (!inverseLocaleMapping.containsKey(l.getBcp47Tag())) {
          iterator.remove();
        }
      }
    }
  }

  private Path getTargetPath(FileMatch fileMatch, LocaleRepository locale) throws CommandException {

    String targetLocale;

    if (inverseLocaleMapping != null) {
      targetLocale = inverseLocaleMapping.get(locale.getBcp47Tag());
    } else {
      targetLocale = locale.getBcp47Tag();
    }

    logger.info("processing locale for import: {}", targetLocale);
    Path targetPath =
        commandDirectories.getTargetDirectoryPath().resolve(fileMatch.getTargetPath(targetLocale));

    return targetPath;
  }
}
