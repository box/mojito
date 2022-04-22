package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"push", "p"}, commandDescription = "Push assets to be localized to TMS")
public class PushCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PushCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;

    @Parameter(names = {Param.FILTER_OPTIONS_LONG, Param.FILTER_OPTIONS_SHORT}, variableArity = true, required = false, description = Param.FILTER_OPTIONS_DESCRIPTION)
    List<String> filterOptionsParam;

    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;

    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;

    @Parameter(names = {Param.BRANCH_LONG, Param.BRANCH_SHORT}, arity = 1, required = false, description = Param.BRANCH_DESCRIPTION)
    String branchName;

    @Parameter(names = {"--branch-createdby", "-bc"}, arity = 1, required = false, description = "username of text unit author")
    String branchCreatedBy;

    @Parameter(names = Param.PUSH_TYPE_LONG, arity = 1, required = false, description = Param.PUSH_TYPE_DESCRIPTION)
    PushService.PushType pushType = PushService.PushType.NORMAL;

    @Parameter(names = {"--asset-mapping", "-am"}, required = false, description = "Asset mapping, format: \"local1:remote1;local2:remote2\"", converter = AssetMappingConverter.class)
    Map<String, String> assetMapping;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    PushService pushService;

    CommandDirectories commandDirectories;

    @Override
    public void execute() throws CommandException {

        commandDirectories = new CommandDirectories(sourceDirectoryParam);

        consoleWriter.newLine().a("Push assets to repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        ArrayList<FileMatch> sourceFileMatches = commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex);

        Stream<SourceAsset> sourceAssetStream = sourceFileMatches.stream()
                .sorted(Comparator.comparing(FileMatch::getSourcePath))
                .map(sourceFileMatch -> {
                    String sourcePath = sourceFileMatch.getSourcePath();

                    String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);
                    SourceAsset sourceAsset = new SourceAsset();
                    sourceAsset.setBranch(branchName);
                    sourceAsset.setBranchCreatedByUsername(branchCreatedBy);
                    sourceAsset.setPath(commandHelper.getMappedSourcePath(assetMapping, sourcePath));
                    sourceAsset.setContent(assetContent);
                    sourceAsset.setExtractedContent(false);
                    sourceAsset.setRepositoryId(repository.getId());
                    sourceAsset.setFilterConfigIdOverride(sourceFileMatch.getFileType().getFilterConfigIdOverride());
                    sourceAsset.setFilterOptions(commandHelper.getFilterOptionsOrDefaults(sourceFileMatch.getFileType(), filterOptionsParam));

                    return sourceAsset;
                });

        pushService.push(repository, sourceAssetStream, branchName, pushType);

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }
}
