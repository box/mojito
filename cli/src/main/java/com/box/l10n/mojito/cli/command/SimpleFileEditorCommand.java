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
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.filters.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.filters.POFilter;
import com.box.l10n.mojito.okapi.steps.AbstractMd5ComputationStep;
import com.box.l10n.mojito.okapi.steps.FilterEventsToInMemoryRawDocumentStep;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.common.resource.StartGroup;
import net.sf.okapi.common.skeleton.GenericSkeleton;
import net.sf.okapi.common.skeleton.GenericSkeletonPart;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Parameter(names = {"--input-filter-regex"}, required = false, description = "Regex to filter input files with a regex eg. '.*Localizable.strings.*'")
    String inputFilterRegex;

    @Parameter(names = {"--po-remove-usages"}, required = false, description = "To remove usages/refreneces/location/line starting with #: in PO files")
    boolean removeUsagesInPO = false;

    @Parameter(names = {"--po-add-context-comment"}, required = false, description = "Add context and comment when missing")
    boolean addContextAndComment = false;

    @Parameter(names = {"--macstrings-remove-usages"}, required = false, description = "To remove location from both Mac strings and string dict files")
    boolean removeUsagesInMacStrings = false;

    @Parameter(names = {"--json-indent"}, required = false, description = "To indent JSON files")
    boolean indentJson = false;

    CommandDirectories commandDirectories;

    Pattern inputFilterPattern;

    @Override
    public boolean shouldShowInCommandList() {
        return false;
    }

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Edit files: ").fg(Ansi.Color.CYAN).println(2);
        commandDirectories = new CommandDirectories(inputDirectoryParam, outputDirectoryParam);
        inputFilterPattern = inputFilterRegex == null ? null : Pattern.compile(inputFilterRegex);

        if (removeUsagesInPO) {
            removeUsagesInPOFiles();
        }

        if (addContextAndComment) {
            addContextAndCommentInPOFiles();
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
        commandDirectories.listFilesWithExtensionInSourceDirectory("json").stream()
                .filter(getInputFilterMatch())
                .forEach(inputPath -> {
                    consoleWriter.a(" - Indent: ").fg(Ansi.Color.MAGENTA).a(inputPath.toString()).print();
                    String jsonStr = Files.lines(inputPath).collect(Collectors.joining());
                    String modifiedContent = JsonIndenter.indent(jsonStr);
                    writeOutputFile(inputPath, modifiedContent);

                });
    }

    void removeUsagesInPOFiles() throws CommandException {
        POFileType poFileType = new POFileType();
        commandDirectories.listFilesWithExtensionInSourceDirectory(poFileType.getSourceFileExtension(), poFileType.getTargetFileExtension()).stream()
                .filter(getInputFilterMatch())
                .forEach(inputPath -> {
                    consoleWriter.a(" - Remove usages: ").fg(Ansi.Color.MAGENTA).a(inputPath.toString()).print();
                    String modifiedContent = Files.lines(inputPath)
                            .filter(line -> !line.startsWith("#:"))
                            .collect(Collectors.joining("\n"));
                    writeOutputFile(inputPath, modifiedContent);
                });
    }

    void addContextAndCommentInPOFiles() throws CommandException {
        POFileType poFileType = new POFileType();

        Pattern localePattern = Pattern.compile(".*/(.*?)/LC_MESSAGES/.*\\.po$");

        commandDirectories.listFilesWithExtensionInSourceDirectory(poFileType.getSourceFileExtension(), poFileType.getTargetFileExtension()).stream()
                .filter(getInputFilterMatch())
                .forEach(inputPath -> {
                    String inputPathStr = inputPath.toString();
                    LocaleId localeId = LocaleId.EMPTY;
                    if (inputPathStr.endsWith(".po")) {
                        Matcher matcher = localePattern.matcher(inputPathStr);
                        if (matcher.matches()) {
                            String strLocale = matcher.group(1);
                            localeId = LocaleId.fromString(strLocale);
                        } else {
                            throw new RuntimeException("can't find locale");
                        }
                    }
                    consoleWriter.a(" - Add context and comment to: ").fg(Ansi.Color.MAGENTA).a(inputPathStr).print();
                    String modifiedContent = addContextAndCommentInPOFile(commandHelper.getFileContent(inputPath), localeId);
                    writeOutputFile(inputPath, modifiedContent);
                });
    }

    String addContextAndCommentInPOFile(String poContent, LocaleId localeId) {
        IPipelineDriver driver = new PipelineDriver();
        driver.addStep(new RawDocumentToFilterEventsStep(new POFilter()));
        driver.addStep(new AbstractMd5ComputationStep() {

            @Override
            protected Event handleTextUnit(Event event) {
                event = super.handleTextUnit(event);
                GenericSkeleton genericSkeleton = (GenericSkeleton) event.getTextUnit().getSkeleton();
                addContextAndCommentInSkeleton(genericSkeleton);
                return event;
            }

            @Override
            protected Event handleStartGroup(Event event) {
                StartGroup startGroup = event.getStartGroup();
                if ("x-gettext-plurals".equals(startGroup.getType())) {
                    GenericSkeleton genericSkeleton = (GenericSkeleton) startGroup.getSkeleton();
                    addContextAndCommentInSkeleton(genericSkeleton);
                }
                return event;
            }

            @Override
            public String getName() {
                return "addContextAndCommentInPOFile";
            }

            @Override
            public String getDescription() {
                return "addContextAndCommentInPOFile";
            }
        });

        FilterEventsToInMemoryRawDocumentStep filterEventsToInMemoryRawDocumentStep = new FilterEventsToInMemoryRawDocumentStep();
        driver.addStep(filterEventsToInMemoryRawDocumentStep);
        RawDocument rawDocument = new RawDocument(poContent, LocaleId.ENGLISH, localeId);
        rawDocument.setAnnotation(new CopyFormsOnImport());
        driver.addBatchItem(rawDocument);
        logger.debug("Start processing batch");
        driver.processBatch();
        return filterEventsToInMemoryRawDocumentStep.getOutput(rawDocument);
    }

    void addContextAndCommentInSkeleton(GenericSkeleton genericSkeleton) {
        String skeletonStr = genericSkeleton.toString();
        boolean needsContext = !skeletonStr.contains("msgctx");
        boolean needsComment = !skeletonStr.contains("#.");
        for (GenericSkeletonPart part : genericSkeleton.getParts()) {
            StringBuilder sb = part.getData();
            String str = sb.toString();
            if (needsComment) {
                if (skeletonStr.contains("#:")) {
                    str = str.replaceFirst("#: ", "#. --\n#: ");
                } else {
                    str = str.replaceFirst("msgid ", "#. --\nmsgid ");
                }
            }
            if (needsContext) {
                str = str.replaceFirst("msgid ", "msgctxt \" - \"\nmsgid ");
            }
            sb.replace(0, sb.length(), str);
        }
    }

    void removeUsagesInMacStringsAndStringDict() throws CommandException {
        Stream.of(new MacStringsFileType(), new MacStringsdictFileType())
                .flatMap(fileType -> commandDirectories.listFilesWithExtensionInSourceDirectory(
                        fileType.getSourceFileExtension(),
                        fileType.getTargetFileExtension()).stream()
                        .filter(path -> fileType.getSourceFilePattern().getPattern().matcher(path.toAbsolutePath().toString()).matches()
                                || fileType.getTargetFilePattern().getPattern().matcher(path.toAbsolutePath().toString()).matches())
                )
                .filter(getInputFilterMatch())
                .forEach(inputPath -> {
                    consoleWriter.a(" - Remove usages: ").fg(Ansi.Color.MAGENTA).a(inputPath.toString()).print();
                    String modifiedContent = commandHelper.getFileContent(inputPath)
                            .replaceAll(ExtractUsagesFromTextUnitComments.USAGES_PATTERN, "");
                    writeOutputFile(inputPath, modifiedContent);
                });
    }

    Predicate<Path> getInputFilterMatch() {
        return path -> inputFilterPattern == null || inputFilterPattern.matcher(path.toAbsolutePath().toString()).matches();
    }

    void writeOutputFile(Path inputPath, String modifiedContent) {
        Path outputPath = commandDirectories.resolveWithTargetDirectoryAndCreateParentDirectories(inputPath);
        commandHelper.writeFileContent(modifiedContent, outputPath, inputPath);
        consoleWriter.a(" --> ").fg(Ansi.Color.CYAN).a(outputPath.toAbsolutePath().toString()).println();
    }

}
