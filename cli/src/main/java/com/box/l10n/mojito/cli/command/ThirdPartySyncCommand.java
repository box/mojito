package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.ThirdPartyClient;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import static org.fusesource.jansi.Ansi.Color.CYAN;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"thirdparty-sync", "tps"}, commandDescription = "Third-party command to sychronize text units and screenshots with third party TMS")
public class ThirdPartySyncCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ThirdPartySyncCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--project", "-p"}, arity = 1, required = true, description = "Third party project to synchronize with")
    String thirdPartyProjectId;

    @Autowired
    ThirdPartyClient thirdPartyClient;

    @Autowired
    CommandHelper commandHelper;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Third party TMS synchronization for repository: ").fg(CYAN).a(repositoryParam).reset()
                .a(" project id: ").fg(CYAN).a(thirdPartyProjectId).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        PollableTask pollableTask = thirdPartyClient.sync(repository.getId(), thirdPartyProjectId);

        commandHelper.waitForPollableTask(pollableTask.getId());

        consoleWriter.fg(Ansi.Color.GREEN).newLine().a("Finished").println(2);
    }

}