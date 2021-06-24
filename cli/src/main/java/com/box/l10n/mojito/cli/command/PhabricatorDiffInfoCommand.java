package com.box.l10n.mojito.cli.command;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.phabricator.DifferentialDiff;
import com.box.l10n.mojito.phabricator.payload.QueryDiffsFields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to export Phabricator Diff info to list of envrionment variables
 * <p>
 * It will export revision id, base commit, author email and author username
 * (that's computed by removing the email part from the author email, it is not coming from phabricator).
 * <p>
 * Format is a list of environment variables like this:
 * MOJITO_PHAB_REVISION_ID=789456
 * MOJITO_PHAB_BASE_COMMIT=c32dadb23
 * MOJITO_PHAB_AUTHOR_EMAIL=user@test.com
 * MOJITO_PHAB_AUTHOR_USERNAME=user
 * <p>
 * Example how to source the result of the command in bash shell
 * source <(mojito phab-diff-to-env-variables --diff-id 123456)
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"phab-diff-to-env-variables"}, commandDescription = "Get diff info: revision id, base commit, " +
        "author email and username (computed by removing email part) as a list of enviroment variables")
public class PhabricatorDiffInfoCommand extends Command {
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(PhabricatorDiffInfoCommand.class);

    @Qualifier("ansiCodeEnabledFalse")
    @Autowired
    ConsoleWriter consoleWriterAnsiCodeEnabledFalse;

    @Autowired(required = false)
    DifferentialDiff differentialDiff;

    @Parameter(names = {"--diff-id"}, arity = 1, required = true, description = "Diff id")
    String diffId = null;

    @Override
    public boolean shouldShowInCommandList() {
        return false;
    }

    @Override
    public void execute() throws CommandException {
        PhabricatorPreconditions.checkNotNull(differentialDiff);

        QueryDiffsFields queryDiffsFields = differentialDiff.queryDiff(diffId);
        consoleWriterAnsiCodeEnabledFalse.a("MOJITO_PHAB_REVISION_ID=").a(queryDiffsFields.getRevisionId()).println();
        consoleWriterAnsiCodeEnabledFalse.a("MOJITO_PHAB_BASE_COMMIT=").a(queryDiffsFields.getSourceControlBaseRevision()).println();
        consoleWriterAnsiCodeEnabledFalse.a("MOJITO_PHAB_AUTHOR_EMAIL=").a(queryDiffsFields.getAuthorEmail()).println();
        consoleWriterAnsiCodeEnabledFalse.a("MOJITO_PHAB_AUTHOR_USERNAME=").a(getUsernameForAuthorEmail(queryDiffsFields.getAuthorEmail())).println();
    }


    String getUsernameForAuthorEmail(String email) {
        String username = null;

        if (email != null) {
            username = email.replaceFirst("(.*)@.*$", "$1");
        }

        return username;
    }
}
