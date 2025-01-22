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
import com.box.l10n.mojito.cli.model.LocaleInfo;
import com.box.l10n.mojito.cli.model.LocalizedAssetBody;
import com.box.l10n.mojito.cli.model.MultiLocalizedAssetBody;
import com.box.l10n.mojito.cli.model.PollableTask;
import com.box.l10n.mojito.cli.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.cli.model.RepositoryLocaleStatisticRepository;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.cli.model.RepositoryStatisticRepository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.fusesource.jansi.Ansi.Color;
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
    commandNames = {"pull", "l"},
    commandDescription = "Pull localized assets from TMS")
public class PullCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PullCommand.class);

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
      names = {"--inheritance-mode"},
      required = false,
      description =
          "Inheritance Mode. Used when there is no translations in the target locale for a text unit. (USE_PARENT to fallback to parent locale translation, REMOVE_UNTRANSLATED to remove the text unit from the file ",
      converter = LocalizedAssetBodyInheritanceModeConverter.class)
  LocalizedAssetBody.InheritanceModeEnum inheritanceMode =
      LocalizedAssetBody.InheritanceModeEnum.USE_PARENT;

  @Parameter(
      names = {"--status"},
      required = false,
      description =
          "To choose the translations used to generate the file based on their status. ACCEPTED: only includes translations that are accepted. ACCEPTED_OR_NEEDS_REVIEW: includes translations that are accepted or that need review. ALL: includes all translations available even if they need re-translation (\"rejected\" translations are always excluded as they are considered harmful).",
      converter = LocalizedAssetBodyStatusConverter.class)
  LocalizedAssetBody.StatusEnum status = LocalizedAssetBody.StatusEnum.ALL;

  @Parameter(
      names = {"--asset-mapping", "-am"},
      required = false,
      description = "Asset mapping, format: \"local1:remote1;local2:remote2\"",
      converter = AssetMappingConverter.class)
  Map<String, String> assetMapping;

  @Parameter(
      names = {"--fully-translated"},
      required = false,
      description =
          "To pull localized assets only if all strings for the locale are fully translated")
  Boolean onlyIfFullyTranslated = false;

  @Parameter(
      names = {"--async-ws"},
      required = false,
      description = "Use async WS, use for processing big files (should become default eventually)")
  Boolean asyncWS = false;

  @Parameter(
      names = {"--record-pull-run"},
      required = false,
      description =
          "To record the list of text unit variants that are used to generate the files, through an abstraction called `pull run`. "
              + "A file will be generated with the pull run name.")
  Boolean recordPullRun = false;

  @Parameter(
      names = {"--parallel"},
      required = false,
      description =
          "Indicates that the pull should use parallel execution. This is run as an asynchronous request, if --async-ws is also specified it will be ignored.")
  Boolean isParallel = false;

  @Autowired AssetWsApiProxy assetClient;

  @Autowired CommandHelper commandHelper;

  @Autowired ObjectMapper objectMapper;

  @Autowired LocaleMappingHelper localeMappingHelper;

  Map<String, RepositoryLocaleRepository> repositoryLocalesWithoutRootLocale;

  RepositoryLocaleRepository rootRepositoryLocale;

  RepositoryRepository repository;

  CommandDirectories commandDirectories;

  /** Contains a map of locale for generating localized file a locales defined in the repository. */
  Map<String, String> localeMappings;

  /** Map of locale and the boolean value for fully translated status of the locale */
  Map<String, Boolean> localeFullyTranslated = new HashMap<>();

  String pullRunName;

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Pull localized asset from repository: ")
        .fg(Color.CYAN)
        .a(repositoryParam)
        .println(2);

    repository = commandHelper.findRepositoryByName(repositoryParam);

    if (onlyIfFullyTranslated) {
      initLocaleFullyTranslatedMap(repository);
    }

    commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

    initPullRunName();
    initRepositoryLocalesMapAndRootRepositoryLocale(repository);
    localeMappings = localeMappingHelper.getLocaleMapping(localeMappingParam);

    if (isParallel) {
      PullCommandParallel pullCommandParallel = new PullCommandParallel(this);
      pullCommandParallel.pull();
    } else {
      for (FileMatch sourceFileMatch :
          commandHelper.getSourceFileMatches(
              commandDirectories,
              fileTypes,
              sourceLocale,
              sourcePathFilterRegex,
              directoriesIncludePatterns,
              directoriesExcludePatterns)) {

        consoleWriter.a("Localizing: ").fg(Color.CYAN).a(sourceFileMatch.getSourcePath()).println();

        List<String> filterOptions =
            commandHelper.getFilterOptionsOrDefaults(
                sourceFileMatch.getFileType(), filterOptionsParam);

        generateLocalizedFiles(sourceFileMatch, filterOptions);
      }
    }

    writePullRunFileIfNeeded();

    consoleWriter.fg(Color.GREEN).newLine().a("Finished").println(2);
  }

  private void generateLocalizedFiles(FileMatch sourceFileMatch, List<String> filterOptions) {
    if (localeMappingParam != null) {
      generateLocalizedFilesWithLocaleMaping(repository, sourceFileMatch, filterOptions);
    } else {
      generateLocalizedFilesWithoutLocaleMapping(repository, sourceFileMatch, filterOptions);
    }
  }

  void initPullRunName() {
    if (recordPullRun) {
      pullRunName = UUID.randomUUID().toString();
    }
  }

  void writePullRunFileIfNeeded() {
    if (recordPullRun) {
      Path pullRunOuputFile =
          commandDirectories.getTargetDirectoryPath().resolve(PullRunHelper.PULL_RUN_NAME_FILE);
      consoleWriter
          .a("Writing pull run name to file: ")
          .fg(Color.CYAN)
          .a(commandDirectories.relativizeWithUserDirectory(pullRunOuputFile).toString())
          .println();
      commandHelper.writeFileContent(pullRunName, pullRunOuputFile);
    }
  }

  /**
   * Default generation, uses the locales defined in the repository to generate the localized files.
   *
   * @param repository
   * @param sourceFileMatch
   * @param filterOptions
   * @throws CommandException
   */
  void generateLocalizedFilesWithoutLocaleMapping(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {

    logger.debug("Generate localized files (without locale mapping)");

    for (RepositoryLocaleRepository repositoryLocale :
        repositoryLocalesWithoutRootLocale.values()) {
      generateLocalizedFile(repository, sourceFileMatch, filterOptions, null, repositoryLocale);
    }
  }

  /**
   * Generation with locale mapping. The localized files are generated using specific output tags
   * while still using the repository locale to fetch the proper translations.
   *
   * @param repository
   * @param sourceFileMatch
   * @param filterOptions
   * @throws CommandException
   */
  void generateLocalizedFilesWithLocaleMaping(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {

    logger.debug("Generate localized files with locale mapping");

    for (Map.Entry<String, String> localeMapping : localeMappings.entrySet()) {
      String outputBcp47tag = localeMapping.getKey();
      RepositoryLocaleRepository repositoryLocale =
          getRepositoryLocaleForOutputBcp47Tag(outputBcp47tag);
      generateLocalizedFile(
          repository, sourceFileMatch, filterOptions, outputBcp47tag, repositoryLocale);
    }
  }

  void generateLocalizedFile(
      RepositoryRepository repository,
      FileMatch sourceFileMatch,
      List<String> filterOptions,
      String outputBcp47tag,
      RepositoryLocaleRepository repositoryLocale)
      throws CommandException {
    if (shouldGenerateLocalizedFile(repositoryLocale)) {
      LocalizedAssetBody localizedAsset =
          getLocalizedAsset(
              repository, sourceFileMatch, repositoryLocale, outputBcp47tag, filterOptions);
      writeLocalizedAssetToTargetDirectory(localizedAsset, sourceFileMatch);
    } else {
      consoleWriter
          .a(" - Skipping locale: ")
          .fg(Color.CYAN)
          .a(repositoryLocale.getLocale().getBcp47Tag())
          .print();
      consoleWriter.a(" --> ").fg(Color.MAGENTA).a("not fully translated").println();
    }
  }

  /**
   * Gets the {@link RepositoryLocaleRepository} that correspond to the output BCP47 tag
   * based on the {@link #localeMappings
   *
   * @param outputBcp47tag
   * @return the repository locale to be used for the output BCP47 tag
   * @throws CommandException if the mapping is invalid
   */
  RepositoryLocaleRepository getRepositoryLocaleForOutputBcp47Tag(String outputBcp47tag)
      throws CommandException {

    String repositoryLocaleBcp47Tag = localeMappings.get(outputBcp47tag);

    RepositoryLocaleRepository repositoryLocale;

    if (rootRepositoryLocale.getLocale().getBcp47Tag().equals(outputBcp47tag)) {
      repositoryLocale = rootRepositoryLocale;
    } else {
      repositoryLocale = repositoryLocalesWithoutRootLocale.get(repositoryLocaleBcp47Tag);
    }

    if (repositoryLocale == null) {
      throw new CommandException(
          "Invalid locale mapping for tag: "
              + outputBcp47tag
              + ", locale: "
              + repositoryLocaleBcp47Tag
              + " is not available in the repository locales");
    }

    return repositoryLocale;
  }

  /**
   * Gets the list of {@link RepositoryLocaleRepository}s of a {@link RepositoryRepository}
   * excluding the root locale (the only locale that has no parent locale).
   *
   * @param repository the repository
   * @return the list of {@link RepositoryLocaleRepository}s excluding the root locale.
   */
  private Map<String, RepositoryLocaleRepository> initRepositoryLocalesMapAndRootRepositoryLocale(
      RepositoryRepository repository) {

    repositoryLocalesWithoutRootLocale = new HashMap<>();

    for (RepositoryLocaleRepository repositoryLocale : repository.getRepositoryLocales()) {
      if (repositoryLocale.getParentLocale() != null) {
        repositoryLocalesWithoutRootLocale.put(
            repositoryLocale.getLocale().getBcp47Tag(), repositoryLocale);
      } else {
        rootRepositoryLocale = repositoryLocale;
      }
    }

    return repositoryLocalesWithoutRootLocale;
  }

  void writeLocalizedAssetToTargetDirectory(
      LocalizedAssetBody localizedAsset, FileMatch sourceFileMatch) throws CommandException {

    Path targetPath =
        commandDirectories
            .getTargetDirectoryPath()
            .resolve(sourceFileMatch.getTargetPath(localizedAsset.getBcp47Tag()));

    commandHelper.writeFileContent(localizedAsset.getContent(), targetPath, sourceFileMatch);

    Path relativeTargetFilePath = commandDirectories.relativizeWithUserDirectory(targetPath);
    consoleWriter.a(" --> ").fg(Color.MAGENTA).a(relativeTargetFilePath.toString()).println();
  }

  LocalizedAssetBody getLocalizedAsset(
      RepositoryRepository repository,
      FileMatch sourceFileMatch,
      RepositoryLocaleRepository repositoryLocale,
      String outputBcp47tag,
      List<String> filterOptions)
      throws CommandException {
    consoleWriter
        .a(" - Processing locale: ")
        .fg(Color.CYAN)
        .a(repositoryLocale.getLocale().getBcp47Tag())
        .print();

    String sourcePath =
        commandHelper.getMappedSourcePath(assetMapping, sourceFileMatch.getSourcePath());

    AssetAssetSummary assetByPathAndRepositoryId;

    try {
      logger.debug(
          "Getting the asset for path: {} and locale: {}",
          sourcePath,
          repositoryLocale.getLocale().getBcp47Tag());
      assetByPathAndRepositoryId =
          assetClient.getAssetByPathAndRepositoryId(sourcePath, repository.getId());
    } catch (AssetNotFoundException e) {
      throw new CommandException(
          "Asset with path [" + sourcePath + "] was not found in repo [" + repositoryParam + "]",
          e);
    }

    String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);

    LocalizedAssetBody localizedAsset = null;

    if (asyncWS) {
      localizedAsset =
          getLocalizedAssetBodyAsync(
              sourceFileMatch,
              repositoryLocale,
              outputBcp47tag,
              filterOptions,
              assetByPathAndRepositoryId,
              assetContent);
    } else {
      localizedAsset =
          getLocalizedAssetBodySync(
              sourceFileMatch,
              repositoryLocale,
              outputBcp47tag,
              filterOptions,
              assetByPathAndRepositoryId,
              assetContent,
              localizedAsset);
    }

    logger.trace("LocalizedAsset content = {}", localizedAsset.getContent());

    return localizedAsset;
  }

  private LocalizedAssetBody getLocalizedAssetBody(
      FileMatch sourceFileMatch,
      RepositoryLocaleRepository repositoryLocale,
      String outputBcp47tag,
      List<String> filterOptions,
      AssetAssetSummary assetByPathAndRepositoryId,
      String assetContent) {
    LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
    localizedAssetBody.setAssetId(assetByPathAndRepositoryId.getId());
    localizedAssetBody.setLocaleId(repositoryLocale.getLocale().getId());
    localizedAssetBody.setContent(assetContent);
    localizedAssetBody.setOutputBcp47tag(outputBcp47tag);
    localizedAssetBody.setFilterConfigIdOverride(
        ofNullable(sourceFileMatch.getFileType().getFilterConfigIdOverride())
            .map(
                filterConfigIdOverride ->
                    LocalizedAssetBody.FilterConfigIdOverrideEnum.fromValue(
                        filterConfigIdOverride.name()))
            .orElse(null));
    localizedAssetBody.setFilterOptions(filterOptions);
    localizedAssetBody.setInheritanceMode(inheritanceMode);
    localizedAssetBody.setStatus(status);
    localizedAssetBody.setPullRunName(pullRunName);
    return localizedAssetBody;
  }

  LocalizedAssetBody getLocalizedAssetBodySync(
      FileMatch sourceFileMatch,
      RepositoryLocaleRepository repositoryLocale,
      String outputBcp47tag,
      List<String> filterOptions,
      AssetAssetSummary assetByPathAndRepositoryId,
      String assetContent,
      LocalizedAssetBody localizedAsset)
      throws CommandException {
    // TODO remove this is temporary, Async service is implemented but we don't use it yet by
    // default
    int count = 0;
    int maxCount = 5;
    while (localizedAsset == null && count < maxCount) {
      try {
        LocalizedAssetBody localizedAssetBody =
            this.getLocalizedAssetBody(
                sourceFileMatch,
                repositoryLocale,
                outputBcp47tag,
                filterOptions,
                assetByPathAndRepositoryId,
                assetContent);
        localizedAsset =
            assetClient.getLocalizedAssetForContent(
                localizedAssetBody,
                assetByPathAndRepositoryId.getId(),
                repositoryLocale.getLocale().getId());
      } catch (Exception e) {
        count++;
        consoleWriter
            .fg(Color.RED)
            .a("Attempt ")
            .a(count)
            .a("/")
            .a(maxCount)
            .a(" for locale: ")
            .a(repositoryLocale.getLocale().getBcp47Tag())
            .a(" failed. Retrying...")
            .println();
      }
    }

    if (count == maxCount) {
      throw new CommandException(
          "getLocalizedAssetBodySync failed even after retries. retry count: " + count);
    }

    return localizedAsset;
  }

  private static List<LocaleInfo> mapRepoLocaleToLocaleInfos(
      RepositoryLocaleRepository locale,
      Map<RepositoryLocaleRepository, List<String>> repoLocaleToOutputTagsMap) {
    List<LocaleInfo> localeInfos = new ArrayList<>();
    if (repoLocaleToOutputTagsMap.containsKey(locale)) {
      for (String outputTag : repoLocaleToOutputTagsMap.get(locale)) {
        LocaleInfo localeInfo = new LocaleInfo();
        localeInfo.setLocaleId(locale.getLocale().getId());
        localeInfo.setOutputBcp47tag(outputTag);
        localeInfos.add(localeInfo);
      }
    } else {
      LocaleInfo localeInfo = new LocaleInfo();
      localeInfo.setLocaleId(locale.getLocale().getId());
      localeInfos.add(localeInfo);
    }
    return localeInfos;
  }

  PollableTask getLocalizedAssetBodyParallel(
      FileMatch sourceFileMatch,
      List<RepositoryLocaleRepository> repositoryLocales,
      Map<RepositoryLocaleRepository, List<String>> localeIdToOutputTagsMap,
      List<String> filterOptions,
      AssetAssetSummary assetByPathAndRepositoryId,
      String assetContent)
      throws CommandException {
    List<LocaleInfo> localeInfos =
        repositoryLocales.stream()
            .map(locale -> mapRepoLocaleToLocaleInfos(locale, localeIdToOutputTagsMap))
            .flatMap(List::stream)
            .toList();
    MultiLocalizedAssetBody multiLocalizedAssetBody = new MultiLocalizedAssetBody();
    multiLocalizedAssetBody.setAssetId(assetByPathAndRepositoryId.getId());
    multiLocalizedAssetBody.setSourceContent(assetContent);
    multiLocalizedAssetBody.setLocaleInfos(localeInfos);
    multiLocalizedAssetBody.setGenerateLocalizedAssetJobIds(new HashMap<>());
    multiLocalizedAssetBody.setFilterConfigIdOverride(
        ofNullable(sourceFileMatch.getFileType().getFilterConfigIdOverride())
            .map(
                filterConfigIdOverride ->
                    MultiLocalizedAssetBody.FilterConfigIdOverrideEnum.fromValue(
                        filterConfigIdOverride.name()))
            .orElse(null));
    multiLocalizedAssetBody.setFilterOptions(filterOptions);
    multiLocalizedAssetBody.setInheritanceMode(
        ofNullable(inheritanceMode)
            .map(
                inheritanceModeEnum ->
                    MultiLocalizedAssetBody.InheritanceModeEnum.fromValue(
                        inheritanceModeEnum.name()))
            .orElse(null));
    multiLocalizedAssetBody.setStatus(
        ofNullable(status)
            .map(statusEnum -> MultiLocalizedAssetBody.StatusEnum.fromValue(statusEnum.name()))
            .orElse(null));
    multiLocalizedAssetBody.setPullRunName(pullRunName);
    return assetClient.getLocalizedAssetForContentParallel(
        multiLocalizedAssetBody, assetByPathAndRepositoryId.getId());
  }

  LocalizedAssetBody getLocalizedAssetBodyAsync(
      FileMatch sourceFileMatch,
      RepositoryLocaleRepository repositoryLocale,
      String outputBcp47tag,
      List<String> filterOptions,
      AssetAssetSummary assetByPathAndRepositoryId,
      String assetContent)
      throws CommandException {
    LocalizedAssetBody localizedAssetBody =
        this.getLocalizedAssetBody(
            sourceFileMatch,
            repositoryLocale,
            outputBcp47tag,
            filterOptions,
            assetByPathAndRepositoryId,
            assetContent);
    LocalizedAssetBody localizedAsset;
    PollableTask localizedAssetForContentAsync =
        assetClient.getLocalizedAssetForContentAsync(
            localizedAssetBody, assetByPathAndRepositoryId.getId());
    commandHelper.waitForPollableTask(localizedAssetForContentAsync.getId());
    String jsonOutput =
        commandHelper.pollableTaskClient.getPollableTaskOutput(
            localizedAssetForContentAsync.getId());
    localizedAsset = objectMapper.readValueUnchecked(jsonOutput, LocalizedAssetBody.class);
    return localizedAsset;
  }

  private void initLocaleFullyTranslatedMap(RepositoryRepository repository) {
    RepositoryStatisticRepository repoStat = repository.getRepositoryStatistic();
    if (repoStat != null) {
      for (RepositoryLocaleStatisticRepository repoLocaleStat :
          repoStat.getRepositoryLocaleStatistics()) {
        localeFullyTranslated.put(
            repoLocaleStat.getLocale().getBcp47Tag(),
            repoLocaleStat.getForTranslationCount() == 0L);
      }
    }
  }

  protected boolean shouldGenerateLocalizedFile(RepositoryLocaleRepository repositoryLocale) {
    boolean localize = true;

    if (onlyIfFullyTranslated) {
      if (repositoryLocale.isToBeFullyTranslated()) {
        localize = localeFullyTranslated.get(repositoryLocale.getLocale().getBcp47Tag());
      } else {
        // the root locale (which is defacto fully translated) is not in the localeFullyTranslated
        // map so default to true
        localize =
            localeFullyTranslated.getOrDefault(
                repositoryLocale.getParentLocale().getLocale().getBcp47Tag(), true);
      }
    }

    return localize;
  }
}
