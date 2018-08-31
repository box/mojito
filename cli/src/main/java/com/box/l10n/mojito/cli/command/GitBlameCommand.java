package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.TextUnitWithUsageClient;
import com.box.l10n.mojito.rest.entity.*;
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
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

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
    TextUnitWithUsageClient textUnitWithUsageClient;

    @Autowired
    CommandHelper commandHelper;

    CommandDirectories commandDirectories;

    org.eclipse.jgit.lib.Repository gitRepository;

    @Override
    public void execute() throws CommandException {

        commandDirectories = new CommandDirectories(sourceDirectoryParam);

        consoleWriter.newLine().a("Git blame for repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        List<PollableTask> pollableTasks = new ArrayList<>();

        List<TextUnitWithUsage> textUnitsToBlame = textUnitWithUsageClient.getTextUnitToBlame(repository.getId());
        List<GitInfoForTextUnit> gitInfoForTextUnitList = new ArrayList<>();

        if (fileType.getSourceFileExtension().equals("pot")) {
            blameWithTextUnitUsages(textUnitsToBlame, gitInfoForTextUnitList);
        } else {
            blameSourceFiles(textUnitsToBlame, gitInfoForTextUnitList);
        }

        saveGitInformation(gitInfoForTextUnitList);

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    /**
     * Runs git-blame on each line of the file
     * @param textUnitsToBlame
     * @param gitInfoForTextUnitList
     * @throws CommandException
     */
    void blameSourceFiles(List<TextUnitWithUsage> textUnitsToBlame, List<GitInfoForTextUnit> gitInfoForTextUnitList) throws CommandException {

        ArrayList<FileMatch> sourceFileMatches = commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex);

        for (FileMatch sourceFileMatch : sourceFileMatches) {

            Path sourceRelativePath = getGitRepository().getDirectory().getParentFile().toPath().relativize(sourceFileMatch.getPath());
            BlameResult blameResultForFile = getBlameResultForFile(sourceRelativePath.toString());

            for (int i = 0; i < blameResultForFile.getResultContents().size(); i++) {
                String lineText = blameResultForFile.getResultContents().getString(i);

                TextUnitWithUsage textUnitWithUsage = getTextUnitNameFromLine(lineText, textUnitsToBlame);
                if (textUnitWithUsage != null) {
                    gitInfoForTextUnitList.add(getBlameResults(i, blameResultForFile, textUnitWithUsage));
                }
            }
        }

    }

    /**
     * Reads in the lines of the file and runs git-blame on usage locations given by file
     * @param textUnitsToBlame
     * @param gitInfoForTextUnitList
     * @throws CommandException
     */
    void blameWithTextUnitUsages(List<TextUnitWithUsage> textUnitsToBlame, List<GitInfoForTextUnit> gitInfoForTextUnitList) throws CommandException {

        for (TextUnitWithUsage textUnitWithUsage : textUnitsToBlame) {

            for (String usage : textUnitWithUsage.getUsages()) {
                String filename = getFileName(usage);
                int line = getLineNumber(usage);
                if (extractedFilePrefix != null)
                    filename = filename.replace(extractedFilePrefix, "");
                // TODO: optimize for plural strings on same line - don't need to blame multiple times
                BlameResult blameResultForFile = getBlameResultForFile(filename);
                gitInfoForTextUnitList.add(getBlameResults(line, blameResultForFile, textUnitWithUsage));
            }
        }

    }

    /**
     * Save information from git-blame
     * @param gitInfoForTextUnitList
     */
    private void saveGitInformation(List<GitInfoForTextUnit> gitInfoForTextUnitList) {
        GitInfoForTextUnits gitInfoForTextUnits = new GitInfoForTextUnits();

        gitInfoForTextUnits.setGitInfoForTextUnitList(gitInfoForTextUnitList);
        textUnitWithUsageClient.saveGitInfoForTextUnits(gitInfoForTextUnits);
    }

    /**
     * Get the git-blame information for given line number
     * @param lineNumber
     * @param blameResultForFile
     * @param textUnitWithUsage
     * @return
     */
    private GitInfoForTextUnit getBlameResults(int lineNumber, BlameResult blameResultForFile, TextUnitWithUsage textUnitWithUsage) {
        GitInfoForTextUnit gitInfoForTextUnit = new GitInfoForTextUnit();
        UserGitInfo userGitInfo = new UserGitInfo();

        gitInfoForTextUnit.setTextUnitId(textUnitWithUsage.getTextUnitId());
        gitInfoForTextUnit.setUserGitInfo(userGitInfo);

        userGitInfo.setUserName(blameResultForFile.getSourceAuthor(lineNumber).getName());
        userGitInfo.setUserEmail(blameResultForFile.getSourceAuthor(lineNumber).getEmailAddress());
        userGitInfo.setCommitId(blameResultForFile.getSourceCommit(lineNumber).toString());
        userGitInfo.setCommitDate(Integer.toString(blameResultForFile.getSourceCommit(lineNumber).getCommitTime()));

        return gitInfoForTextUnit;
    }

    /**
     * Builds a git repository if current directory is within a git repository
     * @return
     * @throws CommandException
     */
    org.eclipse.jgit.lib.Repository getGitRepository() throws CommandException {

        if (gitRepository == null) {
            FileRepositoryBuilder builder = new FileRepositoryBuilder();

            try {
                gitRepository = builder
                        .findGitDir(new File(sourceDirectoryParam))
                        .readEnvironment()
                        .build();
            } catch (IOException ioe) {
                throw new CommandException("Can't build the git repository");
            }
        }

        return gitRepository;
    }

    /**
     * Get the git-blame information for entire file
     * @param filePath
     * @return
     * @throws CommandException
     */
    BlameResult getBlameResultForFile(String filePath) throws CommandException {

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
     * Checks if the given line contains a text unit
     * @param line
     * @param textUnitWithUsages
     * @return textUnitWithUsage that is in the line, if any, or null if none
     */
    TextUnitWithUsage getTextUnitNameFromLine(String line, List<TextUnitWithUsage> textUnitWithUsages) {

        if (line != null) {
            for (TextUnitWithUsage textUnitWithUsage : textUnitWithUsages) {
                if (line.contains(textUnitNameToStringInSourceFile(textUnitWithUsage.getTextUnitName()))) {
                    return textUnitWithUsage;
                }
            }
        }
        return null;

    }

    /**
     * Converts text unit name back to how name appears in source code
     * @param textUnitName
     * @return text unit name as string in source file
     */
    static String textUnitNameToStringInSourceFile(String textUnitName) {

        String stringInFile = textUnitName;

        if (textUnitName != null && textUnitName.matches(".+_(zero|one|two|few|many|other)$"))
            stringInFile = textUnitName.split("_(zero|one|two|few|many|other)")[0];

        return stringInFile;

    }

    /**
     * Extracts the file name from the given usage
     * @param usage
     * @return
     */
    static String getFileName(String usage) {
        return usage.split(":")[0];
    }

    /**
     * Extracts the line number from the given usage
     * Must account for difference in line numbers starting with 1 in file and 0 in array
     * @param usage
     * @return
     */
    static int getLineNumber(String usage) throws CommandException{
        try {
            return Integer.parseInt(usage.split(":")[1]) - 1;
        } catch (ArrayIndexOutOfBoundsException e) {
            String msg = MessageFormat.format("Can't get line number from usage: {0}", usage);
            logger.error(msg, e);
            throw new CommandException(msg, e);
        }
    }

}
