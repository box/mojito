package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.CancelDropConfig;
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

/**
 * Command to cancel a drop. Displays the list of drops available and ask the
 * user the drop id to be cancelled.
 *
 * @author jaurambault, wadimw
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"drop-cancel"}, commandDescription = "Cancel an exported drop")
public class DropCancelCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropCancelCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {"--drop-id", "-i"}, arity = 1, required = false, description = "ID of a drop to cancel (skip drop fetching and process only given ID)")
    Long dropId = null;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--number-drops-fetched"}, arity = 1, required = false, description = "Number of drops fetched")
    Long numberOfDropsToFetchParam = 10L;

    @Parameter(names = {"--show-all", "-all"}, required = false, description = "Show all drops (already imported drops are hidden by default)")
    Boolean alsoShowImportedParam = false;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    Console console;

    @Autowired
    DropClient dropClient;

    @Override
    public void execute() throws CommandException {
        if (dropId != null) {
            cancelSpecifiedDrop();
        } else {
            cancelDropsSelectingFromAvailableList();
        }
        consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
    }

    private void cancelSpecifiedDrop() throws CommandException {
        consoleWriter.newLine().a("Cancel drop: ").fg(Color.CYAN).a(dropId).println(2);

        CancelDropConfig cancelDropConfig = dropClient.cancelDrop(dropId);
        PollableTask task = cancelDropConfig.getPollableTask();

        commandHelper.waitForPollableTask(task.getId());
    }

    private void cancelDropsSelectingFromAvailableList() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);
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
                consoleWriter.newLine().a("Cancel drop: ").fg(Color.CYAN).a(dropId).reset().a(" in repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

                CancelDropConfig cancelDropConfig = dropClient.cancelDrop(dropId);
                PollableTask pollableTask = cancelDropConfig.getPollableTask();

                commandHelper.waitForPollableTask(pollableTask.getId());
            }
        }
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
        return getFromConsoleDropIds(numberedAvailableDrops);
    }

    private List<Long> getFromConsoleDropIds(Map<Long, Drop> numberedAvailableDrops) throws CommandException {
        consoleWriter.newLine().a("Enter Drop number to cancel").println();
        Long dropNumber = console.readLine(Long.class);

        if (!numberedAvailableDrops.containsKey(dropNumber)) {
            throw new CommandException("Please enter a number from the list: " + numberedAvailableDrops.keySet());
        }

        Long selectId = numberedAvailableDrops.get(dropNumber).getId();

        return Arrays.asList(selectId);
    }
}
