package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.filefinder.FileFinder;
import com.box.l10n.mojito.cli.filefinder.FileFinderException;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.client.exception.RestClientException;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class CommandHelper {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CommandHelper.class);

    /**
     * Supported BOM
     */
    private final ByteOrderMark[] boms = {ByteOrderMark.UTF_8, ByteOrderMark.UTF_16BE, ByteOrderMark.UTF_16LE};
    
    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    PollableTaskClient pollableTaskClient;

    @Autowired
    CommandWaitForPollableTaskListener commandWaitForPollableTaskListener;

    @Autowired
    ConsoleWriter consoleWriter;

    /**
     * @param repositoryName Name of repository
     * @return
     */
    public Repository findRepositoryByName(String repositoryName) throws CommandException {

        try {
            return repositoryClient.getRepositoryByName(repositoryName);
        } catch (RestClientException e) {
            throw new CommandException("Repository: " + repositoryName + " could not be found", e);
        }
    }

    /**
     * Get list of {@link com.box.l10n.mojito.cli.filefinder.FileMatch} from
     * source directory
     *
     * @param commandDirectories
     * @param fileType
     * @param sourcePathFilterRegex
     * @return
     * @throws CommandException
     */
    public ArrayList<FileMatch> getSourceFileMatches(CommandDirectories commandDirectories, FileType fileType, String sourcePathFilterRegex) throws CommandException {
        logger.debug("Search for source asset to be localized");
        FileFinder fileFinder = getFileFinder(commandDirectories, fileType, sourcePathFilterRegex);
        return fileFinder.getSources();
    }

    /**
     * Get list of {@link com.box.l10n.mojito.cli.filefinder.FileMatch} from
     * target directory
     *
     * @param commandDirectories
     * @param fileType
     * @param sourcePathFilterRegex
     * @return
     * @throws CommandException
     */
    public ArrayList<FileMatch> getTargetFileMatches(CommandDirectories commandDirectories, FileType fileType, String sourcePathFilterRegex) throws CommandException {
        logger.debug("Search for target assets that are already localized");
        FileFinder fileFinder = getFileFinder(commandDirectories, fileType, sourcePathFilterRegex);
        return fileFinder.getTargets();
    }

    /**
     * Get {@link FileFinder} from source directory
     *
     * @param commandDirectories
     * @param fileType
     * @param sourcePathFilterRegex
     * @return
     * @throws CommandException
     */
    protected FileFinder getFileFinder(CommandDirectories commandDirectories, FileType fileType, String sourcePathFilterRegex) throws CommandException {
        FileFinder fileFinder = new FileFinder();
        fileFinder.setSourceDirectory(commandDirectories.getSourceDirectoryPath());
        fileFinder.setTargetDirectory(commandDirectories.getTargetDirectoryPath());
        fileFinder.setSourcePathFilterRegex(sourcePathFilterRegex);

        if (fileType != null) {
            fileFinder.setFileTypes(fileType);
        }

        try {
            fileFinder.find();
        } catch (FileFinderException e) {
            throw new CommandException(e.getMessage(), e);
        }

        return fileFinder;
    }

    /**
     * Get content from {@link java.nio.file.Path} using UTF8
     *
     * @param path
     * @return
     * @throws CommandException
     */
    public String getFileContent(Path path) throws CommandException {
        try {
            File file = path.toFile();
            BOMInputStream inputStream = new BOMInputStream(FileUtils.openInputStream(file), false, boms);
            String fileContent;
            if (inputStream.hasBOM()) {
                fileContent = IOUtils.toString(inputStream, inputStream.getBOMCharsetName());
            } else {
                fileContent = IOUtils.toString(inputStream);
            }
            return fileContent;
        } catch (IOException e) {
            throw new CommandException("Cannot get file content for path: " + path.toString(), e);
        }
    }
    
    /**
     * Writes the content into a file using same format as source file
     *
     * @param content content to be written
     * @param path path to the file
     * @param sourceFileMatch
     * @throws CommandException
     */
    public void writeFileContent(String content, Path path, FileMatch sourceFileMatch) throws CommandException {
        try {
            File outputFile = path.toFile();
            BOMInputStream inputStream = new BOMInputStream(FileUtils.openInputStream(sourceFileMatch.getPath().toFile()), false, boms);
            if (inputStream.hasBOM()) {
                FileUtils.writeByteArrayToFile(outputFile, inputStream.getBOM().getBytes());
                FileUtils.writeByteArrayToFile(outputFile, content.getBytes(inputStream.getBOMCharsetName()), true);
            } else {
                FileUtils.writeStringToFile(outputFile, content, Charsets.UTF_8);
            }
        } catch (IOException e) {
            throw new CommandException("Cannot write file content in path: " + path.toString(), e);
        }
    }
    
    /**
     * Writes the content into a file in UTF8
     *
     * @param content content to be written
     * @param path path to the file
     * @throws CommandException
     */
    public void writeFileContent(String content, Path path) throws CommandException {
        try {
            Files.write(content, path.toFile(), Charsets.UTF_8);
        } catch (IOException e) {
            throw new CommandException("Cannot write file content in path: " + path.toString(), e);
        }
    }

    /**
     * Waits for {@link PollableTask} to be all finished (see {@link PollableTask#isAllFinished()
     * }). Infinite timeout.
     *
     * @param pollableId the {@link PollableTask#id}
     * @throws com.box.l10n.mojito.cli.command.CommandException
     */
    public void waitForPollableTask(Long pollableId) throws CommandException {

        consoleWriter.newLine().a("Running, task id: ").fg(Ansi.Color.MAGENTA).a(pollableId).a(" ").println();

        try {
            pollableTaskClient.waitForPollableTask(pollableId, PollableTaskClient.NO_TIMEOUT, commandWaitForPollableTaskListener);
        } catch (PollableTaskException e) {
            throw new CommandException(e.getMessage(), e.getCause());
        }
    }

    public void initializeFileType(FileType fileType, String sourceLocale) {

        if (!Strings.isNullOrEmpty(sourceLocale)) {
            fileType.getLocaleType().setSourceLocale(sourceLocale);
        }
    }

}
