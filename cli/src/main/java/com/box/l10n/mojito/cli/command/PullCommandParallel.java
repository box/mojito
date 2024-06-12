package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;
import com.box.l10n.mojito.rest.entity.MultiLocalizedAssetBody;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
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
    this.localeMappingTypeParam = pullCommand.localeMappingTypeParam;
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

    pollableTaskIdToFileMatchMap.put(
        generateLocalizedFilesParallel(repository, sourceFileMatch, filterOptions).getId(),
        sourceFileMatch);
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

  private PollableTask generateLocalizedFilesParallel(
      Repository repository, FileMatch sourceFileMatch, List<String> filterOptions) {
    Asset assetByPathAndRepositoryId;

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

    List<RepositoryLocale> repositoryLocales =
        getMapOutputTagToRepositoryLocale().values().stream()
            .distinct()
            .filter(
                repoLocale -> shouldGenerateLocalizedFileWithCliOutput(sourceFileMatch, repoLocale))
            .toList();

    return getLocalizedAssetBodyParallel(
        sourceFileMatch,
        repositoryLocales,
        getRepoLocaleToOutputTagsMap(),
        filterOptions,
        assetByPathAndRepositoryId,
        commandHelper.getFileContentWithXcodePatch(sourceFileMatch));
  }

  private Map<RepositoryLocale, List<String>> getRepoLocaleToOutputTagsMap() {
    Map<RepositoryLocale, List<String>> localeIdToOutputTagsMap;

    if (localeMappings != null) {

      Map<String, RepositoryLocale> mapOutputTagToRepositoryLocale =
          getMapOutputTagToRepositoryLocale();

      localeIdToOutputTagsMap =
          mapOutputTagToRepositoryLocale.entrySet().stream()
              .collect(
                  Collectors.groupingBy(
                      Map.Entry::getValue,
                      Collectors.mapping(Map.Entry::getKey, Collectors.toList())));

    } else {
      // Adapted for backward compatibility - a deeper refactor could improve clarity.
      // Note: If there is no mapping, it returns an empty map. The impact on the backend is
      // unclear,
      // so this behavior is being preserved.
      localeIdToOutputTagsMap = new HashMap<>();
    }

    return localeIdToOutputTagsMap;
  }

  private boolean shouldGenerateLocalizedFileWithCliOutput(
      FileMatch sourceFileMatch, RepositoryLocale repositoryLocale) {
    boolean localize = shouldGenerateLocalizedFile(repositoryLocale);
    if (!localize) {
      printLocaleSkippedToConsole(sourceFileMatch, repositoryLocale);
    }
    return localize;
  }

  private synchronized void printLocaleSkippedToConsole(
      FileMatch sourceFileMatch, RepositoryLocale repositoryLocale) {
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
