package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.POFileType;
import com.box.l10n.mojito.io.Files;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"po-remove-usages"}, commandDescription = "Removes string usages/location information from PO files")
public class RemoveUsagesFromPoFileCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RemoveUsagesFromPoFileCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.TARGET_DIRECTORY_LONG, Param.TARGET_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.TARGET_DIRECTORY_DESCRIPTION)
    String targetDirectoryParam;

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

        consoleWriter.newLine().a("Remove usages from PO files: ").fg(Ansi.Color.CYAN).println(2);
        commandDirectories = new CommandDirectories(sourceDirectoryParam, targetDirectoryParam);

        POFileType poFileType = new POFileType();
        commandDirectories.listFilesWithExtensionInSourceDirectory(poFileType.getSourceFileExtension(), poFileType.getTargetFileExtension())
                .forEach(path -> {
                    String usagesStripped = Files.lines(path)
                            .filter(line -> !line.startsWith("#:"))
                            .collect(Collectors.joining("\n"));

                    Path relativize = commandDirectories.getSourceDirectoryPath().relativize(path);
                    Path target = commandDirectories.getTargetDirectoryPath().resolve(relativize);
                    Files.createDirectories(target.getParent());
                    Files.write(target, usagesStripped);
                });

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

}
