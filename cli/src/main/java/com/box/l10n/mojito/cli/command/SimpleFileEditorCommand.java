package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.file.MacStringsFileType;
import com.box.l10n.mojito.cli.filefinder.file.MacStringsdictFileType;
import com.box.l10n.mojito.cli.filefinder.file.POFileType;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.json.JsonIndenter;
import com.box.l10n.mojito.okapi.ExtractUsagesFromTextUnitComments;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Perform some (very) simple edits on files.
 * <p>
 * Attempted to have a more generic version that works for all file type, started here
 * https://github.com/box/mojito/pull/new/generic_remove_usages. but it has a few hurdles. to revisit later if
 * needed. For now just make a small tool
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"simple-file-editor"}, commandDescription = "Perform simple edits on files")
public class SimpleFileEditorCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(SimpleFileEditorCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    CommandHelper commandHelper;

    @Parameter(names = {Param.SIMPLE_FILE_EDITOR_INPUT_LONG, Param.SIMPLE_FILE_EDITOR_INPUT_SHORT}, arity = 1, required = false, description = Param.SIMPLE_FILE_EDITOR_INPUT_DESCRIPTION)
    String inputDirectoryParam;

    @Parameter(names = {Param.SIMPLE_FILE_EDITOR_OUTPUT_LONG, Param.SIMPLE_FILE_EDITOR_OUTPUT_SHORT}, arity = 1, required = false, description = Param.SIMPLE_FILE_EDITOR_OUTPUT_DESCRIPTION)
    String outputDirectoryParam;

    @Parameter(names = {"--po-remove-usages"}, required = false, description = "To remove usages/refreneces/location/line starting with #: in PO files")
    boolean removeUsagesInPO = false;

    @Parameter(names = {"--macstrings-remove-usages"}, required = false, description = "To remove location from both Mac strings and string dict files")
    boolean removeUsagesInMacStrings = false;

    @Parameter(names = {"--json-indent"}, required = false, description = "To indent JSON files")
    boolean indentJson = false;

    CommandDirectories commandDirectories;

    @Override
    public boolean shouldShowInCommandList() {
        return false;
    }

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Edit files: ").fg(Ansi.Color.CYAN).println(2);
        commandDirectories = new CommandDirectories(inputDirectoryParam, outputDirectoryParam);

        if (removeUsagesInPO) {
            removeUsagesInPOFiles();
        }

        if (removeUsagesInMacStrings) {
            removeUsagesInMacStringsAndStringDict();
        }

        if (indentJson) {
            indentJsonFiles();
        }

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    void indentJsonFiles() throws CommandException {
        commandDirectories.listFilesWithExtensionInSourceDirectory("json")
                .forEach(inputPath -> {
                    consoleWriter.a(" - Indent: ").fg(Ansi.Color.MAGENTA).a(inputPath.toString()).print();
                    String jsonStr = Files.lines(inputPath).collect(Collectors.joining());
                    String modifiedContent = JsonIndenter.indent(jsonStr);
                    writeOutputFile(inputPath, modifiedContent);

                });
    }

    void removeUsagesInPOFiles() throws CommandException {
        POFileType poFileType = new POFileType();
        commandDirectories.listFilesWithExtensionInSourceDirectory(poFileType.getSourceFileExtension(), poFileType.getTargetFileExtension())
                .forEach(inputPath -> {
                    consoleWriter.a(" - Remove usages: ").fg(Ansi.Color.MAGENTA).a(inputPath.toString()).print();
                    String modifiedContent = Files.lines(inputPath)
                            .filter(line -> !line.startsWith("#:"))
                            .collect(Collectors.joining("\n"));
                    writeOutputFile(inputPath, modifiedContent);
                });
    }

    void removeUsagesInMacStringsAndStringDict() throws CommandException {
        Stream.of(new MacStringsFileType(), new MacStringsdictFileType())
                .flatMap(fileType -> commandDirectories.listFilesWithExtensionInSourceDirectory(
                        fileType.getSourceFileExtension(),
                        fileType.getTargetFileExtension()).stream()
                )
                .forEach(inputPath -> {
                    consoleWriter.a(" - Remove usages: ").fg(Ansi.Color.MAGENTA).a(inputPath.toString()).print();
                    String modifiedContent = commandHelper.getFileContent(inputPath)
                            .replaceAll(ExtractUsagesFromTextUnitComments.USAGES_PATTERN, "");
                    writeOutputFile(inputPath, modifiedContent);
                });
    }

    void writeOutputFile(Path inputPath, String modifiedContent) {
        Path outputPath = commandDirectories.resolveWithTargetDirectoryAndCreateParentDirectories(inputPath);
        commandHelper.writeFileContent(modifiedContent, outputPath);
        consoleWriter.a(" --> ").fg(Ansi.Color.CYAN).a(outputPath.toAbsolutePath().toString()).println();
    }

}
