package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to import a drop. Displays the list of drops available and ask the user the drop id to be
 * imported.
 *
 * <p>With {@code --json}: if neither {@code --drop-id} nor {@code --import-fetched} is set, lists
 * available drops and exits (non-interactive). Otherwise imports and returns the imported drop ids.
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

    boolean listOnly =
        isJsonOutput() && importDropId == null && !Boolean.TRUE.equals(importFetchedParam);

    if (listOnly) {
      Map<Long, Drop> numberedAvailableDrops = fetchNumberedAvailableDrops();
      List<Map<String, Object>> available = new ArrayList<>();
      for (Drop drop : numberedAvailableDrops.values()) {
        available.add(toDropJson(drop));
      }
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("repository", repositoryParam);
      data.put("available", available);
      writeJsonSuccess(data);
      return;
    }

    Collection<Long> dropIds = getDropIdsToProcess(importFetchedParam);

    List<Map<String, Object>> imported = new ArrayList<>();

    for (Long dropId : dropIds) {
      if (!isJsonOutput()) {
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
      }

      ImportDropConfig importDropConfig =
          dropClient.importDrop(repository, dropId, importStatusParam);
      PollableTask pollableTask = importDropConfig.getPollableTask();

      commandHelper.waitForPollableTask(pollableTask.getId());

      Map<String, Object> importedDrop = new LinkedHashMap<>();
      importedDrop.put("id", dropId);
      importedDrop.put("pollableTaskId", pollableTask.getId());
      imported.add(importedDrop);
    }

    if (isJsonOutput()) {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("repository", repositoryParam);
      data.put("imported", imported);
      writeJsonSuccess(data);
    } else {
      consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
    }
  }
}
