package com.box.l10n.mojito.cli.command;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.Branch;
import com.box.l10n.mojito.rest.entity.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(commandNames = {"branch-remove", "br"}, commandDescription = "remove branch")
public class BranchRemoveCommand {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(BranchRemoveCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    RepositoryClient repositoryClient;


    @Autowired
    CommandHelper commandHelper;



    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"-b", "--branch"}, arity = 1, required = false, description = "branch")
    String branchName;

    @Parameter(names = { "merge-to-master", "m2m"}, arity = 1, required = true, description = "merge into master branch")
    Boolean mergeToMasterBranch;



    private void removeBranch() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
        Branch branchToRemove = repositoryClient.getBranch(repository.getId(), branchName);

    }

}
