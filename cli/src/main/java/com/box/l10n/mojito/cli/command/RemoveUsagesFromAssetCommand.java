package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.striplocation.UsagesFromAssetRemover;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"remove-usages"}, commandDescription = "Removes string usages/location information from asset (source or localized")
public class RemoveUsagesFromAssetCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RemoveUsagesFromAssetCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.TARGET_DIRECTORY_DESCRIPTION)
    String targetDirectoryParam;

    @Parameter(names = {"--output-directory", "-o"}, arity = 1, required = false, description = "Output directory")
    String outputDirectoryParam;

    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;

    @Parameter(names = {Param.FILTER_OPTIONS_LONG, Param.FILTER_OPTIONS_SHORT}, variableArity = true, required = false, description = Param.FILTER_OPTIONS_DESCRIPTION)
    List<String> filterOptionsParam;

    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;

    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    UsagesFromAssetRemover usagesFromAssetRemover;

    CommandDirectories commandDirectories;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Strip location from files: ").fg(Ansi.Color.CYAN).println(2);

        commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

        for (FileMatch sourceFileMatch : commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex)) {
            List<String> filterOptions = commandHelper.getFilterOptionsOrDefaults(sourceFileMatch.getFileType(), filterOptionsParam);
            String assetContentWithoutUsages = getAssetContentWithoutUsages(sourceFileMatch, filterOptions);
            writeAssetContentToTargetDirectory(assetContentWithoutUsages, sourceFileMatch);
        }

        // TODO yeah actually this is not simple to do with the reuglar filter ... will do later eventually
//        for (FileMatch targetFileMatch : commandHelper.getTargetFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex)) {
//            List<String> filterOptions = commandHelper.getFilterOptionsOrDefaults(targetFileMatch.getFileType(), filterOptionsParam);
//            String assetContentWithoutUsages = getAssetContentWithoutUsages(targetFileMatch, filterOptions);
//            writeAssetContentToTargetDirectory(assetContentWithoutUsages, targetFileMatch);
//        }

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    String getAssetContentWithoutUsages(FileMatch sourceFileMatch, List<String> filterOptions, String targetLocale) {
        String sourcePath = sourceFileMatch.getSourcePath();
        String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);
        FilterConfigIdOverride filterConfigIdOverride = sourceFileMatch.getFileType().getFilterConfigIdOverride();

        try {
            return usagesFromAssetRemover.removeUsages(sourcePath, assetContent, filterConfigIdOverride, filterOptions, filem);
        } catch (UnsupportedAssetFilterTypeException uasft) {
            throw new RuntimeException("Source file match must be for a supported file type", uasft);
        }
    }

    void writeAssetContentToTargetDirectory(String assetContent, FileMatch sourceFileMatch) throws CommandException {
        Path relativize = commandDirectories.getSourceDirectoryPath().relativize(sourceFileMatch.getPath());
        Path targetPath = Paths.get(outputDirectoryParam).resolve(relativize);
        commandHelper.writeFileContent(assetContent, targetPath, sourceFileMatch);
        Path relativeTargetFilePath = commandDirectories.relativizeWithUserDirectory(targetPath);
        consoleWriter.a(" --> ").fg(Ansi.Color.MAGENTA).a(relativeTargetFilePath.toString()).println();
    }
}
