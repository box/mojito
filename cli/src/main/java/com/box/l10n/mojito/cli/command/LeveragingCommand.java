package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.LeveragingClient;
import com.box.l10n.mojito.rest.entity.CopyTmConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to create copy TM from a source repository into a target repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"leveraging-copy-tm"}, commandDescription = "Copy TM from a source repository into a target repository")
public class LeveragingCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(LeveragingCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.SOURCE_REPOSITORY_LONG, Param.SOURCE_REPOSITORY_SHORT}, arity = 1, required = true, description = Param.SOURCE_REPOSITORY_DESCRIPTION)
    String sourceRepositoryParam;

    @Parameter(names = {Param.TARGET_REPOSITORY_LONG, Param.TARGET_REPOSITORY_SHORT}, arity = 1, required = true, description = Param.TARGET_REPOSITORY_DESCRIPTION)
    String targetRepositoryParam;

    @Parameter(names = {"--mode", "-m"}, arity = 1, required = false, description = "Matching mode. "
            + "MD5 will perform matching based on the ID, content and comment. "
            + "EXACT match is only using the content.", converter = CopyTmConfigMode.class)
    CopyTmConfig.Mode mode;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    LeveragingClient leveragingClient;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Copy TM from repository: ").fg(Color.CYAN).a(sourceRepositoryParam).
                reset().a(" into repository: ").fg(Color.CYAN).a(targetRepositoryParam).println(2);

        Repository sourceRepository = commandHelper.findRepositoryByName(sourceRepositoryParam);
        Repository targetRepository = commandHelper.findRepositoryByName(targetRepositoryParam);

        CopyTmConfig copyTM = leveragingClient.copyTM(sourceRepository.getId(), targetRepository.getId(), mode);

        PollableTask pollableTask = copyTM.getPollableTask();
        commandHelper.waitForPollableTask(pollableTask.getId());

        consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
    }

}
