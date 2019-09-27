package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.QuartzJobsClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(commandNames = {"quartz-jobs-delete"}, commandDescription = "Deletes all dynamic quartz jobs")
public class QuartzJobsDeleteCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(QuartzJobsDeleteCommand.class);
    
    @Autowired
    ConsoleWriter consoleWriter;

    @Autowired
    QuartzJobsClient quartzJobsClient;

    @Override
    protected void execute() throws CommandException {   
        consoleWriter.a("Delete quartz jobs").println();
        quartzJobsClient.deleteAllDynamicJobs();
    }
    
}
