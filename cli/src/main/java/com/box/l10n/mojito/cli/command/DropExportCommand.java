package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.ConsoleWriter;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.DropClient;
import com.box.l10n.mojito.rest.entity.ExportDropConfig;
import com.box.l10n.mojito.rest.entity.PollableTask;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import java.util.ArrayList;
import java.util.List;
import org.fusesource.jansi.Ansi.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Command to create a drop for a repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"drop-export"}, commandDescription = "Export a drop for translation")
public class DropExportCommand extends Command {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(DropExportCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--type", "-t"}, arity = 1, required = false, description = "Type of export to perfom: TRANSLATION or REVIEW", converter = ExportDropConfigType.class)
    ExportDropConfig.Type typeParam;

    @Parameter(names = {"--locales", "-l"}, arity = 1, required = false, description = "List of locales to be exported, format: fr-FR,ja-JP")
    List<String> bcp47tagsParam;

    @Autowired
    CommandHelper commandHelper;

    @Autowired
    DropClient dropClient;

    @Override
    public void execute() throws CommandException {

        consoleWriter.newLine().a("Export a Drop from repository: ").fg(Color.CYAN).a(repositoryParam).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        ExportDropConfig exportDropConfig = new ExportDropConfig();
        exportDropConfig.setRepositoryId(repository.getId());
        exportDropConfig.setType(typeParam);
        exportDropConfig.setBcp47Tags(getBcp47TagsForExport(repository));

        exportDropConfig = dropClient.exportDrop(exportDropConfig);

        consoleWriter.a("Drop id: ").fg(Color.CYAN).a(exportDropConfig.getDropId()).print();

        PollableTask pollableTask = exportDropConfig.getPollableTask();
        commandHelper.waitForPollableTask(pollableTask.getId());

        consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
    }

    /**
     * Gets the list of Bcp47Tags for the drop export.
     *
     * Takes the list of tags provided by the user (with validation) or get the
     * list from the repository.
     *
     * @param repository
     * @return list of Bcp47Tags for the drop
     */
    List<String> getBcp47TagsForExport(Repository repository) throws CommandException {

        List<String> bcp47tags;

        if (bcp47tagsParam == null) {
            bcp47tags = getBcp47TagsForExportFromRepository(repository);
        } else {
            bcp47tags = getBcp47TagsForExportFromParam(repository);
        }

        return bcp47tags;
    }

    /**
     * Gets the list of Bcp47Tags for the drop export from the repository.
     *
     * @param repository
     * @return list of Bcp47Tags for the drop
     * @throws CommandException
     */
    List<String> getBcp47TagsForExportFromRepository(Repository repository) {

        List<String> bcp47Tags = new ArrayList<>();

        for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
            if (repositoryLocale.isToBeFullyTranslated()) {
                bcp47Tags.add(repositoryLocale.getLocale().getBcp47Tag());
            }
        }

        return bcp47Tags;
    }

    /**
     * Gets the list of Bcp47Tags for the drop export from the param.
     *
     * @param repository
     * @return list of Bcp47Tags for the drop
     * @throws CommandException
     */
    List<String> getBcp47TagsForExportFromParam(Repository repository) throws CommandException {

        List<String> bcp47tags = new ArrayList<>();
        
        List<String> validTags = new ArrayList<>();

        for (RepositoryLocale repositoryLocale : repository.getRepositoryLocales()) {
            validTags.add(repositoryLocale.getLocale().getBcp47Tag());
        }

        if (validTags.containsAll(bcp47tagsParam)) {
            bcp47tags.addAll(bcp47tagsParam);
        } else {
            bcp47tagsParam.removeAll(validTags);
            String invalidLocales = StringUtils.collectionToDelimitedString(bcp47tagsParam, ", ");
            throw new CommandException("Locales [" + invalidLocales + "] do not exist in the repository");
        }

        return bcp47tags;
    }

}
