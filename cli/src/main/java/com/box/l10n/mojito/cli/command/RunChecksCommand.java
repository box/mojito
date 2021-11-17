package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.checks.CliCheckResult;
import com.box.l10n.mojito.cli.command.checks.CliChecker;
import com.box.l10n.mojito.cli.command.checks.CliCheckerExecutor;
import com.box.l10n.mojito.cli.command.checks.CliCheckerInstantiationException;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.CliCheckerType;
import com.box.l10n.mojito.cli.command.extraction.AbstractExtractionDiffStatistics;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.phabricator.DifferentialDiff;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.ibm.icu.text.MessageFormat;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.Strings.nullToEmpty;

/**
 * Command to execute checks against any new source strings
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"run-checks"}, commandDescription = "Execute checks against new source strings")
public class RunChecksCommand extends Command {

    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommand.class);

    @Autowired
    ExtractionDiffService extractionDiffService;

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired(required = false)
    DifferentialDiff differentialDiff;

    @Autowired(required = false)
    DifferentialRevision differentialRevision;

    @Parameter(names = {"--checker-list", "-cl"}, arity = 1, required = true, description = "List of checks to be run against new source strings")
    List<String> checkerList;

    @Parameter(names = {"--hard-fail", "-hf"}, arity = 1, required = false, description = "List of checks that will cause a hard failure, use ALL if all checks should be hard failures")
    List<String> hardFailList = new ArrayList<>();

    @Parameter(names = {"--parameter-regexes", "-pr"}, arity = 1, required = false, description = "Regex types used to identify parameters in source strings")
    List<String> parameterRegexList = new ArrayList<>();

    @Parameter(names = {"--dictionary-additions-file", "-daf"}, arity = 1, required = false, description = "Path to the dictionary additions file used for the spelling check")
    String dictionaryAdditionsFilePath = "";

    @Parameter(names = {"--glossary-file", "-gf"}, arity = 1, required = false, description = "Path to the glossary file used for the glossary check")
    String glossaryFilePath = "";

    @Parameter(names = {"--name", "-n"}, arity = 1, required = false, description = ExtractionDiffCommand.EXCTRACTION_DIFF_NAME_DESCRIPTION)
    String extractionDiffName = null;

    @Parameter(names = {"--phab-diff-id", "-pdid"}, arity = 1, required = false, description = "Phabricator diff id, required if adding a comment with check failures to a Phabricator diff.")
    String diffId = null;

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

    @Override
    protected void execute() throws CommandException {

        checkPhabricatorPreconditions();
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
                assetExtractionDiffs = extractionDiffService.computeAssetExtractionDiffsWithAddedTextUnits(extractionDiffPaths);
            } else {
                consoleWriter.newLine().a("No new strings in diff to be checked.").println();
                return;
            }
        } catch (MissingExtractionDirectoryExcpetion missingExtractionDirectoryException) {
            throw new CommandException("Can't compute extraction diffs", missingExtractionDirectoryException);
        }

        CliCheckerExecutor cliCheckerExecutor = getCliCheckerExecutor(assetExtractionDiffs);
        List<CliCheckResult> cliCheckerFailures = getCheckerFailures(cliCheckerExecutor);
        checkForHardFail(cliCheckerFailures);
        if(!cliCheckerFailures.isEmpty()) {
            outputFailuresToCommandLine(cliCheckerFailures);
            if (diffId != null) {
                String phabObjectId = "D" + differentialDiff.queryDiff(diffId).getRevisionId();
                consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("Adding comment to Phabricator revision " + phabObjectId).println();
                differentialRevision.addComment(phabObjectId, getMessage(buildPhabricatorComment(cliCheckerFailures)));
            }
        }
        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Checks completed").println(2);
    }

    private void checkForHardFail(List<CliCheckResult> results) {
        AtomicBoolean hardFail = new AtomicBoolean(false);
        StringBuilder hardFailureListString = new StringBuilder();
        hardFailureListString.append("The following checks had hard failures:" + System.lineSeparator());
        results.stream().filter(result -> !result.isSuccessful() && result.isHardFail()).forEach(failure -> {
            hardFail.set(true);
            hardFailureListString.append(failure.getCheckName() + " failures: " + System.lineSeparator());
            hardFailureListString.append(failure.getNotificationText());
            hardFailureListString.append(System.lineSeparator());
        });
        if (hardFail.get()) {
            logger.debug(hardFailureListString.toString());
            throw new CommandException(hardFailureListString.toString());
        }
    }

    private List<CliCheckResult> getCheckerFailures(CliCheckerExecutor cliCheckerExecutor) {
        return cliCheckerExecutor.executeChecks().stream()
                .filter(result -> !result.isSuccessful())
                .collect(Collectors.toList());
    }

    private String getBaseMessage(ImmutableMap<String, Object> arguments) {
        String header = "{icon} {header}" + System.lineSeparator() + System.lineSeparator() + "{notificationText}";
        MessageFormat messageFormat = new MessageFormat(header);
        String formatted = messageFormat.format(arguments);
        return formatted;
    }

    String getMessage(String notificationText) {

        ImmutableMap<String, Object> messageParamMap = ImmutableMap.<String, Object>builder()
                .put("icon", PhabricatorIcon.WARNING.toString())
                .put("header", "**i18n source string checks failed**")
                .put("notificationText", notificationText)
                .build();

        MessageFormat messageFormatForTemplate = new MessageFormat(messageTemplate);
        String formatted = messageFormatForTemplate.format(ImmutableMap.of("baseMessage", getBaseMessage(messageParamMap)));
        return formatted;
    }

    private String buildPhabricatorComment(List<CliCheckResult> failedCheck) {
        StringBuilder sb = new StringBuilder();
        sb.append("**Failed checks:**");
        addDoubleNewLine(sb);
        failedCheck.stream().forEach(check -> {
            sb.append("//**" + check.getCheckName() + "**//");
            addDoubleNewLine(sb);
            sb.append(check.getNotificationText());
        });
        addDoubleNewLine(sb);
        sb.append("**Please correct the above issues in a new commit.**");
        return sb.toString();
    }

    private void addDoubleNewLine(StringBuilder sb) {
        sb.append(System.lineSeparator() + System.lineSeparator());
    }

    private void checkPhabricatorPreconditions() {
        if (diffId != null) {
            PhabricatorPreconditions.checkNotNull(differentialDiff);
            PhabricatorPreconditions.checkNotNull(differentialRevision);
        }
    }

    private CliCheckerExecutor getCliCheckerExecutor(List<AssetExtractionDiff> assetExtractionDiffs) {
        CliCheckerExecutor cliCheckerExecutor = new CliCheckerExecutor(generateChecks(assetExtractionDiffs));
        consoleWriter.newLine().a("Running checks against new strings").println();
        return cliCheckerExecutor;
    }

    private void outputFailuresToCommandLine(List<CliCheckResult> failedCheckNames) {
        consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("Failed checks: ").println();
        failedCheckNames.stream().forEach(check -> {
            consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("\t* " + check.getCheckName()).println();
            consoleWriter.fg(Ansi.Color.YELLOW).newLine().a(indentAllLines(check.getNotificationText()).replaceAll("\\*", "\t\t*")).println();
        });
    }

    private String indentAllLines(String text){
        return text.replaceAll("(m?)^", "\t");
    }

    private CliCheckerOptions generateCheckerOptions() {
        Set<PlaceholderRegularExpressions> regexSet = generateParameterRegexSet();
        Set<String> hardFailureSet = generateHardFailureSet();
        return new CliCheckerOptions(regexSet, hardFailureSet, dictionaryAdditionsFilePath, glossaryFilePath);
    }

    private Set<String> generateHardFailureSet() {
        if(hardFailList.stream().anyMatch(checkName -> checkName.equalsIgnoreCase("ALL"))) {
            return Stream.of(CliCheckerType.values()).map(CliCheckerType::getClassName).collect(Collectors.toSet());
        } else {
            return hardFailList.stream().map(check -> {
                try {
                    return CliCheckerType.valueOf(check).getClassName();
                } catch (IllegalArgumentException e) {
                    throw new CommandException("Unknown check name in hard fail list '" + check + "'");
                }
            }).collect(Collectors.toSet());
        }
    }

    private Set<PlaceholderRegularExpressions> generateParameterRegexSet() {
        return parameterRegexList.stream().map(regexName -> {
            try {
                return PlaceholderRegularExpressions.valueOf(regexName);
            } catch (IllegalArgumentException e) {
                throw new CommandException("Unknown parameter regex name " + regexName);
            }
        }).collect(Collectors.toSet());
    }

    private List<CliChecker> generateChecks(List<AssetExtractionDiff> assetExtractionDiffs) {
        CliCheckerOptions options = generateCheckerOptions();
        return checkerList.stream().map(check -> {
            Optional<CliCheckerType> checkEnum = Enums.getIfPresent(CliCheckerType.class, check);
            if(checkEnum.isPresent()){
                CliChecker checker = createInstanceForClassName(checkEnum.get().getClassName());
                checker.setAssetExtractionDiffs(assetExtractionDiffs);
                checker.setCliCheckerOptions(options);
                return checker;
            }
            throw new CommandException("Unknown check '" + check + "'");
        }).collect(Collectors.toList());
    }

    private CliChecker createInstanceForClassName(String className) throws CliCheckerInstantiationException {
        try {
            Class<?> clazz = Class.forName(className);
            return (CliChecker) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new CliCheckerInstantiationException("Cannot create an instance of CliChecker using reflection", e);
        }
    }
}
