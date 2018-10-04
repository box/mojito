package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.POFileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.GitBlameWithUsageClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.PollableTaskException;
import com.box.l10n.mojito.rest.entity.GitBlame;
import com.box.l10n.mojito.rest.entity.GitBlameWithUsage;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static org.fusesource.jansi.Ansi.Color.CYAN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"git-blame", "gb"}, commandDescription = "Git blame")
public class GitBlameCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitBlameCommand.class);

    /**
     * Size of the batch when fetching {@link GitBlameWithUsage}s to be processed
     */
    static final int BATCH_SIZE = 500;

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.SOURCE_DIRECTORY_LONG, Param.SOURCE_DIRECTORY_SHORT}, arity = 1, required = false, description = Param.SOURCE_DIRECTORY_DESCRIPTION)
    String sourceDirectoryParam;

    @Parameter(names = {Param.FILE_TYPE_LONG, Param.FILE_TYPE_SHORT}, arity = 1, required = false, description = Param.FILE_TYPE_DESCRIPTION,
            converter = FileTypeConverter.class)
    FileType fileType;

    @Parameter(names = {Param.SOURCE_LOCALE_LONG, Param.SOURCE_LOCALE_SHORT}, arity = 1, required = false, description = Param.SOURCE_LOCALE_DESCRIPTION)
    String sourceLocale;

    @Parameter(names = {Param.SOURCE_REGEX_LONG, Param.SOURCE_REGEX_SHORT}, arity = 1, required = false, description = Param.SOURCE_REGEX_DESCRIPTION)
    String sourcePathFilterRegex;

    @Parameter(names = {"--extracted-prefix"}, arity = 1, required = false, description = "prefix for path of extracted files")
    String extractedFilePrefix;

    @Autowired
    AssetClient assetClient;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    GitBlameWithUsageClient gitBlameWithUsageClient;

    @Autowired
    CommandHelper commandHelper;

    CommandDirectories commandDirectories;

    org.eclipse.jgit.lib.Repository gitRepository;

    /**
     * Cache {@link BlameResult} of a given file.
     *
     * A cache is used since getting blame information for a file can be pretty slow and required multiple time for
     * a project. The cache is build with {@link CacheBuilder.newBuilder().softValues().build()} to free
     * {@link BlameResult} if memory is missing
     */
    Cache<String, BlameResult> getBlameResultForFileCache = CacheBuilder.newBuilder().softValues().build();

    @Override
    public void execute() throws CommandException {

        commandDirectories = new CommandDirectories(sourceDirectoryParam);

        consoleWriter.newLine().a("Git blame for repository: ").fg(CYAN).a(repositoryParam).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        List<PollableTask> pollableTasks = new ArrayList<>();

        int numGitBlameWithUsages;
        int offset = 0;
        boolean hasMore = false;
        do {
            if (offset > 0) {
                consoleWriter.erasePreviouslyPrintedLines();
            }

            consoleWriter.a("Process batch: ").fg(YELLOW).a(offset).a("-").a(offset + BATCH_SIZE).println();
            List<GitBlameWithUsage> gitBlameWithUsages =  gitBlameWithUsageClient.getGitBlameWithUsages(repository.getId(), offset, BATCH_SIZE);
            numGitBlameWithUsages = gitBlameWithUsages.size();

            List<GitBlameWithUsage> getGitBlameWithUsagesToProcess = getGitBlameWithUsagesToProcess(gitBlameWithUsages);

            logger.debug("Got: {}, skippped: {} since they already have git blame info",
                    numGitBlameWithUsages,
                    numGitBlameWithUsages - getGitBlameWithUsagesToProcess.size());

            offset += numGitBlameWithUsages;

            if (fileType instanceof POFileType) {
                blameWithTextUnitUsages(getGitBlameWithUsagesToProcess);
            } else {
                blameSourceFiles(getGitBlameWithUsagesToProcess);
            }

            pollableTasks.add(gitBlameWithUsageClient.saveGitBlameWithUsages(getGitBlameWithUsagesToProcess));
        } while (numGitBlameWithUsages == BATCH_SIZE);

        try {
            logger.debug("Wait for all \"git-blame\" tasks to be finished");
            for (PollableTask pollableTask : pollableTasks) {
                commandHelper.waitForPollableTask(pollableTask.getId());
            }
        } catch (PollableTaskException e) {
            throw new CommandException(e.getMessage(), e.getCause());
        }

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    /**
     * We just process {@link GitBlameWithUsage} that have no information at all. If there was a reccord saved
     * with empty information it won't be recomputed.
     *
     * We can later add an option to force update the content if needed
     *
     * @param gitBlameWithUsages
     * @return the entries to be processed
     */
    public List<GitBlameWithUsage> getGitBlameWithUsagesToProcess(List<GitBlameWithUsage> gitBlameWithUsages) {
        List<GitBlameWithUsage> filteredGitBlameWithUsages = new ArrayList<>();

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
            if (gitBlameWithUsage.getGitBlame() == null) {
                logger.debug("will have to blame: {}", gitBlameWithUsage.getTextUnitName());
                filteredGitBlameWithUsages.add(gitBlameWithUsage);
            }
        }
        return filteredGitBlameWithUsages;
    }

    /**
     * Runs git-blame on each line of the file
     *
     * @param gitBlameWithUsages
     * @throws CommandException
     */
    void blameSourceFiles(List<GitBlameWithUsage> gitBlameWithUsages) throws CommandException {

        ArrayList<FileMatch> sourceFileMatches = commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex);

        for (FileMatch sourceFileMatch : sourceFileMatches) {

            Path sourceRelativePath = getGitRepository().getDirectory().getParentFile().toPath().relativize(sourceFileMatch.getPath());
            BlameResult blameResultForFile = getBlameResultForFile(sourceRelativePath.toString());

            if (blameResultForFile != null) {
                for (int i = 0; i < blameResultForFile.getResultContents().size(); i++) {
                    String lineText = blameResultForFile.getResultContents().getString(i);

                    List<GitBlameWithUsage> gitBlameWithUsageList = getGitBlameWithUsagesFromLine(lineText, gitBlameWithUsages);
                    for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsageList) {
                        try {
                            updateBlameResultsInGitBlameWithUsage(i, blameResultForFile, gitBlameWithUsage);
                        } catch(LineMissingException lme) {
                            throw new RuntimeException("Processing source file, this must not happen", lme);
                        }
                    }
                }
            } else {
                consoleWriter.a("Source file:").fg(CYAN).a(sourceRelativePath.toString()).reset().a(" not in Git. Skip it.");
            }
        }

    }

    /**
     * Reads in the lines of the file and runs git-blame on usage locations given by file
     *
     * @param gitBlameWithUsages
     * @throws CommandException
     */
    void blameWithTextUnitUsages(List<GitBlameWithUsage> gitBlameWithUsages) throws CommandException {

        for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {

            if (gitBlameWithUsage.getUsages() != null && gitBlameWithUsage.getUsages().size() > 0) {

                for (String usage : gitBlameWithUsage.getUsages()) {
                    String filename = getFileName(usage);
                    int line = getLineNumber(usage);

                    if (extractedFilePrefix != null) {
                        filename = filename.replace(extractedFilePrefix, "");
                    }

                    try {
                        BlameResult blameResultForFile = getBlameResultForFileCached(filename);
                        updateBlameResultsInGitBlameWithUsage(line, blameResultForFile, gitBlameWithUsage);
                        break;
                    } catch (NoSuchFileException e) {
                        logger.debug("Can't blame file: {} for asset text unit: {}", filename, gitBlameWithUsage.getAssetTextUnitId());
                    } catch (LineMissingException lme) {
                        logger.debug("Missing line: {} in file: {} for asset text unit: {}", lme, filename, gitBlameWithUsage.getAssetTextUnitId());
                    }
                }
            }
        }
    }

    /**
     * Get the git-blame information for given line number
     *
     * @param gitBlameWithUsage
     * @param lineNumber
     * @return
     */
    GitBlame getBlameResults(int lineNumber, BlameResult blameResultForFile) throws LineMissingException {

        GitBlame gitBlame = new GitBlame();

        try {
            gitBlame.setAuthorName(blameResultForFile.getSourceAuthor(lineNumber).getName());
            gitBlame.setAuthorEmail(blameResultForFile.getSourceAuthor(lineNumber).getEmailAddress());
            gitBlame.setCommitName(blameResultForFile.getSourceCommit(lineNumber).getName());
            gitBlame.setCommitTime(Integer.toString(blameResultForFile.getSourceCommit(lineNumber).getCommitTime()));
        } catch (ArrayIndexOutOfBoundsException e) {
            String msg = MessageFormat.format("The line: {0} is not availalbe in the file anymore", lineNumber);
            logger.debug(msg);
            throw new LineMissingException(msg);
        }

        return gitBlame;

    }

    void updateBlameResultsInGitBlameWithUsage(int lineNumber, BlameResult blameResultForFile, GitBlameWithUsage gitBlameWithUsage) throws LineMissingException {
        GitBlame gitBlame = getBlameResults(lineNumber, blameResultForFile);
        gitBlameWithUsage.setGitBlame(gitBlame);
    }

    /**
     * Builds a git repository if current directory is within a git repository
     *
     * @return
     * @throws CommandException
     */
    org.eclipse.jgit.lib.Repository getGitRepository() throws CommandException {

        if (gitRepository == null) {
            logger.debug("Create the gitRepository");

            FileRepositoryBuilder builder = new FileRepositoryBuilder();

            try {
                gitRepository = builder
                        .findGitDir(new File(commandDirectories.getSourceDirectoryPath().toString()))
                        .readEnvironment()
                        .build();
            } catch (IOException ioe) {
                throw new CommandException("Can't build the git repository");
            }
        }

        return gitRepository;
    }


    /**
     * Gets {@link BlameResult} from the cache.
     *
     * @param filePath file path to be blamed
     * @return
     * @throws CommandException something unexpected happened
     * @throws NoSuchFileException if the file is missing and can't be blamed
     */
    BlameResult getBlameResultForFileCached(final String filePath) throws CommandException, NoSuchFileException {
        try {
            return getBlameResultForFileCache.get(filePath, new Callable<BlameResult>() {
                @Override
                public BlameResult call() throws Exception {
                    BlameResult blameResult = getBlameResultForFile(filePath);
                    if (blameResult == null) {
                        throw new NoSuchFileException(filePath);
                    }
                    return blameResult;
                }
            });
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();

            if (cause instanceof NoSuchFileException) {
                throw (NoSuchFileException) cause;
            } else {
                throw new CommandException(cause);
            }
        }
    }

    /**
     * Get the git-blame information for entire file
     *
     * @param filePath
     * @return
     * @throws CommandException
     */
    BlameResult getBlameResultForFile(String filePath) throws CommandException {

        logger.debug("getBlameResultForFile: {}", filePath);
        try {
            org.eclipse.jgit.lib.Repository gitRepository = getGitRepository();

            BlameCommand blamer = new BlameCommand(gitRepository);
            ObjectId commitID = gitRepository.resolve("HEAD");
            blamer.setStartCommit(commitID);
            blamer.setFilePath(filePath);
            BlameResult blame = blamer.call();

            return blame;
        } catch (GitAPIException | IOException e) {
            String msg = MessageFormat.format("Can't get blame result for file: {0}", filePath);
            logger.error(msg, e);
            throw new CommandException(msg, e);
        }
    }

    /**
     * Checks if the given line contains text unit(s) to git-blame
     *
     * @param line
     * @param gitBlameWithUsages
     * @return list of GitBlameWithUsage objects that match current line
     */
    List<GitBlameWithUsage> getGitBlameWithUsagesFromLine(String line, List<GitBlameWithUsage> gitBlameWithUsages) {

        List<GitBlameWithUsage> gitBlameWithUsagesWithLine = new ArrayList<>();

        if (line != null) {
            for (GitBlameWithUsage gitBlameWithUsage : gitBlameWithUsages) {
                if (line.contains(textUnitNameToStringInSourceFile(gitBlameWithUsage.getTextUnitName()))) {
                    gitBlameWithUsagesWithLine.add(gitBlameWithUsage);
                }
            }
        }
        return gitBlameWithUsagesWithLine;
    }

    /**
     * Converts text unit name back to how name appears in source code
     *
     * @param textUnitName
     * @return text unit name as string in source file
     */
    String textUnitNameToStringInSourceFile(String textUnitName) {

        String stringInFile = textUnitName;

        if (textUnitName != null && textUnitName.matches(".+_(zero|one|two|few|many|other)$"))
            stringInFile = textUnitName.split("_(zero|one|two|few|many|other)")[0];

        return stringInFile;

    }

    /**
     * Extracts the file name from the given usage
     *
     * @param usage
     * @return
     */
    String getFileName(String usage) {
        return usage.split(":")[0];
    }

    /**
     * Extracts the line number from the given usage
     * Must account for difference in line numbers starting with 1 in file and 0 in array
     *
     * @param usage
     * @return
     */
    int getLineNumber(String usage) throws CommandException {
        try {
            return Integer.parseInt(usage.split(":")[1]) - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            String msg = MessageFormat.format("Can't get line number from usage: {0}", usage);
            logger.error(msg, e);
            throw new CommandException(msg, e);
        }
    }

}