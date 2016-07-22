package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.Console;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.ImportDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import java.util.HashMap;
import java.util.Map;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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
            converter = ImportDropConfigStatus.class)
    ImportDropConfig.Status importStatusParam = null;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    Console console;

    @Autowired
    DropClient dropClient;

    @Override
    public void execute() throws CommandException {

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

            Long dropId = getSelectedDropId(numberedAvailableDrops);

            consoleWriter.newLine().a("Import drop: ").fg(Color.CYAN).a(dropId).reset().a(" in repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

            ImportDropConfig importDropConfig = dropClient.importDrop(repository, dropId, importStatusParam);
            PollableTask pollableTask = importDropConfig.getPollableTask();

            commandHelper.waitForPollableTask(pollableTask.getId());
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
     * Gets the selected {@link Drop#id} matching the user input.
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
    private Long getSelectedDropId(Map<Long, Drop> numberedAvailableDrops) throws CommandException {
        consoleWriter.newLine().a("Enter Drop number to import").println();
        Long dropNumber = console.readLine(Long.class);

        if (!numberedAvailableDrops.containsKey(dropNumber)) {
            throw new CommandException("Please enter a number from the list: " + numberedAvailableDrops.keySet());
        }

        return numberedAvailableDrops.get(dropNumber).getId();
    }

}
