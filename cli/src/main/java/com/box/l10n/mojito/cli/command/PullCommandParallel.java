package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.apiclient.exception.AssetNotFoundException;
import com.box.l10n.mojito.apiclient.model.AssetAssetSummary;
import com.box.l10n.mojito.apiclient.model.LocalizedAssetBody;
import com.box.l10n.mojito.apiclient.model.MultiLocalizedAssetBody;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi;

public class PullCommandParallel extends PullCommand {

  Map<Long, FileMatch> pollableTaskIdToFileMatchMap = new HashMap<>();

  public PullCommandParallel(PullCommand pullCommand) {
    this.consoleWriter = pullCommand.consoleWriter;
    this.commandHelper = pullCommand.commandHelper;
    this.assetClient = pullCommand.assetClient;
    this.commandDirectories = pullCommand.commandDirectories;
    this.repositoryLocalesWithoutRootLocale = pullCommand.repositoryLocalesWithoutRootLocale;
    this.objectMapper = pullCommand.objectMapper;
    this.localeMappingHelper = pullCommand.localeMappingHelper;
    this.rootRepositoryLocale = pullCommand.rootRepositoryLocale;
    this.repository = pullCommand.repository;
    this.localeMappings = pullCommand.localeMappings;
    this.localeFullyTranslated = pullCommand.localeFullyTranslated;
    this.assetMapping = pullCommand.assetMapping;
    this.status = pullCommand.status;
    this.inheritanceMode = pullCommand.inheritanceMode;
    this.directoriesExcludePatterns = pullCommand.directoriesExcludePatterns;
    this.directoriesIncludePatterns = pullCommand.directoriesIncludePatterns;
    this.sourcePathFilterRegex = pullCommand.sourcePathFilterRegex;
    this.filterOptionsParam = pullCommand.filterOptionsParam;
    this.sourceLocale = pullCommand.sourceLocale;
    this.fileTypes = pullCommand.fileTypes;
    this.localeMappingParam = pullCommand.localeMappingParam;
    this.repositoryParam = pullCommand.repositoryParam;
    this.sourceDirectoryParam = pullCommand.sourceDirectoryParam;
    this.targetDirectoryParam = pullCommand.targetDirectoryParam;
    this.onlyIfFullyTranslated = pullCommand.onlyIfFullyTranslated;
    this.pullRunName = pullCommand.pullRunName;
    this.recordPullRun = pullCommand.recordPullRun;
    this.isParallel = pullCommand.isParallel;
  }

  public void pull() throws CommandException {
    consoleWriter.a("Pulling localized assets in parallel").newLine().println();

    for (FileMatch sourceFileMatch :
        commandHelper.getSourceFileMatches(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns)) {
      consoleWriter
          .a("Sending localize request for: ")
          .fg(Ansi.Color.CYAN)
          .a(sourceFileMatch.getSourcePath())
          .println();

      List<String> filterOptions =
          commandHelper.getFilterOptionsOrDefaults(
              sourceFileMatch.getFileType(), filterOptionsParam);

      sendContentForLocalizedGeneration(sourceFileMatch, filterOptions);
    }

    consoleWriter.println();
    consoleWriter.a("Generating localized files").println();
    pollForLocalizedFiles();
  }

  private void sendContentForLocalizedGeneration(
      FileMatch sourceFileMatch, List<String> filterOptions) {
    if (localeMappingParam != null) {
      pollableTaskIdToFileMatchMap.put(
          generateLocalizedFilesWithLocaleMappingParallel(
                  repository, sourceFileMatch, filterOptions)
              .getId(),
          sourceFileMatch);
    } else {
      pollableTaskIdToFileMatchMap.put(
          generateLocalizedFilesWithoutLocaleMappingParallel(
                  repository, sourceFileMatch, filterOptions)
              .getId(),
          sourceFileMatch);
    }
  }

  private void pollForLocalizedFiles() {
    pollableTaskIdToFileMatchMap.entrySet().parallelStream()
        .forEach(
            entry -> {
              commandHelper.waitForPollableTaskSilencedOutput(entry.getKey());
              String jsonOutput =
                  commandHelper.pollableTaskClient.getPollableTaskOutput(entry.getKey());
              MultiLocalizedAssetBody multiLocalizedAsset =
                  objectMapper.readValueUnchecked(jsonOutput, MultiLocalizedAssetBody.class);
              for (String outputTag :
                  multiLocalizedAsset.getGenerateLocalizedAssetJobIds().keySet()) {
                jsonOutput =
                    commandHelper.pollableTaskClient.getPollableTaskOutput(
                        multiLocalizedAsset.getGenerateLocalizedAssetJobIds().get(outputTag));
                writeLocalizedAssetToTargetDirectory(
                    objectMapper.readValueUnchecked(jsonOutput, LocalizedAssetBody.class),
                    entry.getValue());
              }
            });
  }

  private PollableTask generateLocalizedFilesWithLocaleMappingParallel(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {

    List<RepositoryLocaleRepository> repositoryLocales =
        localeMappings.entrySet().stream()
            .map(entry -> getRepositoryLocaleForOutputBcp47Tag(entry.getKey()))
            .distinct()
            .collect(Collectors.toList());
    return generateLocalizedFiles(repository, sourceFileMatch, filterOptions, repositoryLocales);
  }

  private PollableTask generateLocalizedFilesWithoutLocaleMappingParallel(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {

    logger.debug("Generate localized files (without locale mapping)");

    return generateLocalizedFiles(
        repository,
        sourceFileMatch,
        filterOptions,
        Lists.newArrayList(repositoryLocalesWithoutRootLocale.values()));
  }

  void writeLocalizedAssetToTargetDirectory(
      LocalizedAssetBody localizedAsset, FileMatch sourceFileMatch) throws CommandException {

    Path targetPath =
        commandDirectories
            .getTargetDirectoryPath()
            .resolve(sourceFileMatch.getTargetPath(localizedAsset.getBcp47Tag()));

    commandHelper.writeFileContent(localizedAsset.getContent(), targetPath, sourceFileMatch);

    Path relativeTargetFilePath = commandDirectories.relativizeWithUserDirectory(targetPath);
    printFileGeneratedToConsole(localizedAsset, sourceFileMatch, relativeTargetFilePath);
  }

  private synchronized void printFileGeneratedToConsole(
      LocalizedAssetBody localizedAsset, FileMatch sourceFileMatch, Path relativeTargetFilePath) {
    consoleWriter
        .a(" - Generated file for locale ")
        .fg(Ansi.Color.YELLOW)
        .a(localizedAsset.getBcp47Tag())
        .reset()
        .a(": ")
        .fg(Ansi.Color.CYAN)
        .a(sourceFileMatch.getSourcePath())
        .a(" --> ")
        .fg(Ansi.Color.MAGENTA)
        .a(relativeTargetFilePath.toString())
        .println();
  }

  void generateLocalizedFilesWithoutLocaleMapping(
      RepositoryRepository repository, FileMatch sourceFileMatch, List<String> filterOptions)
      throws CommandException {

    logger.debug("Generate localized files (without locale mapping)");

    generateLocalizedFiles(
        repository,
        sourceFileMatch,
        filterOptions,
        Lists.newArrayList(repositoryLocalesWithoutRootLocale.values()));
  }

  private PollableTask generateLocalizedFiles(
      RepositoryRepository repository,
      FileMatch sourceFileMatch,
      List<String> filterOptions,
      List<RepositoryLocaleRepository> repositoryLocales) {
    AssetAssetSummary assetByPathAndRepositoryId;

    String sourcePath =
        commandHelper.getMappedSourcePath(assetMapping, sourceFileMatch.getSourcePath());

    try {
      logger.debug("Getting the asset for path: {}", sourceFileMatch.getSourcePath());
      assetByPathAndRepositoryId =
          assetClient.getAssetByPathAndRepositoryId(sourcePath, repository.getId());
    } catch (AssetNotFoundException e) {
      throw new CommandException(
          String.format(
              "Asset with path [%s] was not found in repo [%s]", sourcePath, repositoryParam),
          e);
    }

    return getLocalizedAssetBodyParallel(
        sourceFileMatch,
        repositoryLocales.stream()
            .filter(
                repoLocale -> shouldGenerateLocalizedFileWithCliOutput(sourceFileMatch, repoLocale))
            .collect(Collectors.toList()),
        getRepoLocaleToOutputTagsMap(),
        filterOptions,
        assetByPathAndRepositoryId,
        commandHelper.getFileContentWithXcodePatch(sourceFileMatch));
  }

  private Map<RepositoryLocaleRepository, List<String>> getRepoLocaleToOutputTagsMap() {
    Map<RepositoryLocaleRepository, List<String>> localeIdToOutputTagsMap = new HashMap<>();

    if (localeMappings != null) {
      for (Map.Entry<String, String> mapping : localeMappings.entrySet()) {
        String outputBcp47tag = mapping.getKey();
        RepositoryLocaleRepository locale = getRepositoryLocaleForOutputBcp47Tag(outputBcp47tag);
        if (localeIdToOutputTagsMap.containsKey(locale)) {
          localeIdToOutputTagsMap.get(locale).add(outputBcp47tag);
        } else {
          localeIdToOutputTagsMap.put(locale, Lists.newArrayList(outputBcp47tag));
        }
      }
    }

    return localeIdToOutputTagsMap;
  }

  private boolean shouldGenerateLocalizedFileWithCliOutput(
      FileMatch sourceFileMatch, RepositoryLocaleRepository repositoryLocale) {
    boolean localize = shouldGenerateLocalizedFile(repositoryLocale);
    if (!localize) {
      printLocaleSkippedToConsole(sourceFileMatch, repositoryLocale);
    }
    return localize;
  }

  private synchronized void printLocaleSkippedToConsole(
      FileMatch sourceFileMatch, RepositoryLocaleRepository repositoryLocale) {
    consoleWriter
        .a("Skipping locale: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryLocale.getLocale().getBcp47Tag())
        .reset()
        .a(" for: ")
        .fg(Ansi.Color.CYAN)
        .a(sourceFileMatch.getSourcePath())
        .print();
    consoleWriter.a(" --> ").fg(Ansi.Color.MAGENTA).a("as not fully translated").println();
  }
}
