package com.box.l10n.mojito.cli.command;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
@Parameters(commandNames = {"branch-delete", "bd"}, commandDescription = "Delete branches")
public class BranchDeleteCommand extends Command {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(BranchDeleteCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    AssetClient assetClient;

    @Autowired
    CommandHelper commandHelper;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {Param.BRANCH_NAME_LONG, Param.BRANCH_NAME_SHORT}, arity = 1, description = Param.BRANCH_NAME_DESCRIPTION)
    String branchName = null;

    @Override
    public void execute() throws CommandException {
        consoleWriter.newLine().a("Delete branch: ").fg(Ansi.Color.CYAN).a(branchName).reset()
                .a(" from repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println(1);
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        Branch branchToRemove = repositoryClient.getBranch(repository.getId(), branchName);

        if (branchToRemove == null) {
            throw new CommandException(String.format("Cannot find branch in %s by branchName %s.", repositoryParam, branchName));
        }
        PollableTask pollableTask = repositoryClient.deleteBranch(branchToRemove.getId(), repository.getId());
        commandHelper.waitForPollableTask(pollableTask.getId());

        consoleWriter.newLine().a("deleted --> branch name: ").fg(Ansi.Color.MAGENTA).a(branchName).println();
    }
}
