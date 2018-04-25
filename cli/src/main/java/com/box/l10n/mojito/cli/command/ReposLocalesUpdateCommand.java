package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 *
 * Command to update locales in a repository.
 *
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"repos-locales-update"}, commandDescription = "Updates locales accross multiple repositories")
public class ReposLocalesUpdateCommand extends RepoCommand {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ReposLocalesUpdateCommand.class);

    /**
     * Each individual locales would be added. Bracket enclosed locale will set
     * that locale to be partially translated. Arrow (->) indicate inheritance,
     * which the parent locale being referenced.
     * <p/>
     * Example:      <code>
     * "fr-FR" "(fr-CA)->fr-FR" "en-GB" "(en-CA)->en-GB" "en-AU"
     * 1. Adds: fr-Fr, fr-CA, en-GB, en-CA, en-AU
     * 2. fr-CA and en-CA are children locale of fr-FR and en-GB and do not need to be fully translated
     * </code>
     */
    @Parameter(names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT}, variableArity = true, required = false, description = Param.REPOSITORY_LOCALES_DESCRIPTION)
    List<String> encodedBcp47Tags;

    @Parameter(names = {"--repository-names", "-rns"}, variableArity = true, required = false, description = "Filter repository names to update (if none, update all repositories)")
    List<String> repositoryNames;

    @Override
    public void execute() throws CommandException {

        try {
            List<Repository> repositoriesToUpdate = getRepositoriesToUpdate(repositoryNames);
            Set<RepositoryLocale> repositoryLocales = localeHelper.extractRepositoryLocalesFromInput(encodedBcp47Tags, true);

            consoleWriter.a("Update locales in repositories").println();
            for (Repository repository : repositoriesToUpdate) {
                repositoryClient.updateRepository(repository.getName(), null, null, repositoryLocales, null);
                consoleWriter.a(" - updated repository: ").fg(Ansi.Color.MAGENTA).a(repository.getName()).println();
            }
            
        } catch (ParameterException | RepositoryNotFoundException | ResourceNotUpdatedException ex) {
            throw new CommandException(ex.getMessage(), ex);
        }
    }

    List<Repository> getRepositoriesToUpdate(List<String> repositoryNames) {
        List<Repository> repositoriesForUpdate = new ArrayList<>();

        List<Repository> allRepositories = repositoryClient.getRepositories(null);
        
        if (repositoryNames != null) {
            HashSet<String> repositoryNamesAsSet = new HashSet<>(repositoryNames);
            for (Repository allRepository : allRepositories) {
                if (repositoryNamesAsSet.contains(allRepository.getName())) {
                    repositoriesForUpdate.add(allRepository);
                }
            }
        } else {
            repositoriesForUpdate = allRepositories;
        }

        return repositoriesForUpdate;
    }

}
