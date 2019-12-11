package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.XcodeXliffFileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.exception.AssetNotFoundException;
import com.box.l10n.mojito.rest.entity.Asset;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.rest.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.rest.entity.RepositoryStatistic;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"pull", "l"}, commandDescription = "Pull localized assets from TMS")
public class PullCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PullCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.TARGET_DIRECTORY_DESCRIPTION)
    String targetDirectoryParam;

    @Parameter(names = {Param.REPOSITORY_LOCALES_MAPPING_LONG, Param.REPOSITORY_LOCALES_MAPPING_SHORT}, arity = 1, required = false, description = "Locale mapping, format: \"fr:fr-FR,ja:ja-JP\". "
            + "The keys contain BCP47 tags of the generated files and the values indicate which repository locales are used to fetch the translations.")
    String localeMappingParam;

    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;

    @Parameter(names = {Param.FILTER_OPTIONS_LONG, Param.FILTER_OPTIONS_SHORT}, variableArity = true, required = false, description = Param.FILTER_OPTIONS_DESCRIPTION)
    List<String> filterOptionsParam;

    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;

    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;

    @Parameter(names = {"--inheritance-mode"}, required = false, description = "Inheritance Mode. Used when there is no translations in the target locale for a text unit. (USE_PARENT to fallback to parent locale translation, REMOVE_UNTRANSLATED to remove the text unit from the file ",
            converter = LocalizedAssetBodyInheritanceModeConverter.class)
    LocalizedAssetBody.InheritanceMode inheritanceMode = LocalizedAssetBody.InheritanceMode.USE_PARENT;

    @Parameter(names = {"--status"}, required = false, description = "To choose the translations used to generate the file based on their status. ACCEPTED: only includes translations that are accepted. ACCEPTED_OR_NEEDS_REVIEW: includes translations that are accepted or that need review. ALL: includes all translations available even if they need re-translation (\"rejected\" translations are always excluded as they are considered harmful).",
            converter = LocalizedAssetBodyStatusConverter.class)
    LocalizedAssetBody.Status status = LocalizedAssetBody.Status.ALL;

    @Parameter(names = {"--asset-mapping", "-am"}, required = false, description = "Asset mapping, format: \"local1:remote1;local2:remote2\"", converter = AssetMappingConverter.class)
    Map<String, String> assetMapping;

    @Parameter(names = {"--fully-translated"}, required = false, description = "To pull localized assets only if all strings for the locale are fully translated")
    Boolean onlyIfFullyTranslated = false;

    @Autowired
    AssetClient assetClient;

    @Autowired
    CommandHelper commandHelper;

    Map<String, RepositoryLocale> repositoryLocalesWithoutRootLocale;

    RepositoryLocale rootRepositoryLocale;

    Repository repository;

    CommandDirectories commandDirectories;

    /**
     * Contains a map of locale for generating localized file a locales defined
     * in the repository.
     */
    Map<String, String> localeMappings;

    /**
     * Map of locale and the boolean value for fully translated status of the
     * locale
     */
    Map<String, Boolean> localeFullyTranslated = new HashMap<>();

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Pull localized asset from repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

        repository = commandHelper.findRepositoryByName(repositoryParam);

        if (onlyIfFullyTranslated) {
            initLocaleFullyTranslatedMap(repository);
        }

        commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

        initRepositoryLocalesMapAndRootRepositoryLocale(repository);
        localeMappings = commandHelper.getLocaleMapping(localeMappingParam);

        for (FileMatch sourceFileMatch : commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex)) {

            consoleWriter.a("Localizing: ").fg(Color.CYAN).a(sourceFileMatch.getSourcePath()).println();

            List<String> filterOptions = commandHelper.getFilterOptionsOrDefaults(sourceFileMatch.getFileType(), filterOptionsParam);

            if (localeMappingParam != null) {
                generateLocalizedFilesWithLocaleMaping(repository, sourceFileMatch, filterOptions);
            } else {
                generateLocalizedFilesWithoutLocaleMapping(repository, sourceFileMatch, filterOptions);
            }
        }

        consoleWriter.fg(Color.GREEN).newLine().a("Finished").println(2);
    }

    /**
     * Default generation, uses the locales defined in the repository to
     * generate the localized files.
     *
     * @param repository
     * @param sourceFileMatch
     * @param filterOptions
     * @throws CommandException
     */
    void generateLocalizedFilesWithoutLocaleMapping(Repository repository, FileMatch sourceFileMatch, List<String> filterOptions) throws CommandException {

        logger.debug("Generate localized files (without locale mapping)");

        for (RepositoryLocale repositoryLocale : repositoryLocalesWithoutRootLocale.values()) {
            generateLocalizedFile(repository, sourceFileMatch, filterOptions, null, repositoryLocale);
        }
    }

    /**
     * Generation with locale mapping. The localized files are generated using
     * specific output tags while still using the repository locale to fetch the
     * proper translations.
     *
     * @param repository
     * @param sourceFileMatch
     * @param filterOptions
     * @throws CommandException
     */
    void generateLocalizedFilesWithLocaleMaping(Repository repository, FileMatch sourceFileMatch, List<String> filterOptions) throws CommandException {

        logger.debug("Generate localzied files with locale mapping");

        for (Map.Entry<String, String> localeMapping : localeMappings.entrySet()) {
            String outputBcp47tag = localeMapping.getKey();
            RepositoryLocale repositoryLocale = getRepositoryLocaleForOutputBcp47Tag(outputBcp47tag);
            generateLocalizedFile(repository, sourceFileMatch, filterOptions, outputBcp47tag, repositoryLocale);
        }
    }

    void generateLocalizedFile(Repository repository, FileMatch sourceFileMatch, List<String> filterOptions, String outputBcp47tag, RepositoryLocale repositoryLocale) throws CommandException {
        if (shouldGenerateLocalizedFile(repositoryLocale)) {
            LocalizedAssetBody localizedAsset = getLocalizedAsset(repository, sourceFileMatch, repositoryLocale, outputBcp47tag, filterOptions);
            writeLocalizedAssetToTargetDirectory(localizedAsset, sourceFileMatch);
        } else {
            consoleWriter.a(" - Skipping locale: ").fg(Color.CYAN).a(repositoryLocale.getLocale().getBcp47Tag()).print();
            consoleWriter.a(" --> ").fg(Color.MAGENTA).a("not fully translated").println();
        }
    }

    /**
     * Gets the {@link RepositoryLocale} that correspond to the output BCP47 tag
     * based on the {@link #localeMappings
     *
     * @param outputBcp47tag
     * @return the repository locale to be used for the output BCP47 tag
     * @throws CommandException if the mapping is invalid
     */
    RepositoryLocale getRepositoryLocaleForOutputBcp47Tag(String outputBcp47tag) throws CommandException {

        String repositoryLocaleBcp47Tag = localeMappings.get(outputBcp47tag);

        RepositoryLocale repositoryLocale;

        if (rootRepositoryLocale.getLocale().getBcp47Tag().equals(outputBcp47tag)) {
            repositoryLocale = rootRepositoryLocale;
        } else {
            repositoryLocale = repositoryLocalesWithoutRootLocale.get(repositoryLocaleBcp47Tag);
        }

        if (repositoryLocale == null) {
            throw new CommandException("Invalid locale mapping for tag: " + outputBcp47tag + ", locale: " + repositoryLocaleBcp47Tag + " is not available in the repository locales");
        }

        return repositoryLocale;
    }

    /**
     * Gets the list of {@link RepositoryLocale}s of a {@link Repository}
     * excluding the root locale (the only locale that has no parent locale).
     *
     * @param repository the repository
     * @return the list of {@link RepositoryLocale}s excluding the root locale.
     */
    private Map<String, RepositoryLocale> initRepositoryLocalesMapAndRootRepositoryLocale(Repository repository) {

        repositoryLocalesWithoutRootLocale = new HashMap<>();

        for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
            if (repositoryLocale.getParentLocale() != null) {
                repositoryLocalesWithoutRootLocale.put(repositoryLocale.getLocale().getBcp47Tag(), repositoryLocale);
            } else {
                rootRepositoryLocale = repositoryLocale;
            }
        }

        return repositoryLocalesWithoutRootLocale;
    }

    void writeLocalizedAssetToTargetDirectory(LocalizedAssetBody localizedAsset, FileMatch sourceFileMatch) throws CommandException {

        Path targetPath = commandDirectories.getTargetDirectoryPath().resolve(sourceFileMatch.getTargetPath(localizedAsset.getBcp47Tag()));

        commandHelper.writeFileContent(localizedAsset.getContent(), targetPath, sourceFileMatch);

        Path relativeTargetFilePath = commandDirectories.relativizeWithUserDirectory(targetPath);

        consoleWriter.a(" --> ").fg(Color.MAGENTA).a(relativeTargetFilePath.toString()).println();
    }

    LocalizedAssetBody getLocalizedAsset(Repository repository, FileMatch sourceFileMatch, RepositoryLocale repositoryLocale, String outputBcp47tag, List<String> filterOptions) throws CommandException {
        consoleWriter.a(" - Processing locale: ").fg(Color.CYAN).a(repositoryLocale.getLocale().getBcp47Tag()).print();

        String sourcePath = sourceFileMatch.getSourcePath();

        if (assetMapping != null && assetMapping.get(sourcePath) != null) {
            String mapping = assetMapping.get(sourcePath);
            logger.debug("Use asset mapping from: {} to {}", sourcePath, mapping);
            sourcePath = mapping;
        }

        Asset assetByPathAndRepositoryId;

        try {
            logger.debug("Getting the asset for path: {} and locale: {}", sourcePath, repositoryLocale.getLocale().getBcp47Tag());
            assetByPathAndRepositoryId = assetClient.getAssetByPathAndRepositoryId(sourcePath, repository.getId());
        } catch (AssetNotFoundException e) {
            throw new CommandException("Asset with path [" + sourcePath + "] was not found in repo [" + repositoryParam + "]", e);
        }

        String assetContent = commandHelper.getFileContent(sourceFileMatch.getPath());

        // TODO(P1) This is to inject xml:space="preserve" in the trans-unit element
        // in the xcode-generated xliff until xcode fixes the bug of not adding this attribute
        // See Xcode bug http://www.openradar.me/23410569
        if (sourceFileMatch.getFileType().getClass() == XcodeXliffFileType.class) {
            assetContent = commandHelper.setPreserveSpaceInXliff(assetContent);
        }

        LocalizedAssetBody localizedAsset = assetClient.getLocalizedAssetForContent(
                assetByPathAndRepositoryId.getId(),
                repositoryLocale.getLocale().getId(),
                assetContent,
                outputBcp47tag,
                sourceFileMatch.getFileType().getFilterConfigIdOverride(),
                filterOptions,
                status,
                inheritanceMode
        );

        logger.trace("LocalizedAsset content = {}", localizedAsset.getContent());

        return localizedAsset;
    }

    private void initLocaleFullyTranslatedMap(Repository repository) {
        RepositoryStatistic repoStat = repository.getRepositoryStatistic();
        if (repoStat != null) {
            for (RepositoryLocaleStatistic repoLocaleStat : repoStat.getRepositoryLocaleStatistics()) {
                localeFullyTranslated.put(repoLocaleStat.getLocale().getBcp47Tag(), repoLocaleStat.getForTranslationCount() == 0L);
            }
        }
    }

    private boolean shouldGenerateLocalizedFile(RepositoryLocale repositoryLocale) {
        boolean localize = true;

        if (onlyIfFullyTranslated) {
            if (repositoryLocale.isToBeFullyTranslated()) {
                localize = localeFullyTranslated.get(repositoryLocale.getLocale().getBcp47Tag());
            } else {
                localize = localeFullyTranslated.get(repositoryLocale.getParentLocale().getLocale().getBcp47Tag());
            }
        }

        return localize;
    }

}
