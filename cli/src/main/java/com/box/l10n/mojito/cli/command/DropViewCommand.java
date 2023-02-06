package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Command to view info about drops. Displays the list of drops available.
 *
 * @author ehoogerbeets
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"drop-view"}, commandDescription = "View info about drops")
public class DropViewCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropViewCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--number-drops-fetched"}, arity = 1, required = false, description = "Number of drops fetched")
    Long numberOfDropsToFetchParam = 10L;

    @Parameter(names = {"--show-all", "-all"}, required = false, description = "Show all drops (already imported drops are hidden by default)")
    Boolean alsoShowImportedParam = false;

    @Parameter(names = {"--csv"}, required = false, description = "When showing drops, output info in csv format so that it is easily parsable by scripts.")
    Boolean showCsvOutput = false;

    @Parameter(names = {"--json"}, required = false, description = "When showing drops, output info in json format so that it is easily parsable by scripts.")
    Boolean showJsonOutput = false;

    @Parameter(names = {"--dropid", "-i"}, required = false, arity = 1, description = "Show information only about the drop with the given id")
    Long dropid = -1L;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    Console console;

    @Autowired
    DropClient dropClient;

    private void outputInfo(Drop drop) {
        if (dropid != -1 && drop.getId().longValue() != dropid.longValue()) return;
        if (showCsvOutput) {
            consoleWriter.a(drop.getId()).a(",").a(drop.getName());

            if (Boolean.TRUE.equals(drop.getCanceled())) {
                consoleWriter.a(",CANCELED");
            } else if (drop.getLastImportedDate() == null) {
                consoleWriter.a(",NEW");
            } else {
                consoleWriter.a(",").a(drop.getLastImportedDate());
            }
        } else {
            consoleWriter.
                    a("id: ").fg(Color.MAGENTA).a(drop.getId()).reset().
                    a(", name: ").fg(Color.MAGENTA).a(drop.getName()).reset();

            if (Boolean.TRUE.equals(drop.getCanceled())) {
                consoleWriter.fg(Color.GREEN).a(" CANCELED");
            } else if (drop.getLastImportedDate() == null) {
                consoleWriter.fg(Color.GREEN).a(" NEW");
            } else {
                consoleWriter.a(", last import: ").fg(Color.MAGENTA).a(drop.getLastImportedDate());
            }
        }
        consoleWriter.println();
    }

    @Override
    public void execute() throws CommandException {

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        Map<Long, Drop> numberedAvailableDrops = getNumberedAvailableDrops(repository.getId());

        if (numberedAvailableDrops.isEmpty()) {
            if (showCsvOutput) {
                consoleWriter.println();
            } else if (showJsonOutput) {
                consoleWriter.a("{}").println();
            } else {
                consoleWriter.newLine().a("No drop available").println();
            }
        } else {
            if (!showCsvOutput && !showJsonOutput) consoleWriter.newLine().a("Drops available").println();

            logger.debug("Display drops information");
            if (showJsonOutput) {
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    String value = objectMapper.writeValueAsString(
                        (dropid == -1) ?
                            numberedAvailableDrops :
                            numberedAvailableDrops.get(dropid)
                    );
                    consoleWriter.a(value).println();
                } catch (JsonProcessingException e) {
                    System.out.println(e);
                    throw new CommandException(e);
                }
            } else {
                for (Map.Entry<Long, Drop> entry : numberedAvailableDrops.entrySet()) {

                    Drop drop = entry.getValue();

                    outputInfo(drop);
                }
            }
        }
    }

    /**
     * Gets available {@link Drop}s and returns a map where the drop id is
     * the map key.
     *
     * @return
     */
    private Map<Long, Drop> getNumberedAvailableDrops(Long repositoryId) {

        logger.debug("Build a map of drops keyed by the dropId");
        Map<Long, Drop> dropIds = new HashMap<>();

        long i = 1;

        for (Drop availableDrop : dropClient.getDrops(repositoryId, getImportedFilter(), 0L, numberOfDropsToFetchParam).getContent()) {
            dropIds.put(availableDrop.getId(), availableDrop);
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

    private List<Long> getWithImportFetchedDropIds(Map<Long, Drop> numberedAvailableDrops) {
        return numberedAvailableDrops.entrySet().stream()
                .filter(x -> !Boolean.TRUE.equals(x.getValue().getCanceled()))
                .map(x -> x.getValue().getId())
                .collect(Collectors.toList());
    }

}
