package com.box.l10n.mojito.cli.command;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(commandNames = {"branch-delete", "bd"}, commandDescription = "delete branch")
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

    @Parameter(names = {Param.BRANCH_LONG, Param.BRANCH_SHORT}, arity = 1, required = true, description = Param.BRANCH_DESCRIPTION)
    String branchName;


    @Override
    public void execute() throws CommandException {
        consoleWriter.newLine().a("Marking branch as deleted in ").a(repositoryParam).a(" for branch: ").fg(Ansi.Color.CYAN).a(branchName).println(2);
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        Branch branchToRemove = repositoryClient.getBranch(repository.getId(), branchName);

        if (branchToRemove == null) {
            throw new CommandException(String.format("Cannot find branch in %s by branchName %s.", repositoryParam, branchName));
        }
        repositoryClient.deleteBranch(branchToRemove.getId(), repository.getId());
        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Mark deleted finished").println(2);
    }

}
