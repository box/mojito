package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.apiclient.DropWsApi;
import com.box.l10n.mojito.apiclient.model.ExportDropConfig;
import com.box.l10n.mojito.apiclient.model.PollableTask;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleStatisticRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryStatisticRepository;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
@Parameters(
    commandNames = {"drop-export"},
    commandDescription = "Export a drop for translation")
public class DropExportCommand extends Command {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(DropExportCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--type", "-t"},
      arity = 1,
      required = false,
      description = "Type of export to perfom: TRANSLATION or REVIEW",
      converter = ExportDropConfigTypeConverter.class)
  ExportDropConfig.TypeEnum typeParam;

  @Parameter(
      names = {Param.EXPORT_LOCALES_LONG, Param.EXPORT_LOCALES_SHORT},
      arity = 1,
      required = false,
      description = Param.EXPORT_LOCALES_DESCRIPTION)
  List<String> bcp47tagsParam;

  @Parameter(
      names = {"--use-inheritance"},
      required = false,
      description =
          "To export with translations inherited from parent locale (only used with REVIEW type)")
  Boolean useInheritance = false;

  @Autowired CommandHelper commandHelper;

  @Autowired DropWsApi dropClient;

  @Override
  public void execute() throws CommandException {

    consoleWriter
        .newLine()
        .a("Export a Drop from repository: ")
        .fg(Color.CYAN)
        .a(repositoryParam)
        .println(2);

    RepositoryRepository repository = commandHelper.findRepositoryByName(repositoryParam);

    if (useInheritance && typeParam != ExportDropConfig.TypeEnum.REVIEW) {
      throw new CommandException("--use-inheritance can only be used with --type REVIEW");
    }

    if (typeParam == ExportDropConfig.TypeEnum.REVIEW || shouldCreateDrop(repository)) {
      ExportDropConfig exportDropConfig = new ExportDropConfig();
      exportDropConfig.setRepositoryId(repository.getId());
      exportDropConfig.setType(typeParam);
      exportDropConfig.setLocales(getBcp47TagsForExport(repository));
      exportDropConfig.setUseInheritance(useInheritance);

      exportDropConfig = dropClient.exportDrop(exportDropConfig);

      consoleWriter.a("Drop id: ").fg(Color.CYAN).a(exportDropConfig.getDropId()).print();

      PollableTask pollableTask = exportDropConfig.getPollableTask();
      commandHelper.waitForPollableTask(pollableTask.getId());

      consoleWriter.newLine().fg(Color.GREEN).a("Finished").println(2);
    } else {
      consoleWriter
          .newLine()
          .fg(Color.GREEN)
          .a("Repository is already fully translated")
          .println(2);
    }
  }

  /**
   * Gets the list of Bcp47Tags for the drop export.
   *
   * <p>Takes the list of tags provided by the user (with validation) or get the list from the
   * repository.
   *
   * @param repository
   * @return list of Bcp47Tags for the drop
   */
  List<String> getBcp47TagsForExport(RepositoryRepository repository) throws CommandException {

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
  List<String> getBcp47TagsForExportFromRepository(RepositoryRepository repository) {

    List<String> bcp47Tags = new ArrayList<>();

    for (RepositoryLocaleRepository repositoryLocale : repository.getRepositoryLocales()) {
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
  List<String> getBcp47TagsForExportFromParam(RepositoryRepository repository)
      throws CommandException {

    List<String> bcp47tags = new ArrayList<>();

    List<String> validTags = new ArrayList<>();

    for (RepositoryLocaleRepository repositoryLocale : repository.getRepositoryLocales()) {
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

  protected boolean shouldCreateDrop(RepositoryRepository repository) {
    boolean createDrop = false;
    Set<String> bcp47TagsForTranslation =
        Sets.newHashSet(getBcp47TagsForExportFromRepository(repository));
    RepositoryStatisticRepository repoStat = repository.getRepositoryStatistic();
    if (repoStat != null) {
      for (RepositoryLocaleStatisticRepository repoLocaleStat :
          repoStat.getRepositoryLocaleStatistics()) {
        if (bcp47TagsForTranslation.contains(repoLocaleStat.getLocale().getBcp47Tag())
            && repoLocaleStat.getForTranslationCount() > 0L) {
          createDrop = true;
          break;
        }
      }
    }
    return createDrop;
  }
}
