package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.Console;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.Drop;
import com.box.l10n.mojito.rest.entity.Repository;
import org.fusesource.jansi.Ansi.Color;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description =
            Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--number-drops-fetched"}, arity = 1, required = false, description = "Number of drops fetched")
    Long numberOfDropsToFetchParam = 10L;

    @Parameter(names = {"--show-all", "-all"}, required = false, description = "Show drops with all statuses (already" +
            " imported drops are hidden by default)")
    Boolean alsoShowImportedParam = false;

    @Parameter(names = {"--drop-id", "-i"}, required = false, arity = 1, description = "Show information only about " +
            "the drop with the given id")
    Long dropId = null;

    @Parameter(names = {"--csv"}, required = false, description = "When showing drops, output info in csv format so " +
            "that it is easily parsable by scripts.")
    Boolean showCsvOutput = false;

    @Parameter(names = {"--json"}, required = false, description = "When showing drops, output info in json format so" +
            " that it is easily parsable by scripts.")
    Boolean showJsonOutput = false;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    Console console;

    @Autowired
    DropClient dropClient;

    @Override
    public void execute() throws CommandException {
        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        Map<Long, Drop> numberedAvailableDrops;
        if (dropId != null) {
            // TODO: fetch info only for the specified drop instead of fetching an arbitrary page and hoping it's there
            Optional<Drop> drop = getNumberedAvailableDrops(repository.getId()).values()
                    .stream().filter(d -> d.getId().equals(dropId)).findAny();
            numberedAvailableDrops = drop.map(value -> Collections.singletonMap(1L, value))
                    .orElse(Collections.emptyMap());
        } else {
            numberedAvailableDrops = getNumberedAvailableDrops(repository.getId());
        }

        if (Boolean.TRUE.equals(showCsvOutput)) {
            writeCsv(numberedAvailableDrops);
        } else if (Boolean.TRUE.equals(showJsonOutput)) {
            writeJson(numberedAvailableDrops);
        } else {
            writeHumanReadable(numberedAvailableDrops);
        }


    }

    /**
     * Outputs available drops to a structured output writer
     */
    private void writeJson(Map<Long, Drop> drops) {
        JSONArray jsonDrops = new JSONArray();
        for (Drop drop : drops.values()) {
            JSONObject jsonDrop = new JSONObject();
            jsonDrop.put("id", drop.getId());
            jsonDrop.put("name", drop.getName());
            jsonDrop.put("lastImportedDate", drop.getLastImportedDate());
            jsonDrop.put("canceled", drop.getCanceled());
            jsonDrops.add(jsonDrop);
        }

        consoleWriter.a(jsonDrops.toJSONString()).print();
    }

    private void writeCsv(Map<Long, Drop> drops) {
        StringBuilder csv = new StringBuilder();
        csv.append("id,name,importStatus");

        for (Drop drop : drops.values()) {
            csv.append(drop.getId()).append(",").append(drop.getName());

            if (Boolean.TRUE.equals(drop.getCanceled())) {
                csv.append(",CANCELED");
            } else if (drop.getLastImportedDate() == null) {
                csv.append(",NEW");
            } else {
                csv.append(",").append(drop.getLastImportedDate());
            }
        }

        consoleWriter.a(csv.toString());
    }

    private void writeHumanReadable(Map<Long, Drop> drops) {
        if (drops.isEmpty()) {
            consoleWriter.newLine().a("No drop available").println();
        } else {
            consoleWriter.newLine().a("Drops available").println();

            logger.debug("Display drops information");
            for (Map.Entry<Long, Drop> entry : drops.entrySet()) {

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
}
