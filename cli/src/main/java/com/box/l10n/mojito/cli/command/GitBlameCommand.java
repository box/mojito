package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.filefinder.FileMatch;
import com.box.l10n.mojito.cli.filefinder.file.FileType;
import com.box.l10n.mojito.cli.filefinder.file.XcodeXliffFileType;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.TextUnitWithUsageClient;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.box.l10n.mojito.rest.entity.TextUnitWithUsage;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    // add parameters to specify prefix from Mojito and prefix on localhost

    boolean fetchTextUnitUsages = false;

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

        // fetchTextUnitUsages - currently hardcoded
        if (fetchTextUnitUsages) {
            blameWithTextUnitUsages();
        } else {
            blameSourceFiles();
        }


        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

    void blameSourceFiles() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        List<PollableTask> pollableTasks = new ArrayList<>();

        ArrayList<FileMatch> sourceFileMatches = commandHelper.getSourceFileMatches(commandDirectories, fileType, sourceLocale, sourcePathFilterRegex);

        for (FileMatch sourceFileMatch : sourceFileMatches) {

            String sourcePath = sourceFileMatch.getSourcePath();

            logger.info("File: {}", sourcePath);

            BlameResult blameResultForFile = getBlameResultForFile(sourcePath);

            for (int i = 0; i < blameResultForFile.getResultContents().size(); i++) {
                String lineText = blameResultForFile.getResultContents().getString(i);

//                logger.info("Line text: {}", lineText);
                String textUnitName = getTextUnitNameFromLine(lineText, getTextUnitNames(repository.getId()));

                if (textUnitName != null) {
                    logger.info("{} --> {}", textUnitName, lineText);
                    logger.info("{}", blameResultForFile.getSourceAuthor(i).getName());
                    logger.info("{}", blameResultForFile.getSourceCommit(i).toString());
                }
            }
        }
    }

    // for po file
    void blameWithTextUnitUsages() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        List<PollableTask> pollableTasks = new ArrayList<>();

        Map<String, List<Integer>> filesAndLinesToBlame = getUsages();

        for (Map.Entry<String, List<Integer>> fileListEntry : filesAndLinesToBlame.entrySet()) {
            logger.info("current file: {}", fileListEntry.getKey());
            logger.info("lines: {}", fileListEntry.getValue());
        }

//        logger.info("map contains {}", filesAndLinesToBlame);
    }

    // for po file
    private Map<String, List<Integer>> getUsages() {
        Map<String, List<Integer>> filesAndLines = new HashMap<>();

        List<Integer> lines = new ArrayList<>();
        lines.add(new Integer(61));
        filesAndLines.put("webapp/app/common/react/components/growth/unauth/signup/FullPageSignup/FullPageSignup.js", lines);
        List<Integer> lines2 = new ArrayList<>();
        lines2.add(new Integer(104));
        filesAndLines.put("webapp/app/analytics/react/components/PageControls/DatePicker.js", lines2);
        List<Integer> lines3 = new ArrayList<>();
        lines3.add(93); lines3.add(91); lines3.add(92);
        filesAndLines.put("webapp/app/common/lib/SignupModalManager.js", lines3);

        return filesAndLines;
    }

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


    // textunit to find author

    String getTextUnitNameFromLine(String line, List<String> textUnitNames) {

        for (int i = 0; i < line.length(); i++) {
            for (int j = i + 1; j < line.length(); j++) {
                for (String textUnitName : textUnitNames) {
                    if (line.substring(i, j).equals(textUnitName)) {
                        return textUnitName;
                    }
                }
            }
        }

        return null;
    }

    /**
     * this is a dumb implementation. Need to check that the text unit is actual plural form. can't do that only
     * passing the name.
     *
     * @param textUnitName
     * @return
     */
    static String textUnitNameToStringInSourceFile(String textUnitName, boolean isPlural) {
        String stringInFile = textUnitName;

        if (isPlural) {
            stringInFile = textUnitName.split(" _(zero|one|two|few|many|other)")[0];
        }

        return stringInFile;
    }


    // TO remove
    List<String> getTextUnitNames(Long repositoryId) {

        List<String> textUnitNames = new ArrayList<>();

        for (TextUnitWithUsage textUnitWithUsage : textUnitWithUsageClient.getTextUnitToBlame(repositoryId)) {
            textUnitNames.add(textUnitWithUsage.getTextUnitName());
        }
//        textUnitNames.add("business_account_upsell_megaphone_title");
//        textUnitNames.add("business_account_upsell_megaphone_disclaimer");
//        textUnitNames.add("business_account_upsell_megaphone_disclaimer_terms");

        return textUnitNames;
    }


}
