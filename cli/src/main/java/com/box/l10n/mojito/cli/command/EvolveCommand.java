package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.apiclient.AssetWsApiProxy;
import com.box.l10n.mojito.cli.apiclient.RepositoryWsApiProxy;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.cli.model.LocaleRepository;
import com.box.l10n.mojito.cli.model.LocalizedAssetBody;
import com.box.l10n.mojito.cli.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.cli.model.RepositoryRepository;
import com.box.l10n.mojito.cli.model.SourceAsset;
import com.box.l10n.mojito.evolve.Course;
import com.box.l10n.mojito.evolve.Evolve;
import com.box.l10n.mojito.io.Files;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.google.common.base.Preconditions;
import com.ibm.icu.util.ULocale;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(
    commandNames = {"evolve-sync"},
    commandDescription = "Synchronize Evolve courses with Mojito")
public class EvolveCommand extends Command {

  static final List<String> FILTER_OPTIONS =
      Arrays.asList(
          "usagesKeyPattern=_previewUrl",
          "noteKeepOrReplace=true",
          "noteKeyPattern=_previewUrl",
          "usagesKeepOrReplace=true",
          "extractAllPairs=false",
          "exceptions=_fields/.*/value",
          UUID.randomUUID().toString());

  /** logger */
  static Logger logger = LoggerFactory.getLogger(EvolveCommand.class);

  @Autowired ConsoleWriter consoleWriter;

  @Parameter(
      names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT},
      arity = 1,
      required = true,
      description = Param.REPOSITORY_DESCRIPTION)
  String repositoryParam;

  @Parameter(
      names = {"--state", "-s"},
      arity = 1,
      required = false,
      description = "Course status to be synced")
  String state = "inTranslation";

  @Parameter(
      names = {"--write-json-to"},
      arity = 1,
      required = false,
      description =
          "Directory to write localized json courses before they are sent. If not provided, don't write")
  String writeJsonTo = null;

  @Autowired(required = false)
  Evolve evolve;

  @Autowired RepositoryWsApiProxy repositoryClient;

  @Autowired AssetWsApiProxy assetClient;

  @Autowired PollableTaskClient pollableTaskClient;

  @Autowired CommandHelper commandHelper;

  @Override
  public boolean shouldShowInCommandList() {
    return false;
  }

  @Override
  public void execute() throws CommandException {
    checkEvolveConfiguration();

    consoleWriter
        .newLine()
        .a("Synchronize Evolve courses with status: ")
        .fg(Ansi.Color.CYAN)
        .a(state)
        .reset()
        .a(" with repository: ")
        .fg(Ansi.Color.CYAN)
        .a(repositoryParam)
        .println(2);

    RepositoryRepository repository = commandHelper.findRepositoryByName(repositoryParam);

    evolve
        .getCourses()
        .filter(course -> course.getState().equals(state))
        .forEach(
            course -> {
              translateCourse(repository, course);
            });
  }

  void translateCourse(RepositoryRepository repository, Course course) {

    consoleWriter
        .a("Get translations for course: ")
        .fg(Ansi.Color.CYAN)
        .a(course.getId())
        .println();
    String translationsByCourseId = evolve.getTranslationsByCourseId(course.getId());

    SourceAsset sourceAsset = sendSource(repository, course.getId(), translationsByCourseId);

    repository.getRepositoryLocales().stream()
        .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
        .map(RepositoryLocaleRepository::getLocale)
        .forEach(
            locale -> {
              consoleWriter
                  .a("Get localized course: ")
                  .fg(Ansi.Color.CYAN)
                  .a(locale.getBcp47Tag())
                  .println();
              String localizedCourse =
                  getLocalizedCourse(
                      sourceAsset.getAddedAssetId(), locale.getId(), translationsByCourseId);
              localizedCourse = removeBOMIfExists(localizedCourse);

              if (writeJsonTo != null) {
                writeJsonToFile(repository, course, locale, localizedCourse);
              }

              evolve.createCourseTranslationsById(
                  course.getId(),
                  localizedCourse,
                  locale.getBcp47Tag(),
                  isRightToLeft(locale.getBcp47Tag()));
            });
  }

  void writeJsonToFile(
      RepositoryRepository repository,
      Course course,
      LocaleRepository locale,
      String localizedCourse) {
    Preconditions.checkNotNull(writeJsonTo);
    Path path =
        Paths.get(
            writeJsonTo, repository.getName(), course.getId(), locale.getBcp47Tag() + ".json");
    Files.createDirectories(path.getParent());
    Files.write(path, localizedCourse);
  }

  SourceAsset sendSource(
      RepositoryRepository repository, String courseId, String translationsByCourseId) {
    consoleWriter.a("Sending source course to Mojito").println();
    SourceAsset sourceAsset = new SourceAsset();
    sourceAsset.setRepositoryId(repository.getId());
    sourceAsset.setPath(courseId + ".json");
    sourceAsset.setContent(translationsByCourseId);
    sourceAsset.setFilterOptions(FILTER_OPTIONS);
    sourceAsset = assetClient.importSourceAsset(sourceAsset);
    pollableTaskClient.waitForPollableTask(sourceAsset.getPollableTask().getId());
    return sourceAsset;
  }

  String getLocalizedCourse(Long assetId, long localeId, String translationsByCourseId) {
    LocalizedAssetBody localizedAssetBody = new LocalizedAssetBody();
    localizedAssetBody.setAssetId(assetId);
    localizedAssetBody.setLocaleId(localeId);
    localizedAssetBody.setContent(translationsByCourseId);
    localizedAssetBody.setOutputBcp47tag(null);
    localizedAssetBody.setFilterConfigIdOverride(null);
    localizedAssetBody.setFilterOptions(FILTER_OPTIONS);
    localizedAssetBody.setStatus(LocalizedAssetBody.StatusEnum.ALL);
    localizedAssetBody.setInheritanceMode(LocalizedAssetBody.InheritanceModeEnum.USE_PARENT);
    localizedAssetBody.setPullRunName(null);
    LocalizedAssetBody localizedAssetForContent =
        assetClient.getLocalizedAssetForContent(localizedAssetBody, assetId, localeId);

    return localizedAssetForContent.getContent();
  }

  boolean isRightToLeft(String bcp47Tag) {
    ULocale uLocale = ULocale.forLanguageTag(bcp47Tag);
    return uLocale.isRightToLeft();
  }

  String removeBOMIfExists(String course) {
    if (course.startsWith("\uFEFF")) {
      course = course.substring(1);
    }
    return course;
  }

  void checkEvolveConfiguration() throws CommandException {
    if (evolve == null) {
      throw new CommandException(
          "Evolve must be configured with: \"l10n.evolve.url\" and \"l10n.evolve.token\"");
    }
  }
}
