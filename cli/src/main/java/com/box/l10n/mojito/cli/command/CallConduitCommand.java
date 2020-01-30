package com.box.l10n.mojito.cli.command;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.phabricator.conduit.arc.ArcCallConduit;
import com.box.l10n.mojito.phabricator.conduit.payload.Data;
import com.box.l10n.mojito.phabricator.conduit.payload.RevisionSearchFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
