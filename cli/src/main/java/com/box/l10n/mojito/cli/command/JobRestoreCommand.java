package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.ScheduledJobClient;
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
    commandNames = {"job-restore"},
    commandDescription = "Restores a scheduled job")
public class JobRestoreCommand extends Command {
  @Parameter(
      names = {Param.JOB_UUID_LONG, Param.JOB_UUID_SHORT},
      arity = 1,
      required = true,
      description = Param.JOB_UUID_DESCRIPTION)
  String uuidParam;

  @Autowired ConsoleWriter consoleWriter;
  @Autowired ScheduledJobClient scheduledJobClient;

  @Override
  public void execute() throws CommandException {
    try {
      scheduledJobClient.restoreJob(UUID.fromString(uuidParam));
      consoleWriter
          .a("restored --> scheduled job id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(uuidParam)
          .println();
    } catch (Exception ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }
}
