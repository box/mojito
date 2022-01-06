package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.checks.AbstractCliChecker;
import com.box.l10n.mojito.cli.command.checks.CheckerOptionsMapEntryConverter;
import com.box.l10n.mojito.cli.command.checks.CheckerOptionsMapEntry;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliCheckerExecutor;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.CliCheckerType;
import com.box.l10n.mojito.cli.command.checks.CliCheckerTypeConverter;
import com.box.l10n.mojito.cli.command.checks.PlaceholderRegularExpressionConverter;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.phabricator.PhabricatorMessageBuilder;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command to execute checks against any new source strings
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"phab-extraction-check"}, commandDescription = "Execute checks against new source strings")
public class PhabricatorExtractionCheckCommand extends Command {

    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommand.class);

    protected static final String SKIP_I18N_CHECKS_FLAG = "SKIP_I18N_CHECKS";
    protected static final String PHAB_OVERRIDE_MESSAGE = PhabricatorIcon.WARNING + " **WARNING**" + System.lineSeparator() + System.lineSeparator() + "I18N source string checks have been disabled by override, " +
            "if this was not intentional please remove the '**" + SKIP_I18N_CHECKS_FLAG + "**' string from your Phabricator differential test plan and re-trigger a build.";

    @Autowired
    ExtractionDiffService extractionDiffService;

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired(required = false)
    PhabricatorMessageBuilder phabricatorMessageBuilder;

    @Autowired(required = false)
    DifferentialRevision differentialRevision;

    @Parameter(names = {"--checker-list", "-cl"}, variableArity = true, required = true, converter = CliCheckerTypeConverter.class, description = "List of checks to be run against new source strings")
    List<CliCheckerType> checkerList;

    @Parameter(names = {"--hard-fail", "-hf"}, variableArity = true, required = false, description = "List of checks that will cause a hard failure, use ALL if all checks should be hard failures")
    List<String> hardFailList = new ArrayList<>();

    @Parameter(names = {"--parameter-regexes", "-pr"}, variableArity = true, required = false, converter = PlaceholderRegularExpressionConverter.class, description = "Regex types used to identify parameters in source strings")
    List<PlaceholderRegularExpressions> parameterRegexList = new ArrayList<>();

    @Parameter(names = {"--name", "-n"}, arity = 1, required = false, description = ExtractionDiffCommand.EXCTRACTION_DIFF_NAME_DESCRIPTION)
    String extractionDiffName = null;

    @Parameter(names = {"--objectid", "-oid"}, arity = 1, required = false, description = "Phabricator object id, required if adding a comment with check failures to Phabricator.")
    String phabObjectId = null;

    @Parameter(names = {"--phab-message-template", "-pmt"}, arity = 1, required = false, description = "Optional message template to customize the Phabricator notification message. eg. '{baseMessage}. Check [[https://build.org/1234|build]].' ")
    String messageTemplate = "{baseMessage}";

    @Parameter(names = {"--current", "-c"}, arity = 1, required = true, description = ExtractionDiffCommand.CURRENT_EXTRACTION_NAME_DESCRIPTION)
    String currentExtractionName;

    @Parameter(names = {"--base", "-b"}, arity = 1, required = true, description = ExtractionDiffCommand.BASE_EXTRACTION_NAME_DESCRIPTION)
    String baseExtractionName;

    @Parameter(names = {"--output-directory", "-o"}, arity = 1, required = false, description = ExtractionDiffCommand.OUTPUT_DIRECTORY_DESCRIPTION)
    String outputDirectoryParam = ExtractionDiffPaths.DEFAULT_OUTPUT_DIRECTORY;

    @Parameter(names = {"--input-directory", "-i"}, arity = 1, required = false, description = ExtractionDiffCommand.INPUT_DIRECTORY_DESCRIPTION)
    String inputDirectoryParam = ExtractionPaths.DEFAULT_OUTPUT_DIRECTORY;

    @Parameter(names = {"--checker-options", "-co"}, variableArity = true, required = false, converter = CheckerOptionsMapEntryConverter.class, description = "Map of options to be used in individual checkers. Options must be in the form 'name':'value'.")
    List<CheckerOptionsMapEntry> checkerOptions = new ArrayList<>();

    @Override
    protected void execute() throws CommandException {

        checkPhabricatorPreconditions();

        if (areChecksSkipped()) {
            consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("Checks disabled as '" + SKIP_I18N_CHECKS_FLAG + "' string is present in Phabricator test plan.");
            differentialRevision.addComment(phabObjectId, PHAB_OVERRIDE_MESSAGE);
        } else {
            ExtractionPaths baseExtractionPaths = new ExtractionPaths(inputDirectoryParam, baseExtractionName);
            ExtractionPaths currentExtractionPaths = new ExtractionPaths(inputDirectoryParam, currentExtractionName);
            ExtractionDiffPaths extractionDiffPaths = ExtractionDiffPaths.builder()
                    .outputDirectory(outputDirectoryParam)
                    .diffExtractionName(extractionDiffName)
                    .baseExtractorPaths(baseExtractionPaths)
                    .currentExtractorPaths(currentExtractionPaths)
                    .build();

            List<AssetExtractionDiff> assetExtractionDiffs;

            try {
                if (extractionDiffService.hasAddedTextUnits(extractionDiffPaths)) {
                    assetExtractionDiffs = extractionDiffService.findAssetExtractionDiffsWithAddedTextUnits(extractionDiffPaths);
                    CliCheckerExecutor cliCheckerExecutor = getCliCheckerExecutor();
                    List<CliCheckResult> cliCheckerResults = executeChecks(cliCheckerExecutor, assetExtractionDiffs);
                    checkForHardFail(cliCheckerResults);
                    if (!cliCheckerResults.isEmpty()) {
                        List<CliCheckResult> failures = getCheckerFailures(cliCheckerResults).collect(Collectors.toList());
                        outputFailuresToCommandLine(failures);
                        if (StringUtils.isNotBlank(phabObjectId)) {
                            addCommentToPhabricatorRevision(failures);
                        }
                    }
                } else {
                    consoleWriter.newLine().a("No new strings in diff to be checked.").println();
                }
            } catch (MissingExtractionDirectoryExcpetion missingExtractionDirectoryException) {
                throw new CommandException("Can't compute extraction diffs", missingExtractionDirectoryException);
            }
            consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Checks completed").println(2);
        }

    }

    protected boolean areChecksSkipped() {
        return StringUtils.isNotBlank(phabObjectId) && differentialRevision.getTestPlan(phabObjectId).contains(SKIP_I18N_CHECKS_FLAG);
    }

    private void addCommentToPhabricatorRevision(List<CliCheckResult> cliCheckerFailures) {
        consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("Adding comment to Phabricator revision " + phabObjectId).println();
        differentialRevision.addComment(phabObjectId, getPhabricatorMessage(buildPhabricatorComment(cliCheckerFailures)));
    }

    private void checkForHardFail(List<CliCheckResult> results) {
        if (getCheckerHardFailures(results).count() > 0) {
            String hardFailureListString = "The following checks had hard failures:" + System.lineSeparator() +
                    getCheckerHardFailures(results).map(failure -> failure.getCheckName() + " failures: " + System.lineSeparator() + failure.getNotificationText() + System.lineSeparator())
                            .collect(Collectors.joining(System.lineSeparator()));
            logger.debug(hardFailureListString);
            throw new CommandException(hardFailureListString);
        }
    }

    private List<CliCheckResult> executeChecks(CliCheckerExecutor cliCheckerExecutor, List<AssetExtractionDiff> assetExtractionDiffs) {
        consoleWriter.newLine().a("Running checks against new strings").println();
        return cliCheckerExecutor.executeChecks(assetExtractionDiffs).stream()
                .filter(result -> !result.isSuccessful())
                .collect(Collectors.toList());
    }

    private Stream<CliCheckResult> getCheckerFailures(List<CliCheckResult> results) {
        return results.stream().filter(result -> !result.isSuccessful());
    }

    private Stream<CliCheckResult> getCheckerHardFailures(List<CliCheckResult> results) {
        return getCheckerFailures(results).filter(CliCheckResult::isHardFail);
    }

    String getPhabricatorMessage(String notificationText) {

        ImmutableMap<String, Object> messageParamMap = ImmutableMap.<String, Object>builder()
                .put("icon", PhabricatorIcon.WARNING.toString())
                .put("header", "**i18n source string checks failed**")
                .put("notificationText", notificationText)
                .build();
        String message = "{icon} {header}" + System.lineSeparator() + System.lineSeparator() + "{notificationText}";
        return phabricatorMessageBuilder.getFormattedPhabricatorMessage(messageTemplate, "baseMessage", phabricatorMessageBuilder.getBaseMessage(messageParamMap, message));
    }

    private String buildPhabricatorComment(List<CliCheckResult> failedCheck) {
        String notification = failedCheck.stream().map(check -> "//**" + check.getCheckName() + "**//" + System.lineSeparator()).collect(Collectors.joining(System.lineSeparator()));
        return "**Failed checks:**" + addDoubleNewLine() + notification + addDoubleNewLine() + "**Please correct the above issues in a new commit.**";
    }

    private String addDoubleNewLine() {
        return System.lineSeparator() + System.lineSeparator();
    }

    private void checkPhabricatorPreconditions() {
        if (phabObjectId != null) {
            PhabricatorPreconditions.checkNotNull(differentialRevision);
        }
    }

    private CliCheckerExecutor getCliCheckerExecutor() {
        return new CliCheckerExecutor(generateCheckerList(generateCheckerOptions()));
    }

    private void outputFailuresToCommandLine(List<CliCheckResult> failedCheckNames) {
        consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("Failed checks: ").println();
        failedCheckNames.stream().forEach(check -> {
            consoleWriter.fg(Ansi.Color.YELLOW).newLine().a(check.getCheckName()).println();
            consoleWriter.fg(Ansi.Color.YELLOW).newLine().a(check.getNotificationText().replaceAll("\\*", "\t*")).println();
        });
    }

    private CliCheckerOptions generateCheckerOptions() {
        Set<PlaceholderRegularExpressions> regexSet = Sets.newHashSet(parameterRegexList);
        Set<CliCheckerType> hardFailureSet = getClassNamesOfHardFailures();
        return new CliCheckerOptions(regexSet, hardFailureSet, buildOptionsMap());
    }

    private ImmutableMap<String, String> buildOptionsMap() {
        return ImmutableMap.<String, String>builder().putAll(checkerOptions.stream()
                .collect(Collectors.toMap(CheckerOptionsMapEntry::getKey, CheckerOptionsMapEntry::getValue)))
                .build();
    }

    private Set<CliCheckerType> getClassNamesOfHardFailures() {
        if (hardFailList.stream().anyMatch(checkName -> "ALL".equalsIgnoreCase(checkName))) {
            return Stream.of(CliCheckerType.values()).collect(Collectors.toSet());
        } else {
            return hardFailList.stream().map(check -> {
                try {
                    return CliCheckerType.valueOf(check);
                } catch (IllegalArgumentException e) {
                    throw new CommandException("Unknown check name in hard fail list '" + check + "'");
                }
            }).collect(Collectors.toSet());
        }
    }

    private List<AbstractCliChecker> generateCheckerList(CliCheckerOptions checkerOptions) {
        List<AbstractCliChecker> checkers = new ArrayList<>();
        for (CliCheckerType checkerType : checkerList) {
            AbstractCliChecker checker = checkerType.getCliChecker();
            checker.setCliCheckerOptions(checkerOptions);
            checkers.add(checker);
        }

        return checkers;
    }

}
