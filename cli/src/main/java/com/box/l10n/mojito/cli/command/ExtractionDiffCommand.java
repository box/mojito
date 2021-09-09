package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.box.l10n.mojito.shell.Shell;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Computes a diff between to local extractions.
 * <p>
 * The diff is stored in a JSON file. It shows newly added and removed text units and files.
 * <p>
 * This can be used to check if there are new text units added in a commit/branch and then trigger the push command
 * only when needed. Script like this:
 * <p>
 * <p>
 * BASE_COMMIT=85e5c705b2
 * CURRENT_COMMIT=dfcb732e8a
 * BRANCH_NAME=D32904
 * BRANCH_CREATEDBY=aurambaj
 * <p>
 * git reset --hard ${BASE_COMMIT}
 * dmojito extract -n ${BASE_COMMIT}
 * <p>
 * git reset --hard ${CURRENT_COMMIT}
 * mojito extract -n ${CURRENT_COMMIT}
 * <p>
 * mojito extract-diff -c ${CURRENT_COMMIT} -b ${BASE_COMMIT} --push-to testrepo --push-to-branch ${BRANCH_NAME}
 * --push-to-branch-createdby ${BRANCH_CREATEDBY}
 * <p>
 * Phabricator integrations:
 * BRANCH_NAME="D$(mojito phabricator-get-revision-id --target-phid ${HARBORMASTER_BUILD_TARGET_PHID})"
 * <p>
 * Get username from git log, eg:
 * BRANCH_CREATEDBY=$(git log --format='%ae' HEAD^\!)
 * BRANCH_CREATEDBY=${BRANCH_CREATOR%%"@somemail.com"}
 * <p>
 * Get diff info from phabricator diff, {@see com.box.l10n.mojito.cli.command.PhabricatorDiffInfoCommand}
 * source <(mojito phab-diff-to-env-variables --diff-id 12345)
 * echo ${MOJITO_PHAB_BASE_COMMIT}
 * ...
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"extract-diff"}, commandDescription = "Generate a diff between 2 local extractions")
public class ExtractionDiffCommand extends Command {

    public static final String EXCTRACTION_DIFF_NAME_DESCRIPTION = "Name of the directory that will contain the diff, if not provided it will be {currentExtractionName}_{baseExtractionName}";
    public static final String OUTPUT_DIRECTORY_DESCRIPTION = "The output directory of the extraction diff (default " + ExtractionDiffPaths.DEFAULT_OUTPUT_DIRECTORY + ")";
    public static final String INPUT_DIRECTORY_DESCRIPTION = "The input directory that contains the extractions (default " + ExtractionPaths.DEFAULT_OUTPUT_DIRECTORY + ")";
    public static final String CURRENT_EXTRACTION_NAME_DESCRIPTION = "The current extraction name";
    public static final String BASE_EXTRACTION_NAME_DESCRIPTION = "The base extraction name";

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {"--current", "-c"}, arity = 1, required = true, description = CURRENT_EXTRACTION_NAME_DESCRIPTION)
    String currentExtractionName;

    @Parameter(names = {"--base", "-b"}, arity = 1, required = true, description = BASE_EXTRACTION_NAME_DESCRIPTION)
    String baseExtractionName;

    @Parameter(names = {"--name", "-n"}, arity = 1, required = false, description = EXCTRACTION_DIFF_NAME_DESCRIPTION)
    String extractionDiffName = null;

    @Parameter(names = {"--output-directory", "-o"}, arity = 1, required = false, description = OUTPUT_DIRECTORY_DESCRIPTION)
    String outputDirectoryParam = ExtractionDiffPaths.DEFAULT_OUTPUT_DIRECTORY;

    @Parameter(names = {"--input-directory", "-i"}, arity = 1, required = false, description = INPUT_DIRECTORY_DESCRIPTION)
    String inputDirectoryParam = ExtractionPaths.DEFAULT_OUTPUT_DIRECTORY;

    @Parameter(names = {"--push-to", "-p"}, arity = 1, required = false, description = "Push to the specified repository if there are added text units in the diff")
    String pushToRepository = null;

    @Parameter(names = {"--push-to-fallback", "-pf"}, arity = 1, required = false, hidden = true, description = "Optional repository name to use when the primary pushToRepository repository isn't available. Useful during repo renames and migrations.")
    String pushToRepositoryFallback = null;

    @Parameter(names = {"--push-to-branch", "-pb"}, arity = 1, required = false, description = "Optional branch name when pushing to a repository")
    String pushToBranchName;

    @Parameter(names = {"--push-to-branch-createdby", "-pbc"}, arity = 1, required = false, description = "Optional username who owns the branch when pusing to a repository")
    String pushToBranchCreatedBy;

    @Parameter(names = Param.PUSH_TYPE_LONG, arity = 1, required = false, description = Param.PUSH_TYPE_DESCRIPTION)
    PushService.PushType pushType = PushService.PushType.NORMAL;

    @Parameter(names = "--fail-safe", arity = 1, required = false, description = "To fail safe, the command will exit succesfuly even if the processing failed")
    boolean failSafe = false;

    @Parameter(names = "--fail-safe-email", arity = 1, required = false, description = "Attempt to notify this email if the command fails")
    String failSafeEmail = null;

    @Parameter(names = "--fail-safe-message", arity = 1, required = false, description = "Message for the fail safe email")
    String failSafeMessage = null;

    @Autowired
    ExtractionDiffService extractionDiffService;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    PushService pushService;

    @Override
    public void execute() throws CommandException {
        try {
            executeUnsafe();
        } catch (Throwable t) {
            if (failSafe) {
                failSafe(t);
            } else {
                throw t;
            }
        }
    }

    void executeUnsafe() throws CommandException {
        consoleWriter.newLine().a("Generate diff between extractions: ").fg(Ansi.Color.CYAN).a(currentExtractionName).reset()
                .a(" and: ").fg(Ansi.Color.CYAN).a(baseExtractionName).println(2);

        ExtractionPaths baseExtractionPaths = new ExtractionPaths(inputDirectoryParam, baseExtractionName);
        ExtractionPaths currentExtractionPaths = new ExtractionPaths(inputDirectoryParam, currentExtractionName);
        ExtractionDiffPaths extractionDiffPaths = ExtractionDiffPaths.builder()
                .outputDirectory(outputDirectoryParam)
                .diffExtractionName(extractionDiffName)
                .baseExtractorPaths(baseExtractionPaths)
                .currentExtractorPaths(currentExtractionPaths)
                .build();

        try {
            extractionDiffService.computeAndWriteDiffs(extractionDiffPaths);
        } catch (MissingExtractionDirectoryExcpetion msobe) {
            throw new CommandException(msobe.getMessage());
        }

        if (pushToRepository != null) {
            boolean hasAddedTextUnits = extractionDiffService.hasAddedTextUnits(extractionDiffPaths);

            if (!hasAddedTextUnits) {
                consoleWriter.a("The diff is empty, don't push to repository: ").fg(Ansi.Color.CYAN).a(pushToRepository).println();
            } else {
                pushToRepository = getValidRepositoryName();
                consoleWriter.a("Push asset diffs to repository: ").fg(Ansi.Color.CYAN).a(pushToRepository).println(2);
                pushToRepository(extractionDiffPaths);
            }
        }

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    private String getValidRepositoryName() {
        List<Repository> repositories = commandHelper.getAllRepositories();

        if (repositories.stream().anyMatch(repository -> repository.getName().equals(pushToRepository))) {
            return pushToRepository;
        }

        if (repositories.stream().anyMatch(repository -> repository.getName().equals(pushToRepositoryFallback))) {
            return pushToRepositoryFallback;
        }

        throw new CommandException("Could not find a valid repository for the name provided. pushToRepository = " + pushToRepository + " pushToRepositoryFallback = " + pushToRepositoryFallback);
    }

    void pushToRepository(ExtractionDiffPaths extractionDiffPaths) throws CommandException {

        Repository repository = commandHelper.findRepositoryByName(pushToRepository);

        Stream<Path> allAssetExtractionDiffPaths = extractionDiffPaths.findAllAssetExtractionDiffPaths();

        Stream<SourceAsset> sourceAssetStream = allAssetExtractionDiffPaths.map(path -> {

            AssetExtractionDiff assetExtractionDiff = objectMapper.readValueUnchecked(path.toFile(), AssetExtractionDiff.class);

            SourceAsset sourceAsset = null;

            if (!assetExtractionDiff.getAddedTextunits().isEmpty()) {
                String assetContent = objectMapper.writeValueAsStringUnchecked(assetExtractionDiff.getAddedTextunits());

                String sourceFileMatchPath = extractionDiffPaths.sourceFileMatchPath(path);

                sourceAsset = new SourceAsset();
                sourceAsset.setBranch(pushToBranchName);
                sourceAsset.setBranchCreatedByUsername(pushToBranchCreatedBy);
                sourceAsset.setPath(sourceFileMatchPath);
                sourceAsset.setContent(assetContent);
                sourceAsset.setExtractedContent(true);
                sourceAsset.setRepositoryId(repository.getId());
                sourceAsset.setFilterConfigIdOverride(assetExtractionDiff.getCurrentFilterConfigIdOverride());
                sourceAsset.setFilterOptions(assetExtractionDiff.getCurrentFilterOptions());
            }

            return sourceAsset;
        }).filter(Objects::nonNull);

        pushService.push(repository, sourceAssetStream, pushToBranchName, pushType);
    }

    void failSafe(Throwable t) {
        String msg = "Unexpected error: " + t.getMessage() + "\n" + ExceptionUtils.getStackTrace(t);
        consoleWriter.newLine().fg(Ansi.Color.YELLOW).a(msg).println(2);
        logger.error("Unexpected error", t);
        consoleWriter.fg(Ansi.Color.GREEN).a("Failing safe...").println();
        tryToSendFailSafeNotification();

    }

    void tryToSendFailSafeNotification() {
        try {
            if (failSafeEmail != null) {
                consoleWriter.a("Send email notification to: ").fg(Ansi.Color.CYAN).a(failSafeEmail).println();
                Shell shell = new Shell();
                shell.exec(buildFailSafeMailCommand());
            }
        } catch (Throwable t) {
            String cantSendEmailMsg = "Can't send fail safe email: " + t.getMessage() + "\n" + ExceptionUtils.getStackTrace(t);
            consoleWriter.newLine().fg(Ansi.Color.RED).a(cantSendEmailMsg).println(2);
            logger.error("Unexpected error", t);
        }
    }

    String buildFailSafeMailCommand() {
        return "echo '" + failSafeMessage + "' | mail -s 'Extraction diff command failed for branch: " + pushToBranchName + "' " + failSafeEmail;
    }
}
