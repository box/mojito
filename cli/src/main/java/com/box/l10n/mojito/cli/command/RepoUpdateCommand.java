package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import static com.box.l10n.mojito.cli.command.RepoCommand.INTEGRITY_CHECK_LONG_PARAM;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.entity.IntegrityChecker;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
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
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"repo-update"}, commandDescription = "Updates a repository")
public class RepoUpdateCommand extends RepoCommand {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepoUpdateCommand.class);
          
    @Parameter(names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_NAME_DESCRIPTION)
    String nameParam;
    
    @Parameter(names = {Param.REPOSITORY_NEW_NAME_LONG, Param.REPOSITORY_NEW_NAME_SHORT}, arity = 1, required = false, description = Param.REPOSITORY_NAME_DESCRIPTION)
    String newNameParam;

    @Parameter(names = {Param.REPOSITORY_DESCRIPTION_LONG, Param.REPOSITORY_DESCRIPTION_SHORT}, arity = 1, required = false, description = Param.REPOSITORY_DESCRIPTION_DESCRIPTION)
    String descriptionParam;

    /**
     * Each individual locales would be added.  Bracket enclosed locale will set that locale to be partially translated.
     * Arrow (->) indicate inheritance, which the parent locale being referenced.
     * <p/>Example:
     * <code>
     * "fr-FR" "(fr-CA)->fr-FR" "en-GB" "(en-CA)->en-GB" "en-AU"
     * 1. Adds: fr-Fr, fr-CA, en-GB, en-CA, en-AU
     * 2. fr-CA and en-CA are children locale of fr-FR and en-GB and do not need to be fully translated
     * </code>
     */
    @Parameter(names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT}, variableArity = true, required = false, description = Param.REPOSITORY_LOCALES_DESCRIPTION)
    List<String> encodedBcp47Tags;

    /**
     * Comma seperated by "FILE_EXTENSION:CHECKER_TYPE"
     * "properties:message-format,xliff:sprintf"
     * <p/>
     * For all available checker types: com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType
     */
    @Parameter(names = {INTEGRITY_CHECK_LONG_PARAM, INTEGRITY_CHECK_SHORT_PARAM}, arity = 1, required = false,
            description = INTEGRITY_CHECK_DESCRIPTION)
    String integrityCheckParam;

    @Override
    public void execute() throws CommandException {        
        consoleWriter.a("Update repository: ").fg(Ansi.Color.CYAN).a(nameParam).println();

        Set<RepositoryLocale> repositoryLocales = localeHelper.extractRepositoryLocalesFromInput(encodedBcp47Tags, true);
        Set<IntegrityChecker> integrityCheckers = extractIntegrityCheckersFromInput(integrityCheckParam, true);

        try {
            repositoryClient.updateRepository(nameParam, newNameParam, descriptionParam, repositoryLocales, integrityCheckers);
            consoleWriter.newLine().a("updated --> repository name: ").fg(Ansi.Color.MAGENTA).a(nameParam).println();
        } catch (RepositoryNotFoundException ex) {
            throw new CommandException("Repository not found: " + nameParam, ex);
        }
    }
    
}
