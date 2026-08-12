package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.Repository;
import java.util.*;
import java.util.stream.Collectors;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author wadimw
 */
public abstract class ProcessDropCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(ProcessDropCommand.class);

  @Autowired protected ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  protected String repositoryParam;

  @Parameter(
      names = {"--number-drops-fetched"},
      arity = 1,
      required = false,
      description = "Number of drops fetched")
  protected Long numberOfDropsToFetchParam = 10L;

  @Parameter(
      names = {"--show-all", "-all"},
      required = false,
      description = "Show all drops (already imported drops are hidden by default)")
  protected Boolean alsoShowImportedParam = false;

  @Parameter(
      names = {"--drop-id", "-i"},
      arity = 1,
      required = false,
      description = "ID of a drop to process (skip drop fetching and process only given ID)")
  protected Long importDropId = null;

  @Autowired protected CommandHelper commandHelper;

  @Autowired protected Console console;

  @Autowired protected DropClient dropClient;

  protected Collection<Long> getDropIdsToProcess(Boolean allFetched) {

    if (importDropId != null) {
      return Collections.singletonList(importDropId);
    }

    Repository repository = commandHelper.findRepositoryByName(repositoryParam);
    Map<Long, Drop> numberedAvailableDrops = getNumberedAvailableDrops(repository.getId());

    if (numberedAvailableDrops.isEmpty()) {
      // command should not error when no drops are available
      consoleWriter.newLine().a("No drop available").println();
      return Collections.emptyList();
    }

    consoleWriter.newLine().a("Drops available").println();
    displayAvailableDrops(numberedAvailableDrops);

    if (Boolean.TRUE.equals(allFetched)) {
      return getAllFetchedDropIds(numberedAvailableDrops);
    }
    return getFromConsoleDropIds(numberedAvailableDrops);
  }

  protected void displayAvailableDrops(Map<Long, Drop> numberedAvailableDrops) {
    logger.debug("Display drops information");
    for (Map.Entry<Long, Drop> entry : numberedAvailableDrops.entrySet()) {

      Drop drop = entry.getValue();

      consoleWriter
          .a("  ")
          .fg(Ansi.Color.CYAN)
          .a(entry.getKey())
          .reset()
          .a(" - id: ")
          .fg(Ansi.Color.MAGENTA)
          .a(drop.getId())
          .reset()
          .a(", name: ")
          .fg(Ansi.Color.MAGENTA)
          .a(drop.getName())
          .reset();

      if (Boolean.TRUE.equals(drop.getCanceled())) {
        consoleWriter.fg(Ansi.Color.GREEN).a(" CANCELED");
      } else if (drop.getLastImportedDate() == null) {
        consoleWriter.fg(Ansi.Color.GREEN).a(" NEW");
      } else {
        consoleWriter.a(", last import: ").fg(Ansi.Color.MAGENTA).a(drop.getLastImportedDate());
      }

      consoleWriter.println();
    }
  }

  /**
   * Gets available {@link Drop}s and assign them a number (map key) to be referenced in the console
   * input for selection.
   *
   * @return
   */
  protected Map<Long, Drop> getNumberedAvailableDrops(Long repositoryId) {

    logger.debug("Build a map of drops keyed by an incremented integer");
    Map<Long, Drop> dropIds = new HashMap<>();

    long i = 1;

    for (Drop availableDrop :
        dropClient
            .getDrops(repositoryId, getImportedFilter(), 0L, numberOfDropsToFetchParam)
            .getContent()) {
      dropIds.put(i++, availableDrop);
    }

    return dropIds;
  }

  /**
   * Returns the "imported" filter to be passed to {@link DropClient#getDrops(java.lang.Long,
   * java.lang.Boolean, java.lang.Long, java.lang.Long) } based on the CLI parameter {@link
   * #alsoShowImportedParam}.
   *
   * @return the imported filter to get drops
   */
  protected Boolean getImportedFilter() {
    return Boolean.TRUE.equals(alsoShowImportedParam) ? null : false;
  }

  /**
   * Gets the list of selected {@link Drop#id}.
   *
   * <p>First, reads a drop number from the console and then gets the {@link Drop} from the map of
   * available {@link Drop}s.
   *
   * @param numberedAvailableDrops candidate {@link Drop}s for selection
   * @return selected {@link Drop#id}
   * @throws CommandException if the input doesn't match a number from the map of available {@link
   *     Drop}s
   */
  protected List<Long> getFromConsoleDropIds(Map<Long, Drop> numberedAvailableDrops)
      throws CommandException {
    consoleWriter.newLine().a("Enter Drop number to process").println();
    Long dropNumber = console.readLine(Long.class);

    if (!numberedAvailableDrops.containsKey(dropNumber)) {
      throw new CommandException(
          "Please enter a number from the list: " + numberedAvailableDrops.keySet());
    }

    Long selectId = numberedAvailableDrops.get(dropNumber).getId();

    return Arrays.asList(selectId);
  }

  protected List<Long> getAllFetchedDropIds(Map<Long, Drop> numberedAvailableDrops) {
    return numberedAvailableDrops.entrySet().stream()
        .filter(x -> !Boolean.TRUE.equals(x.getValue().getCanceled()))
        .map(x -> x.getValue().getId())
        .collect(Collectors.toList());
  }
}
