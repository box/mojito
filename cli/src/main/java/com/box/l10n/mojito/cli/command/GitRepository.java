package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.rest.entity.GitBlame;
import org.eclipse.jgit.api.BlameCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.blame.BlameResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

public class GitRepository {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(GitRepository.class);

    Repository jgitRepository;

    /**
     * Init a git repository if current directory is within a git repository
     *
     * @throws CommandException
     */
    public void init(String gitDir) throws CommandException {

        logger.debug("Init the jgit Repository");

        FileRepositoryBuilder builder = new FileRepositoryBuilder();

        try {
            jgitRepository = builder
                    .findGitDir(new File(gitDir))
                    .readEnvironment()
                    .build();
        } catch (IOException ioe) {
            throw new CommandException("Can't build the git repository");
        }
    }

    /**
     * Get the git-blame information for given line number
     *
     * @param lineNumber
     * @param blameResultForFile
     * @return
     */
    public GitBlame getBlameResults(int lineNumber, BlameResult blameResultForFile) throws LineMissingException {

        GitBlame gitBlame = new GitBlame();

        try {
            gitBlame.setAuthorName(blameResultForFile.getSourceAuthor(lineNumber).getName());
            gitBlame.setAuthorEmail(blameResultForFile.getSourceAuthor(lineNumber).getEmailAddress());
            gitBlame.setCommitName(blameResultForFile.getSourceCommit(lineNumber).getName());
            gitBlame.setCommitTime(Integer.toString(blameResultForFile.getSourceCommit(lineNumber).getCommitTime()));
        } catch (ArrayIndexOutOfBoundsException e) {
            String msg = MessageFormat.format("The line: {0} is not available in the file anymore", lineNumber);
            logger.debug(msg);
            throw new LineMissingException(msg);
        }

        return gitBlame;

    }

    /**
     * Get the git-blame information for entire file
     *
     * @param filePath
     * @return
     * @throws CommandException
     */
    public BlameResult getBlameResultForFile(String filePath) throws CommandException {

        logger.debug("getBlameResultForFile: {}", filePath);
        try {
            BlameCommand blamer = new BlameCommand(jgitRepository);
            ObjectId commitID = jgitRepository.resolve("HEAD");
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

    public File getDirectory() {
        return jgitRepository.getDirectory();
    }


    public CanonicalTreeParser getHeadTree() throws IOException {
        ObjectId headCommit = jgitRepository.resolve("HEAD^{tree}");
        CanonicalTreeParser treeParser = new CanonicalTreeParser();
        try (ObjectReader reader = jgitRepository.newObjectReader()) {
            treeParser.reset(reader, headCommit);
        }
        return treeParser;
    }

}
