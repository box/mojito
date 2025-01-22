package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.model.AssetIntegrityChecker;
import com.box.l10n.mojito.cli.model.Repository;
import com.box.l10n.mojito.cli.model.RepositoryLocale;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.rest.client.exception.RepositoryNotFoundException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotUpdatedException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Command to update locales in a repository.
 *
 * @author jyi
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"repo-update"},
    commandDescription = "Updates a repository")
public class RepoUpdateCommand extends RepoCommand {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepoUpdateCommand.class);

  @Parameter(
      names = {Param.REPOSITORY_NAME_LONG, Param.REPOSITORY_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String nameParam;

  @Parameter(
      names = {Param.REPOSITORY_NEW_NAME_LONG, Param.REPOSITORY_NEW_NAME_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_NAME_DESCRIPTION)
  String newNameParam;

  @Parameter(
      names = {Param.REPOSITORY_DESCRIPTION_LONG, Param.REPOSITORY_DESCRIPTION_SHORT},
      arity = 1,
      required = false,
      description = Param.REPOSITORY_DESCRIPTION_DESCRIPTION)
  String descriptionParam;

  @Parameter(
      names = {Param.CHECK_SLA_LONG, Param.CHECK_SLA_SHORT},
      arity = 1,
      required = false,
      description = Param.CHECK_SLA_DESCRIPTION)
  Boolean checkSLA;

  /**
   * Each individual locales would be added. Bracket enclosed locale will set that locale to be
   * partially translated. Arrow (->) indicate inheritance, which the parent locale being
   * referenced.
   *
   * <p>Example: <code>
   * "fr-FR" "(fr-CA)->fr-FR" "en-GB" "(en-CA)->en-GB" "en-AU"
   * 1. Adds: fr-Fr, fr-CA, en-GB, en-CA, en-AU
   * 2. fr-CA and en-CA are children locale of fr-FR and en-GB and do not need to be fully translated
   * </code>
   */
  @Parameter(
      names = {Param.REPOSITORY_LOCALES_LONG, Param.REPOSITORY_LOCALES_SHORT},
      variableArity = true,
      required = false,
      description = Param.REPOSITORY_LOCALES_DESCRIPTION)
  List<String> encodedBcp47Tags;

  @Parameter(
      names = {"--repository-names", "-rns"},
      variableArity = true,
      required = false,
      description = "Filter repository names to update (if none, update all repositories)")
  List<String> repositoryNames;

  /**
   * Comma seperated by "FILE_EXTENSION:CHECKER_TYPE" "properties:message-format,xliff:sprintf"
   *
   * <p>For all available checker types:
   * com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType
   */
  @Parameter(
      names = {INTEGRITY_CHECK_LONG_PARAM, INTEGRITY_CHECK_SHORT_PARAM},
      arity = 1,
      required = false,
      description = INTEGRITY_CHECK_DESCRIPTION)
  String integrityCheckParam;

  public void checkRepositoryParams() throws CommandException {
    if (repositoryNames == null && nameParam == null) {
      throw new CommandException(
          "Must provide one of the following options: --name, --repositoryNames");
    } else if (repositoryNames != null && (nameParam != null || newNameParam != null)) {
      throw new CommandException(
          "Can't use --repository-names option with --name or --new-name options");
    }
  }

  @Override
  public void execute() throws CommandException {

    checkRepositoryParams();

    if (nameParam != null) {
      repositoryNames = Arrays.asList(nameParam);
    }

    updateRepositoriesForRepositoryNames();
  }

  public void updateRepositoriesForRepositoryNames() throws CommandException {
    try {
      List<RepositoryRepository> repositoriesForUpdate = getRepositoriesToUpdateFromParams();

      List<RepositoryLocale> repositoryLocales =
          localeHelper.extractRepositoryLocalesFromInput(encodedBcp47Tags, true);
      List<AssetIntegrityChecker> integrityCheckers =
          extractIntegrityCheckersFromInput(integrityCheckParam, true);

      consoleWriter.a("Update repositories").println();
      for (RepositoryRepository repository : repositoriesForUpdate) {
        Repository repositoryBody = new Repository();
        repositoryBody.setDescription(descriptionParam);
        repositoryBody.setName(newNameParam);
        repositoryBody.setRepositoryLocales(repositoryLocales);
        repositoryBody.setCheckSLA(checkSLA);
        if (integrityCheckers != null) {
          repositoryBody.setAssetIntegrityCheckers(integrityCheckers);
        }
        repositoryClient.updateRepository(repository.getName(), repositoryBody);
        consoleWriter
            .newLine()
            .a("updated --> repository name: ")
            .fg(Ansi.Color.MAGENTA)
            .a(repository.getName())
            .println();
      }

    } catch (ParameterException | RepositoryNotFoundException | ResourceNotUpdatedException ex) {
      throw new CommandException(ex.getMessage(), ex);
    }
  }

  List<RepositoryRepository> getRepositoriesToUpdateFromParams() throws CommandException {
    List<RepositoryRepository> repositoriesForUpdate = new ArrayList<>();

    List<RepositoryRepository> allRepositories = repositoryClient.getRepositories(null);

    if (repositoryNames != null) {
      HashSet<String> repositoryNamesAsSet = new HashSet<>(repositoryNames);
      for (RepositoryRepository allRepository : allRepositories) {
        if (repositoryNamesAsSet.contains(allRepository.getName())) {
          repositoriesForUpdate.add(allRepository);
        }
      }
    } else {
      repositoriesForUpdate = allRepositories;
    }

    if (repositoriesForUpdate.isEmpty()) {
      if (nameParam != null) {
        throw new CommandException("Repository with name [" + nameParam + "] is not found");
      } else {
        throw new CommandException("No repositories found");
      }
    }

    return repositoriesForUpdate;
  }
}
