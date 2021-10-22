package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.checks.CliChecker;
import com.box.l10n.mojito.cli.command.checks.CliCheckerExecutor;
import com.box.l10n.mojito.cli.command.checks.CliCheckerInstantiationException;
import com.box.l10n.mojito.cli.command.checks.CliCheckerOptions;
import com.box.l10n.mojito.cli.command.checks.CliCheckerType;
import com.box.l10n.mojito.cli.command.extraction.AssetExtractionDiff;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.ExtractionPaths;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.regex.PlaceholderRegularExpressions;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Command to execute checks against any new source strings
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"run-checks"}, commandDescription = "Execute checks against new source strings")
public class CheckerCommand extends Command {

    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommand.class);

    @Autowired
    ExtractionDiffService extractionDiffService;

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {"--checker-list", "-cl"}, arity = 1, required = true, description = "List of checks to be run against new source strings")
    String checkerList;

    @Parameter(names = {"--hard-fail", "-hf"}, arity = 1, required = false, description = "List of checks that will cause a hard failure, use ALL if all checks should be hard failures")
    String hardFailList = null;

    @Parameter(names = {"--parameter-regexes", "-pr"}, arity = 1, required = false, description = "Regex types used to identify parameters in source strings")
    String parameterRegexList = "";

    @Parameter(names = {"--dictionary-additions-file", "-daf"}, arity = 1, required = false, description = "Path to the dictionary additions file used for the spelling check")
    String dictionaryAdditionsFilePath = "";

    @Parameter(names = {"--glossary-file", "-gf"}, arity = 1, required = false, description = "Path to the glossary file used for the glossary check")
    String glossaryFilePath = "";

    @Parameter(names = {"--name", "-n"}, arity = 1, required = false, description = ExtractionDiffCommand.EXCTRACTION_DIFF_NAME_DESCRIPTION)
    String extractionDiffName = null;

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
                assetExtractionDiffs = extractionDiffService.computeAssetExtractionDiffs(extractionDiffPaths);
            } else {
                consoleWriter.newLine().a("No new strings in diff to be checked.").println();
                return;
            }
        } catch (MissingExtractionDirectoryExcpetion missingExtractionDirectoryException) {
            throw new CommandException("Can't compute extraction diffs", missingExtractionDirectoryException);
        }

        CliCheckerExecutor cliCheckerExecutor = new CliCheckerExecutor(generateChecks(assetExtractionDiffs));
        consoleWriter.newLine().a("Running checks against new strings").println();
        if(!cliCheckerExecutor.executeChecks()) {
            consoleWriter.fg(Ansi.Color.YELLOW).newLine().a("Checks soft failed, sending notifications.").println();
            //TODO: Checks failed, send notifications
        }
        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Checks completed").println(2);
    }

    private CliCheckerOptions generateCheckerOptions() {
        Set<String> regexSet = generateParameterRegexSet();
        Set<String> hardFailureSet = generateHardFailureSet();
        return new CliCheckerOptions(regexSet, hardFailureSet, dictionaryAdditionsFilePath, glossaryFilePath);
    }

    private Set<String> generateHardFailureSet() {
        Set<String> hardFailureSet = new HashSet<>();
        Stream<CliCheckerType> cliCheckerTypeStream = Arrays.stream(CliCheckerType.values());
        if (hardFailList != null) {
            if (hardFailList.equalsIgnoreCase("all")) {
                cliCheckerTypeStream.forEach(cliCheckerType -> hardFailureSet.add(cliCheckerType.name()));
            } else {
                String[] hardFails = hardFailList.split(",");
                for (String checkName : hardFails) {
                    if (cliCheckerTypeStream.anyMatch(cliCheckerType -> cliCheckerType.name().equals(checkName))) {
                        hardFailureSet.add(checkName);
                    } else {
                        throw new CommandException("Unknown check name in hard fail list '" + checkName + "'");
                    }
                }
            }
        }
        return hardFailureSet;
    }

    private Set<String> generateParameterRegexSet() {
        Set<String> regexSet = new HashSet<>();
        if(parameterRegexList != null && !parameterRegexList.isEmpty()) {
            String[] regexNames = parameterRegexList.split(",");
            Map<String, String> placeholderRegexMap = getPlaceholderRegexStringMap();
            for(String regexName : regexNames) {
                if (!placeholderRegexMap.containsKey(regexName)){
                    throw new CommandException("Unknown parameter regex name " + regexName);
                }
                regexSet.add(placeholderRegexMap.get(regexName));
            }
        }
        return regexSet;
    }

    private List<CliChecker> generateChecks(List<AssetExtractionDiff> assetExtractionDiffs) {
        List<CliChecker> cliCheckers = new ArrayList<>();
        String[] checks = checkerList.split(",");
        CliCheckerOptions options = generateCheckerOptions();
        Stream<CliCheckerType> checkerTypes = Arrays.stream(CliCheckerType.values());
        for(String check : checks) {
            if(checkerTypes.anyMatch(c -> c.name().equals(check))){
                CliChecker checker = createInstanceForClassName(CliCheckerType.valueOf(check).getClassName());
                checker.setAssetExtractionDiffs(assetExtractionDiffs);
                checker.setCliCheckerOptions(options);
                cliCheckers.add(checker);
            } else {
                throw new CommandException("Unknown check '" + check + "'");
            }
        }
        return cliCheckers;
    }

    private CliChecker createInstanceForClassName(String className) throws CliCheckerInstantiationException {
        try {
            Class<?> clazz = Class.forName(className);
            return (CliChecker) clazz.newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new CliCheckerInstantiationException("Cannot create an instance of CliChecker using reflection", e);
        }
    }

    private Map<String, String> getPlaceholderRegexStringMap() {
        Map<String, String> placeholderRegexMap = new HashMap<>();
        for(PlaceholderRegularExpressions placeholderRegularExpression : PlaceholderRegularExpressions.values()) {
            placeholderRegexMap.put(placeholderRegularExpression.name(), placeholderRegularExpression.getRegex());
        }
        return placeholderRegexMap;
    }
}
