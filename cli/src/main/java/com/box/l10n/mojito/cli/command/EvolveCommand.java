package com.box.l10n.mojito.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.box.l10n.mojito.cli.command.param.Param;
import com.box.l10n.mojito.cli.console.ConsoleWriter;
import com.box.l10n.mojito.evolve.Course;
import com.box.l10n.mojito.evolve.Evolve;
import com.box.l10n.mojito.rest.client.AssetClient;
import com.box.l10n.mojito.rest.client.PollableTaskClient;
import com.box.l10n.mojito.rest.client.RepositoryClient;
import com.box.l10n.mojito.rest.entity.LocalizedAssetBody;
import com.box.l10n.mojito.rest.entity.Repository;
import com.box.l10n.mojito.rest.entity.RepositoryLocale;
import com.box.l10n.mojito.rest.entity.SourceAsset;
import com.ibm.icu.util.ULocale;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author jaurambault
 */
@Component
@Scope("prototype")
@Parameters(commandNames = {"evolve-sync"}, commandDescription = "Synchronize Evolve courses with Mojito")
public class EvolveCommand extends Command {

    static final List<String> FILTER_OPTIONS = Arrays.asList("usagesKeyPattern=_previewUrl", "noteKeepOrReplace=true",
            "noteKeyPattern=_previewUrl", "usagesKeepOrReplace=true",
            "extractAllPairs=false", "exceptions=_fields/.*/value", UUID.randomUUID().toString());
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(EvolveCommand.class);

    @Autowired
    ConsoleWriter consoleWriter;

    @Parameter(names = {Param.REPOSITORY_LONG, Param.REPOSITORY_SHORT}, arity = 1, required = true, description = Param.REPOSITORY_DESCRIPTION)
    String repositoryParam;

    @Parameter(names = {"--state", "-s"}, arity = 1, required = false, description = "Course status to be synced")
    String state = "inTranslation";

    @Autowired(required = false)
    Evolve evolve;

    @Autowired
    RepositoryClient repositoryClient;

    @Autowired
    AssetClient assetClient;

    @Autowired
    PollableTaskClient pollableTaskClient;

    @Autowired
    CommandHelper commandHelper;

    @Override
    public void execute() throws CommandException {
        checkEvolveConfiguration();

        consoleWriter.newLine().a("Synchronize Evolve courses with status: ").fg(Ansi.Color.CYAN).a(state).
                reset().a(" with repository: ").fg(Ansi.Color.CYAN).a(repositoryParam).println(2);

        Repository repository = commandHelper.findRepositoryByName(repositoryParam);

        evolve.getCourses()
                .filter(course -> course.getState().equals(state))
                .forEach(course -> {
                    translateCourse(repository, course);
                });
    }

    void translateCourse(Repository repository, Course course) {

        consoleWriter.a("Get translations for course: ").fg(Ansi.Color.CYAN).a(course.getId()).println();
        String translationsByCourseId = evolve.getTranslationsByCourseId(course.getId());

        SourceAsset sourceAsset = sendSource(repository, course.getId(), translationsByCourseId);

        repository.getRepositoryLocales().stream()
                .filter(repositoryLocale -> repositoryLocale.getParentLocale() != null)
                .map(RepositoryLocale::getLocale).forEach(locale -> {
            consoleWriter.a("Get localized course: ").fg(Ansi.Color.CYAN).a(locale.getBcp47Tag()).println();
            String localizedCourse = getLocalizedCourse(sourceAsset.getAddedAssetId(), locale.getId(), translationsByCourseId);
            localizedCourse = removeBOMIfExists(localizedCourse);
            evolve.createCourseTranslationsById(course.getId(), localizedCourse, locale.getBcp47Tag(), isRightToLeft(locale.getBcp47Tag()));
        });
    }

    SourceAsset sendSource(Repository repository, String courseId, String translationsByCourseId) {
        consoleWriter.a("Sending source course to Mojito").println();
        SourceAsset sourceAsset = new SourceAsset();
        sourceAsset.setRepositoryId(repository.getId());
        sourceAsset.setPath(courseId + ".json");
        sourceAsset.setContent(translationsByCourseId);
        sourceAsset.setFilterOptions(FILTER_OPTIONS);
        sourceAsset = assetClient.sendSourceAsset(sourceAsset);
        pollableTaskClient.waitForPollableTask(sourceAsset.getPollableTask().getId());
        return sourceAsset;
    }

    String getLocalizedCourse(Long assetId, long localeId, String translationsByCourseId) {

        LocalizedAssetBody localizedAssetForContent = assetClient.getLocalizedAssetForContent(
                assetId,
                localeId,
                translationsByCourseId,
                null,
                null,
                FILTER_OPTIONS,
                LocalizedAssetBody.Status.ALL,
                LocalizedAssetBody.InheritanceMode.USE_PARENT);

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
            throw new CommandException("Evolve must be configured with: \"l10n.evolve.url\" and \"l10n.evolve.token\"");
        }
    }
}
