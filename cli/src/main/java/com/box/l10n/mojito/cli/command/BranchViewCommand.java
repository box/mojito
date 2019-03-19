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

import java.util.List;

@Component
@Scope("prototype")
@Parameters(commandNames = {"branch-view", "bw"}, commandDescription = "View Branches")
public class BranchViewCommand extends Command {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(BranchViewCommand.class);

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

    @Parameter(names = {"--deleted", "-d"}, arity = 1, description = "To show deleted branches")
    Boolean deleted = null;


    @Override
    public void execute() throws CommandException {
        consoleWriter.newLine().a("Branches in repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println();
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        List<Branch> branches = repositoryClient.getBranches(repository.getId(), null, deleted);

        for (Branch branch : branches) {
            consoleWriter.newLine().a(" - ").fg(Ansi.Color.CYAN).a(branch.getName()).reset().a(" (" + branch.getId() + ") ");
            if (branch.getDeleted()) {
                consoleWriter.fg(Ansi.Color.MAGENTA).a(" deleted").reset();
            }
        }

        consoleWriter.println(2);
    }
}
