package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.phabricator.DifferentialRevision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
@Parameters(
    commandNames = {"phab-add-msg"},
    commandDescription = "Add a message to a Phabricator revision")
public class PhabricatorAddMessageCommand extends Command {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(PhabricatorAddMessageCommand.class);

  @Qualifier("ansiCodeEnabledFalse")
  @Autowired
  ConsoleWriter consoleWriterAnsiCodeEnabledFalse;

  @Autowired(required = false)
  DifferentialRevision differentialRevision;

  @Parameter(
      names = {"--object-id", "-o"},
      arity = 1,
      required = true,
      description = "the object id of the revision")
  String objectId;

  @Parameter(
      names = {"--message", "-m"},
      arity = 1,
      required = true,
      description = "message to add")
  String message;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {
    PhabricatorPreconditions.checkNotNull(differentialRevision);
    differentialRevision.addComment(objectId, message);
  }
}
