package com.box.l10n.mojito.cli.command;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import static com.box.l10n.mojito.cli.command.PseudoLocCommand.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.fusesource.jansi.Ansi.Color;

@Component
@Scope("prototype")
@Parameters(commandNames = {"pseudo", "pl"}, commandDescription = "Pull pseudo localized assets from TMS")
public class PseudoLocCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PseudoLocCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.TARGET_DIRECTORY_DESCRIPTION)
    String targetDirectoryParam;

    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;

    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;

    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;

    @Autowired
    AssetClient assetClient;

    @Autowired
    CommandHelper commandHelper;

    Repository repository;

    CommandDirectories commandDirectories;

    public static final String OUTPUT_BCP47_TAG = "en-x-pseudo";

    /**
     * Contains a map of locale for generating localized file a locales defined
     * in the repository.
     */
    Map<String, String> localeMappings;

    @Override
    public void execute() throws CommandException {
        consoleWriter.newLine().a("Pull pseudo localized asset from repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

        repository = commandHelper.findRepositoryByName(repositoryParam);

        commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

        for (FileMatch sourceFileMatch : commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex)) {
            consoleWriter.a("Localizing: ").fg(Color.CYAN).a(sourceFileMatch.getSourcePath()).println();
            generatePseudoLocalizedFile(repository, sourceFileMatch);
        }
        consoleWriter.fg(Color.GREEN).newLine().a("Finished").println(2);
    }

    /**
     * Generates the pseudo localized file
     *
     * @param repository
     * @param sourceFileMatch
     * @throws CommandException
     */
    void generatePseudoLocalizedFile(Repository repository, FileMatch sourceFileMatch) throws CommandException {
        logger.debug("Generate pseudo localzied files");

        LocalizedAssetBody localizedAsset = getPseudoLocalizedAsset(repository, sourceFileMatch);
        writePseudoLocalizedAssetToTargetDirectory(localizedAsset, sourceFileMatch);

    }

    void writePseudoLocalizedAssetToTargetDirectory(LocalizedAssetBody localizedAsset, FileMatch sourceFileMatch) throws CommandException {
        localizedAsset.setBcp47Tag(OUTPUT_BCP47_TAG);

        Path targetPath = commandDirectories.getTargetDirectoryPath().resolve(sourceFileMatch.getTargetPath(localizedAsset.getBcp47Tag()));

        commandHelper.writeFileContent(localizedAsset.getContent(), targetPath, sourceFileMatch);

        Path relativeTargetFilePath = commandDirectories.relativizeWithUserDirectory(targetPath);

        consoleWriter.a(" --> ").fg(Color.MAGENTA).a(relativeTargetFilePath.toString()).println();
    }

    LocalizedAssetBody getPseudoLocalizedAsset(Repository repository, FileMatch sourceFileMatch) throws CommandException {
        consoleWriter.a(" - Processing locale: ").fg(Color.CYAN).a(OUTPUT_BCP47_TAG).print();

        try {
            Asset assetByPathAndRepositoryId = assetClient.getAssetByPathAndRepositoryId(sourceFileMatch.getSourcePath(), repository.getId());

            String assetContent = commandHelper.getFileContent(sourceFileMatch.getPath());

            // TODO(P1) This is to inject xml:space="preserve" in the trans-unit element
            // in the xcode-generated xliff until xcode fixes the bug of not adding this attribute
            // See Xcode bug http://www.openradar.me/23410569
            if (sourceFileMatch.getFileType().getClass() == XcodeXliffFileType.class) {
                assetContent = commandHelper.setPreserveSpaceInXliff(assetContent);
            }

            LocalizedAssetBody pseudoLocalizedAsset = assetClient.getPseudoLocalizedAssetForContent(
                    assetByPathAndRepositoryId.getId(),
                    assetContent,
                    sourceFileMatch.getFileType().getFilterConfigIdOverride());

            logger.trace("PseudoLocalizedAsset content = {}", pseudoLocalizedAsset.getContent());
            return pseudoLocalizedAsset;
        } catch (AssetNotFoundException e) {
            throw new CommandException("Asset with path [" + sourceFileMatch.getSourcePath() + "] was not found in repo [" + repositoryParam + "]", e);
        }
    }
}
