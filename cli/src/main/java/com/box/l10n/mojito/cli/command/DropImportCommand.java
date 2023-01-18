package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to import a drop. Displays the list of drops available and ask the
 * user the drop id to be imported.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"drop-import"}, commandDescription = "Import a translated drop")
public class DropImportCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropImportCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--number-drops-fetched"}, arity = 1, required = false, description = "Number of drops fetched")
    Long numberOfDropsToFetchParam = 10L;

    @Parameter(names = {"--show-all", "-all"}, required = false, description = "Show all drops (already imported drops are hidden by default)")
    Boolean alsoShowImportedParam = false;

    @Parameter(names = {Param.DROP_IMPORT_STATUS}, required = false, description = Param.DROP_IMPORT_STATUS_DESCRIPTION,
            converter = ImportDropConfigStatusConverter.class)
    ImportDropConfig.Status importStatusParam = null;

    @Parameter(names = {"--import-fetched"}, required = false, description = "Import all fetched drops")
    Boolean importFetchedParam = false;

    @Parameter(names = {"--import-drop-id", "-i"}, arity = 1, required = false, description = "Give the drop id to import")
    Long dropId = -1L;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    Console console;

    @Autowired
    DropClient dropClient;

    @Override
    public void execute() throws CommandException {

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        if (dropId == -1L) {
            Map<Long, Drop> numberedAvailableDrops = getNumberedAvailableDrops(repository.getId());

            if (numberedAvailableDrops.isEmpty()) {
                consoleWriter.newLine().a("No drop available").println();
            } else {
                consoleWriter.newLine().a("Drops available").println();

                logger.debug("Display drops information");
                for (Map.Entry<Long, Drop> entry : numberedAvailableDrops.entrySet()) {

                    Drop drop = entry.getValue();

                    consoleWriter.a("  ").fg(Color.CYAN).a(entry.getKey()).reset().
                            a(" - id: ").fg(Color.MAGENTA).a(drop.getId()).reset().
                            a(", name: ").fg(Color.MAGENTA).a(drop.getName()).reset();

                    if (Boolean.TRUE.equals(drop.getCanceled())) {
                        consoleWriter.fg(Color.GREEN).a(" CANCELED");
                    } else if (drop.getLastImportedDate() == null) {
                        consoleWriter.fg(Color.GREEN).a(" NEW");
                    } else {
                        consoleWriter.a(", last import: ").fg(Color.MAGENTA).a(drop.getLastImportedDate());
                    }

                    consoleWriter.println();
                }

                List<Long> dropIds = getSelectedDropIds(numberedAvailableDrops);

                for(Long dropId: dropIds) {
                    consoleWriter.newLine().a("Import drop: ").fg(Color.CYAN).a(dropId).reset().a(" in repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

                    ImportDropConfig importDropConfig = dropClient.importDrop(repository, dropId, importStatusParam);
                    PollableTask pollableTask = importDropConfig.getPollableTask();

                    commandHelper.waitForPollableTask(pollableTask.getId());
                }
            }
        } else {
            consoleWriter.newLine().a("Import drop: ").fg(Color.CYAN).a(dropId).reset().a(" in repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

            ImportDropConfig importDropConfig = dropClient.importDrop(repository, dropId, importStatusParam);
            PollableTask task = importDropConfig.getPollableTask();

            commandHelper.waitForPollableTask(task.getId());
        }
        consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
    }

    /**
     * Gets available {@link Drop}s and assign them a number (map key) to be
     * referenced in the console input for selection.
     *
     * @return
     */
    private Map<Long, Drop> getNumberedAvailableDrops(Long repositoryId) {

        logger.debug("Build a map of drops keyed by an incremented integer");
        Map<Long, Drop> dropIds = new HashMap<>();

        long i = 1;

        for (Drop availableDrop : dropClient.getDrops(repositoryId, getImportedFilter(), 0L, numberOfDropsToFetchParam).getContent()) {
            dropIds.put(i++, availableDrop);
        }

        return dropIds;
    }

    /**
     * Returns the "imported" filter to be passed to {@link DropClient#getDrops(java.lang.Long, java.lang.Boolean, java.lang.Long, java.lang.Long)
     * } based on the CLI parameter {@link #alsoShowImportedParam}.
     *
     * @return the imported filter to get drops
     */
    private Boolean getImportedFilter() {
        return alsoShowImportedParam ? null : false;
    }

    /**
     * Gets the list of selected {@link Drop#id}.
     *
     * <p>
     * First, reads a drop number from the console and then gets the
     * {@link Drop} from the map of available {@link Drop}s.
     *
     * @param numberedAvailableDrops candidate {@link Drop}s for selection
     * @return selected {@link Drop#id}
     * @throws CommandException if the input doesn't match a number from the map
     * of available {@link Drop}s
     */
    private List<Long> getSelectedDropIds(Map<Long, Drop> numberedAvailableDrops) throws CommandException {
        List<Long> selectedDropIds;

        if (importFetchedParam) {
            selectedDropIds = getWithImportFetchedDropIds(numberedAvailableDrops);
        } else {
            selectedDropIds = getFromConsoleDropIds(numberedAvailableDrops);
        }

        return selectedDropIds;
    }

    private List<Long> getFromConsoleDropIds(Map<Long, Drop> numberedAvailableDrops) throws CommandException {
        consoleWriter.newLine().a("Enter Drop number to import").println();
        Long dropNumber = console.readLine(Long.class);

        if (!numberedAvailableDrops.containsKey(dropNumber)) {
            throw new CommandException("Please enter a number from the list: " + numberedAvailableDrops.keySet());
        }

        Long selectId = numberedAvailableDrops.get(dropNumber).getId();

        return Arrays.asList(selectId);
    }

    private List<Long> getWithImportFetchedDropIds(Map<Long, Drop> numberedAvailableDrops) {
        return numberedAvailableDrops.entrySet().stream()
                .filter(x -> !Boolean.TRUE.equals(x.getValue().getCanceled()))
                .map(x -> x.getValue().getId())
                .collect(Collectors.toList());
    }

}
