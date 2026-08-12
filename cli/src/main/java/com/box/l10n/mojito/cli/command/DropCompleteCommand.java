package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameters;
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
    commandNames = {"drop-complete"},
    commandDescription = "Force complete a partially imported drop")
public class DropCompleteCommand extends ProcessDropCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropCompleteCommand.class);

  @Override
  public void execute() throws CommandException {
    Collection<Long> dropIds = getDropIdsToProcess(false);

    for (Long dropId : dropIds) {
      consoleWriter
          .newLine()
          .a("Complete drop: ")
          .fg(Color.CYAN)
          .a(dropId)
          .reset()
          .a(" in repository: ")
          .fg(Color.CYAN)
          .a(repositoryParam)
          .println(2);
      // TODO make drop complete a pollable task to get come confirmation that the action has been
      // performed
      dropClient.completeDrop(dropId);
    }

    consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
  }
}
