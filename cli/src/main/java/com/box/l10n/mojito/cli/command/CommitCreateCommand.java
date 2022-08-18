package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.CommitClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Commit;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author garion
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"commit-create", "cc"}, commandDescription = "Create commit information in Mojito.")
public class CommitCreateCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CommitCreateCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    CommitClient commitClient;

    @Autowired
    RepositoryClient repositoryClient;


    @Parameter(names = {Param.COMMIT_HASH_LONG, Param.COMMIT_HASH_SHORT}, arity = 1, required = true, description =
            Param.COMMIT_CREATE_DESCRIPTION)
    String commitHash;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;


    @Parameter(names = {Param.AUTHOR_NAME_LONG, Param.AUTHOR_NAME_SHORT}, arity = 1, required = true, description =
            Param.AUTHOR_NAME_DESCRIPTION)
    String authorNameParam;


    @Parameter(names = {Param.AUTHOR_EMAIL_LONG, Param.AUTHOR_EMAIL_SHORT}, arity = 1, required = true, description =
            Param.AUTHOR_EMAIL_DESCRIPTION)
    String authorEmailParam;


    @Parameter(names = {Param.CREATION_DATE_LONG, Param.CREATION_DATE_SHORT}, arity = 1, required = true, description =
            Param.CREATION_DATE_DESCRIPTION, converter = DateTimeConverter.class)
    DateTime creationDateParam;

    @Override
    protected void execute() throws CommandException {
        consoleWriter.newLine()
                .a("Store single commit information for repository: ")
                .fg(Ansi.Color.CYAN)
                .a(repositoryParam)
                .println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        Commit commit = commitClient.createCommit(commitHash,
                                                  repository.getId(),
                                                  authorNameParam,
                                                  authorEmailParam,
                                                  creationDateParam);

        consoleWriter.fg(Ansi.Color.GREEN)
                .newLine()
                .a("Finished. Stored in the database with commit ID: ")
                .fg(Ansi.Color.CYAN)
                .a(commit.getId())
                .println(2);
    }
}
