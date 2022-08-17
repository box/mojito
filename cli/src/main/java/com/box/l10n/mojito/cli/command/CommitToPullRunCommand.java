package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.client.CommitClient;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"commit-to-pull-run"}, commandDescription = "Associate a commit to a Pull Run")
public class CommitToPullRunCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CommitToPullRunCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.COMMIT_HASH_LONG, Param.COMMIT_HASH_SHORT}, arity = 1, required = true, description = "The commit to map to a pull run")
    String commitHash;

    @Parameter(names = {"--input-directory", "-i"}, arity = 1, required = false, description = "The input directory where to read the pull run name, filename: pull-run-name.txt)")
    String inputDirectoryParam;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CommitClient commitClient;

    Repository repository;

    String pullRunName;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Mapping commit: ").fg(Color.CYAN).a(commitHash).println(2);

        repository = commandHelper.findRepositoryByName(repositoryParam);

        String pullRunName = readPullRunNameFromFile();

        if (pullRunName != null) {
            consoleWriter.newLine().a("Found pull run name: ").fg(Color.CYAN).a(pullRunName).println(2);
            associateCommitToPullRun(repository, commitHash, pullRunName);
        } else {
            throw new CommandException(String.format("No pull run name found, check the input directory: '%s' is valid " +
                    "and that the pull run name file: '%s' exists", inputDirectoryParam, pullRunName));
        }

        consoleWriter.fg(Color.GREEN).newLine().a("Finished").println(2);
    }

    void associateCommitToPullRun(Repository repository, String commitHash, String pullRunName) {
        commitClient.associateCommitToPullRun(commitHash, repository.getId(), pullRunName);
    }

    String readPullRunNameFromFile() {
        Path pullRunNameFile = Paths.get(inputDirectoryParam).resolve("pull-run-name.txt");
        return commandHelper.getFileContent(pullRunNameFile);
    }

}
