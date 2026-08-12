package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import java.util.*;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to import a drop. Displays the list of drops available and ask the user the drop id to be
 * imported.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"drop-import"},
    commandDescription = "Import a translated drop")
public class DropImportCommand extends ProcessDropCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropImportCommand.class);

  @Parameter(
      names = {Param.DROP_IMPORT_STATUS},
      required = false,
      description = Param.DROP_IMPORT_STATUS_DESCRIPTION,
      converter = ImportDropConfigStatusConverter.class)
  ImportDropConfig.Status importStatusParam = null;

  @Parameter(
      names = {"--import-fetched"},
      required = false,
      description = "Import all fetched drops")
  Boolean importFetchedParam = false;

  @Override
  public void execute() throws CommandException {

    Repository repository = commandHelper.findRepositoryByName(repositoryParam);

    Collection<Long> dropIds = getDropIdsToProcess(importFetchedParam);

    for (Long dropId : dropIds) {
      consoleWriter
          .newLine()
          .a("Import drop: ")
          .fg(Color.CYAN)
          .a(dropId)
          .reset()
          .a(" in repository: ")
          .fg(Color.CYAN)
          .a(repositoryParam)
          .println(2);

      ImportDropConfig importDropConfig =
          dropClient.importDrop(repository, dropId, importStatusParam);
      PollableTask pollableTask = importDropConfig.getPollableTask();

      commandHelper.waitForPollableTask(pollableTask.getId());
    }

    consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
  }
}
