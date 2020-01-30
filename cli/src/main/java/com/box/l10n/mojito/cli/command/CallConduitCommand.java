package com.box.l10n.mojito.cli.command;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.phabricator.conduit.arc.ArcCallConduit;
import com.box.l10n.mojito.cli.phabricator.conduit.arc.ArcCallConduitShell;
import com.box.l10n.mojito.cli.phabricator.conduit.payload.Data;
import com.box.l10n.mojito.cli.phabricator.conduit.payload.RevisionSearchFields;
import com.box.l10n.mojito.cli.shell.Shell;
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

import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_CREATED_BEFORE_LAST_WEEK_SHORT;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_NULL_BRANCH_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_NULL_BRANCH_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_NULL_BRANCH_SHORT;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_TRANSLATED_DESCRIPTION;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_TRANSLATED_LONG;
import static com.box.l10n.mojito.cli.command.param.Param.BRANCH_TRANSLATED_SHORT;

@Component
@Scope("prototype")
@Parameters(commandNames = {"arc-call-conduit-get-revision-id"}, commandDescription = "Get the revision id for a target phid")
public class CallConduitCommand extends Command {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(CallConduitCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    ArcCallConduit arcCallConduitShell;

    @Parameter(names = {"--target-phid"}, arity = 1, required = true, description = "target phid")
    String targetPhid = null;

    @Override
    public void execute() throws CommandException {
        Data<RevisionSearchFields> revisionForTargetPhid = arcCallConduitShell.getRevisionForTargetPhid(targetPhid);
        consoleWriter.a(revisionForTargetPhid.getId()).println();
    }

}
