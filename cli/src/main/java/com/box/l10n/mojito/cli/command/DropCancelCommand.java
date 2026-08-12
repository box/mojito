package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.rest.entity.CancelDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import java.util.*;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to cancel a drop. Displays the list of drops available and ask the user the drop id to be
 * canceled.
 *
 * @author jaurambault, wadimw
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"drop-cancel"},
    commandDescription = "Cancel an exported drop")
public class DropCancelCommand extends ProcessDropCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropCancelCommand.class);

  @Override
  public void execute() throws CommandException {
    Collection<Long> dropIds = getDropIdsToProcess(false);

    for (Long dropId : dropIds) {
      consoleWriter
          .newLine()
          .a("Cancel drop: ")
          .fg(Color.CYAN)
          .a(dropId)
          .reset()
          .a(" in repository: ")
          .fg(Color.CYAN)
          .a(repositoryParam)
          .println(2);

      CancelDropConfig cancelDropConfig = dropClient.cancelDrop(dropId);
      PollableTask pollableTask = cancelDropConfig.getPollableTask();

      commandHelper.waitForPollableTask(pollableTask.getId());
    }

    consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
  }
}
