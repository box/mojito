package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.extraction.ExtractionsPaths;
import com.box.l10n.mojito.cli.command.extraction.ExtractionDiffService;
import com.box.l10n.mojito.cli.command.extraction.MissingExtractionDirectoryExcpetion;
import com.box.l10n.mojito.cli.command.param.Param;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Computes a diff between to local extractions.
 *
 * The diff is stored in a JSON file. It shows newly added and removed text units and files.
 *
 * This can be used to check if there are new text units added in a commit/branch and then trigger the push command
 * only when needed. Script like this:
 *
 * BASE_COMMIT=85e5c705b2
 * CURRENT_COMMIT=dfcb732e8a
 * BRANCH_NAME=D32904
 *
 * git reset --hard ${BASE_COMMIT}
 * dmojito extract -n ${BASE_COMMIT}
 *
 * git reset --hard ${CURRENT_COMMIT}
 * dmojito extract -n ${CURRENT_COMMIT}
 *
 * dmojito extract-diff -n ${CURRENT_COMMIT} -w ${BASE_COMMIT}
 *
 * has_addedTextUnits=$(jq -e ".addedTextUnits | length > 0 " .mojito/extractions/diff.json)
 *
 * if $has_addedTextUnits;
 * then
 *    mojito push -r test -b ${BRANCH_NAME}
 * else
 *    echo "No text units added, no need to push"
 * fi
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"extract-diff"}, commandDescription = "Generate a diff between 2 local extractions")
public class ExtractionDiffCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ExtractionDiffCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.EXTRACTION_NAME_LONG, Param.EXTRACTION_NAME_SHORT}, arity = 1, required = true, description = Param.EXTRACTION_NAME_DESCRIPTION)
    String extractionName;

    @Parameter(names = {"--with-extraction", "-w"}, arity = 1, required = true, description = "the name of the extraction to compare with")
    String diffWithExtractionName;

    @Parameter(names = {Param.EXTRACTION_OUTPUT_LONG, Param.EXTRACTION_OUTPUT_SHORT}, arity = 1, required = false, description = Param.EXTRACTION_OUTPUT_DESCRIPTION)
    String outputDirectoryParam = ExtractionsPaths.DEFAULT_OUTPUT_DIRECTORY;

    @Parameter(names = {Param.EXTRACTION_INPUT_LONG, Param.EXTRACTION_INPUT_SHORT}, arity = 1, required = false, description = Param.EXTRACTION_INPUT_DESCRIPTION)
    String inputDirectoryParam;

    @Parameter(names = {"--diff-filename", "-df"}, arity = 1, required = false, description = "file name of the diff (saved in the output directory)")
    String diffFileName = ExtractionsPaths.DEFAULT_DIFF_FILENAME;

    @Autowired
    ExtractionDiffService extractionDiffService;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Generate diff between extractions: ").fg(Ansi.Color.CYAN).a(extractionName).reset()
                .a(" and: ").fg(Ansi.Color.CYAN).a(diffWithExtractionName).println();

        ExtractionsPaths extractionsPaths = new ExtractionsPaths(outputDirectoryParam, inputDirectoryParam, diffFileName);

        try {
            extractionDiffService.computeExtractionDiffAndSaveToJsonFile(extractionName, diffWithExtractionName, extractionsPaths);
        } catch (MissingExtractionDirectoryExcpetion msobe) {
            throw new CommandException(msobe.getMessage());
        }

        consoleWriter.a("See the diff: ").fg(Ansi.Color.CYAN).a(extractionsPaths.diffPath().toString());

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

}
