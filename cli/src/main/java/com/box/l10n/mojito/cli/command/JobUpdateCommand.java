package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.ScheduledJobClient;
import com.box.l10n.mojito.apiclient.model.ScheduledJobDTO;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import java.util.UUID;
import org.fusesource.jansi.Ansi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author gerryyang
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"job-update"},
    commandDescription = "Updates a scheduled job")
public class JobUpdateCommand extends Command {
  @Parameter(
      names = {Param.JOB_UUID_LONG, Param.JOB_UUID_SHORT},
      arity = 1,
      required = true,
      description = Param.JOB_UUID_DESCRIPTION)
  String uuidParam;

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String repositoryNameParam;

  @Parameter(
      names = {Param.CRON_LONG, Param.CRON_SHORT},
      arity = 1,
      required = false,
      description = Param.CRON_DESCRIPTION)
  String cronParam;

  @Parameter(
      names = {Param.JOB_TYPE_LONG, Param.JOB_TYPE_SHORT},
      arity = 1,
      required = false,
      description = Param.JOB_TYPE_DESCRIPTION)
  String jobTypeParam;

  @Parameter(
      names = {Param.PROPERTIES_STRING_LONG, Param.PROPERTIES_STRING_SHORT},
      arity = 1,
      required = false,
      description = Param.PROPERTIES_STRING_DESCRIPTION)
  String propertiesStringParam;

  @Autowired ConsoleWriter consoleWriter;
  @Autowired ScheduledJobClient scheduledJobClient;

  @Override
  public void execute() throws CommandException {
    try {
      ScheduledJobDTO scheduledJobDTO = new ScheduledJobDTO();
      scheduledJobDTO.setRepository(repositoryNameParam);
      scheduledJobDTO.setCron(cronParam);
      scheduledJobDTO.setPropertiesString(propertiesStringParam);
      if (jobTypeParam != null) {
        scheduledJobDTO.setType(ScheduledJobDTO.TypeEnum.valueOf(jobTypeParam));
      }

      ScheduledJobDTO updatedJob =
          scheduledJobClient.updateJob(UUID.fromString(uuidParam), scheduledJobDTO);

      consoleWriter
          .a("updated --> scheduled job id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(updatedJob.getId())
          .println();
    } catch (Exception ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
