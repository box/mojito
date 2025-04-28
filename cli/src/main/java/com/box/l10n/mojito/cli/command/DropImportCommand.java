package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.DropWsApi;
import com.box.l10n.mojito.apiclient.model.DropDropSummary;
import com.box.l10n.mojito.apiclient.model.ImportDropConfig;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
public class DropImportCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropImportCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--number-drops-fetched"},
      arity = 1,
      required = false,
      description = "Number of drops fetched")
  Long numberOfDropsToFetchParam = 10L;

  @Parameter(
      names = {"--show-all", "-all"},
      required = false,
      description = "Show all drops (already imported drops are hidden by default)")
  Boolean alsoShowImportedParam = false;

  @Parameter(
      names = {Param.DROP_IMPORT_STATUS},
      required = false,
      description = Param.DROP_IMPORT_STATUS_DESCRIPTION,
      converter = ImportDropConfigStatusConverter.class)
  ImportDropConfig.StatusEnum importStatusParam = null;

  @Parameter(
      names = {"--import-fetched"},
      required = false,
      description = "Import all fetched drops")
  Boolean importFetchedParam = false;

  @Autowired CommandHelper commandHelper;

  @Autowired Console console;

  @Autowired DropWsApi dropClient;

  @Override
  public void execute() throws CommandException {

    RepositoryRepository repository = commandHelper.findRepositoryByName(repositoryParam);

    Map<Long, DropDropSummary> numberedAvailableDrops =
        getNumberedAvailableDrops(repository.getId());

    if (numberedAvailableDrops.isEmpty()) {
      consoleWriter.newLine().a("No drop available").println();
    } else {
      consoleWriter.newLine().a("Drops available").println();

      logger.debug("Display drops information");
      for (Map.Entry<Long, DropDropSummary> entry : numberedAvailableDrops.entrySet()) {

        DropDropSummary drop = entry.getValue();

        consoleWriter
            .a("  ")
            .fg(Color.CYAN)
            .a(entry.getKey())
            .reset()
            .a(" - id: ")
            .fg(Color.MAGENTA)
            .a(drop.getId())
            .reset()
            .a(", name: ")
            .fg(Color.MAGENTA)
            .a(drop.getName())
            .reset();

        if (Boolean.TRUE.equals(drop.isCanceled())) {
          consoleWriter.fg(Color.GREEN).a(" CANCELED");
        } else if (drop.getLastImportedDate() == null) {
          consoleWriter.fg(Color.GREEN).a(" NEW");
        } else {
          consoleWriter.a(", last import: ").fg(Color.MAGENTA).a(drop.getLastImportedDate());
        }

        consoleWriter.println();
      }

      List<Long> dropIds = getSelectedDropIds(numberedAvailableDrops);

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

        ImportDropConfig importDropConfigBody = new ImportDropConfig();
        importDropConfigBody.setRepositoryId(repository.getId());
        importDropConfigBody.setDropId(dropId);
        importDropConfigBody.setStatus(importStatusParam);
        ImportDropConfig importDropConfig = dropClient.importDrop(importDropConfigBody);
        PollableTask pollableTask = importDropConfig.getPollableTask();

        commandHelper.waitForPollableTask(pollableTask.getId());
      }
    }

    consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
  }

  /**
   * Gets available {@link DropDropSummary}s and assign them a number (map key) to be referenced in
   * the console input for selection.
   *
   * @return
   */
  private Map<Long, DropDropSummary> getNumberedAvailableDrops(Long repositoryId) {

    logger.debug("Build a map of drops keyed by an incremented integer");
    Map<Long, DropDropSummary> dropIds = new HashMap<>();

    long i = 1;

    for (DropDropSummary availableDrop :
        dropClient
            .getDrops(
                repositoryId,
                getImportedFilter(),
                null,
                0,
                this.numberOfDropsToFetchParam.intValue(),
                null)
            .getContent()) {
      dropIds.put(i++, availableDrop);
    }

    return dropIds;
  }

  /**
   * Returns the "imported" filter to be passed to {@link DropWsApi#getDrops(Long, Boolean, Boolean,
   * java.lang.Integer, java.lang.Integer, java.util.List) } based on the CLI parameter {@link
   * #alsoShowImportedParam}.
   *
   * @return the imported filter to get drops
   */
  private Boolean getImportedFilter() {
    return alsoShowImportedParam ? null : false;
  }

  /**
   * Gets the list of selected {@link DropDropSummary#getId()}.
   *
   * <p>First, reads a drop number from the console and then gets the {@link DropDropSummary} from
   * the map of available {@link DropDropSummary}s.
   *
   * @param numberedAvailableDrops candidate {@link DropDropSummary}s for selection
   * @return selected {@link DropDropSummary#getId()}
   * @throws CommandException if the input doesn't match a number from the map of available {@link
   *     DropDropSummary}s
   */
  private List<Long> getSelectedDropIds(Map<Long, DropDropSummary> numberedAvailableDrops)
      throws CommandException {
    List<Long> selectedDropIds;

    if (importFetchedParam) {
      selectedDropIds = getWithImportFetchedDropIds(numberedAvailableDrops);
    } else {
      selectedDropIds = getFromConsoleDropIds(numberedAvailableDrops);
    }

    return selectedDropIds;
  }

  private List<Long> getFromConsoleDropIds(Map<Long, DropDropSummary> numberedAvailableDrops)
      throws CommandException {
    consoleWriter.newLine().a("Enter Drop number to import").println();
    Long dropNumber = console.readLine(Long.class);

    if (!numberedAvailableDrops.containsKey(dropNumber)) {
      throw new CommandException(
          "Please enter a number from the list: " + numberedAvailableDrops.keySet());
    }

    Long selectId = numberedAvailableDrops.get(dropNumber).getId();

    return Arrays.asList(selectId);
  }

  private List<Long> getWithImportFetchedDropIds(
      Map<Long, DropDropSummary> numberedAvailableDrops) {
    return numberedAvailableDrops.entrySet().stream()
        .filter(x -> !Boolean.TRUE.equals(x.getValue().isCanceled()))
        .map(x -> x.getValue().getId())
        .collect(Collectors.toList());
  }
}
