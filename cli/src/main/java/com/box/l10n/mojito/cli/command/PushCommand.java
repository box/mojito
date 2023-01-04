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
import com.ibm.icu.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** @author jaurambault */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"push", "p"},
    commandDescription = "Push assets to be localized to TMS")
public class PushCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(PushCommand.class);

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
      names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT},
      arity = 1,
      required = false,
      description = Param.SOURCE_REGEX_DESCRIPTION)
  String sourcePathFilterRegex;

  @Parameter(
      names = {Param.BRANCH_LONG, Param.BRANCH_SHORT},
      arity = 1,
      required = false,
      description = Param.BRANCH_DESCRIPTION)
  String branchName;

  @Parameter(
      names = {"--branch-createdby", "-bc"},
      arity = 1,
      required = false,
      description = "username of text unit author")
  String branchCreatedBy;

  @Parameter(
      names = {"--branch-notifiers", "-bn"},
      variableArity = true,
      required = false,
      description = "Optional list of notifiers when pusing to a repository")
  Set<String> pushToBranchNotifiers = Collections.emptySet();

  @Parameter(
      names = Param.PUSH_TYPE_LONG,
      arity = 1,
      required = false,
      description = Param.PUSH_TYPE_DESCRIPTION)
  PushService.PushType pushType = PushService.PushType.NORMAL;

  @Parameter(
      names = {Param.COMMIT_HASH_LONG, Param.COMMIT_HASH_SHORT},
      arity = 1,
      required = false,
      description = Param.COMMIT_HASH_DESCRIPTION)
  String commitHash;

  @Parameter(
      names = {Param.RECORD_PUSH_RUN_LONG, Param.RECORD_PUSH_RUN_SHORT},
      required = false,
      description = Param.RECORD_PUSH_RUN_DESCRIPTION)
  Boolean recordPushRun = false;

  @Parameter(
      names = {"--asset-mapping", "-am"},
      required = false,
      description = "Asset mapping, format: \"local1:remote1;local2:remote2\"",
      converter = AssetMappingConverter.class)
  Map<String, String> assetMapping;

  @Autowired RepositoryClient repositoryClient;

  @Autowired CommandHelper commandHelper;

  @Autowired PushService pushService;

  CommandDirectories commandDirectories;

  @Override
  public void execute() throws CommandException {

    commandDirectories = new CommandDirectories(sourceDirectoryParam);

    consoleWriter
        .newLine()
        .a("Push assets to repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    Repository repository = commandHelper.findRepositoryByName(repositoryParam);

    if (commitHash != null && StringUtils.isBlank(commitHash)) {
      throw new CommandException(
          MessageFormat.format(
              "The value provided with the {0} parameter can not be blank/whitespace.",
              Param.COMMIT_HASH_LONG));
    }

    if (commitHash != null && !recordPushRun) {
      throw new CommandException(
          MessageFormat.format(
              "Whenever {0} is provided, {1} must also be passed in as true.",
              Param.COMMIT_HASH_LONG, Param.RECORD_PUSH_RUN_LONG));
    }

    if (recordPushRun && commitHash == null) {
      throw new CommandException(
          MessageFormat.format(
              "Whenever {0} is set to true, {1} must also be passed in with a commit hash.",
              Param.RECORD_PUSH_RUN_LONG, Param.COMMIT_HASH_LONG));
    }

    if (recordPushRun && pushType == PushService.PushType.SEND_ASSET_NO_WAIT_NO_DELETE) {
      throw new IllegalArgumentException(
          MessageFormat.format(
              "The SEND_ASSET_NO_WAIT_NO_DELETE push type can not be used in conjunction with {0} and {1} currently.",
              Param.COMMIT_HASH_LONG, Param.RECORD_PUSH_RUN_LONG));
    }

    String pushRunName = UUID.randomUUID().toString();
    ArrayList<FileMatch> sourceFileMatches =
        commandHelper.getSourceFileMatches(
            commandDirectories,
            fileTypes,
            sourceLocale,
            sourcePathFilterRegex,
            directoriesIncludePatterns,
            directoriesExcludePatterns);

    Stream<SourceAsset> sourceAssetStream =
        sourceFileMatches.stream()
            .sorted(Comparator.comparing(FileMatch::getSourcePath))
            .map(
                sourceFileMatch -> {
                  String sourcePath = sourceFileMatch.getSourcePath();

                  String assetContent = commandHelper.getFileContentWithXcodePatch(sourceFileMatch);
                  SourceAsset sourceAsset = new SourceAsset();
                  sourceAsset.setBranch(branchName);
                  sourceAsset.setBranchCreatedByUsername(branchCreatedBy);
                  sourceAsset.setBranchNotifiers(pushToBranchNotifiers);
                  sourceAsset.setPath(commandHelper.getMappedSourcePath(assetMapping, sourcePath));
                  sourceAsset.setContent(assetContent);
                  sourceAsset.setExtractedContent(false);
                  sourceAsset.setRepositoryId(repository.getId());
                  sourceAsset.setPushRunName(recordPushRun ? pushRunName : null);
                  sourceAsset.setFilterConfigIdOverride(
                      sourceFileMatch.getFileType().getFilterConfigIdOverride());
                  sourceAsset.setFilterOptions(
                      commandHelper.getFilterOptionsOrDefaults(
                          sourceFileMatch.getFileType(), filterOptionsParam));

                  return sourceAsset;
                });

    pushService.push(repository, sourceAssetStream, branchName, pushType);
    pushService.associatePushRun(repository, pushRunName, commitHash);

    consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
  }
}
