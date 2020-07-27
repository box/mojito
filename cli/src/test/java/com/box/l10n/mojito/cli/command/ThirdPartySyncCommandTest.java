package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.utils.PollableTaskJobMatcher;
import com.box.l10n.mojito.cli.utils.TestingJobListener;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJob;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobInput;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.List;

import static com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction.MAP_TEXTUNIT;
import static com.box.l10n.mojito.rest.thirdparty.ThirdPartySyncAction.PUSH_SCREENSHOT;
import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartySyncCommandTest extends CLITestBase {

    @Autowired
    @Qualifier("fail_on_unknown_properties_false")
    ObjectMapper objectMapper;

    @Autowired
    Scheduler scheduler;

    Matcher<JobKey> jobMatcher;
    TestingJobListener testingJobListener;

    @Before
    public void setUp() throws SchedulerException {
        testingJobListener = new TestingJobListener(objectMapper);
        jobMatcher = new PollableTaskJobMatcher<>(ThirdPartySyncJob.class);
        scheduler.getListenerManager().addJobListener(testingJobListener, jobMatcher);
    }

    @After
    public void tearDown() throws SchedulerException {
        scheduler.getListenerManager().removeJobListener(testingJobListener.getName());
        scheduler.getListenerManager().removeJobListenerMatcher(testingJobListener.getName(), jobMatcher);
    }

    @Test
    public void execute() throws Exception {

        String repoName = testIdWatcher.getEntityName("thirdpartysync_execute");

        Repository repository = repositoryService.createRepository(repoName, repoName + " description", null, false);
        String projectId = testIdWatcher.getEntityName("projectId");

        String pluralSeparator = " _";
        String skipTextUnitPattern = "%skip_text_pattern";
        String skipAssetPattern = "%skip_asset_pattern%";
        List<String> options = Arrays.asList(
                "special-option=value@of%Option",
                "smartling-placeholder-custom=\\{\\{\\}\\}|\\{\\{?.+?\\}\\}?|\\%\\%\\(.+?\\)s|\\%\\(.+?\\)s|\\%\\(.+?\\)d|\\%\\%s|\\%s"
        );

        getL10nJCommander().run("thirdparty-sync",
                "-r", repository.getName(),
                "-p", projectId,
                "-a", MAP_TEXTUNIT.name(), PUSH_SCREENSHOT.name(),
                "-ps", pluralSeparator,
                "-st", skipTextUnitPattern,
                "-sa", skipAssetPattern,
                "-o", options.get(0), options.get(1));

        String output = outputCapture.toString();
        assertThat(output).contains("repository: " + repository.getName());
        assertThat(output).contains("project id: " + projectId);
        assertThat(output).contains("actions: " + Arrays.asList(ThirdPartySyncAction.MAP_TEXTUNIT, ThirdPartySyncAction.PUSH_SCREENSHOT).toString());
        assertThat(output).contains("skip-text-units-with-pattern: " + skipTextUnitPattern);
        assertThat(output).contains("skip-assets-path-pattern: " + skipAssetPattern);
        assertThat(output).contains("options: " + options.toString());

        waitForCondition("Ensure ThirdPartySyncJob gets executed",
                () -> testingJobListener.getExecuted().size() > 0);

        ThirdPartySyncJobInput jobInput = testingJobListener.getFirstInputMapAs(ThirdPartySyncJobInput.class);

        assertThat(jobInput).isNotNull();
        assertThat(jobInput.getRepositoryId()).isEqualTo(repository.getId());
        assertThat(jobInput.getThirdPartyProjectId()).isEqualTo(projectId);
        assertThat(jobInput.getActions()).containsExactlyInAnyOrder(MAP_TEXTUNIT, PUSH_SCREENSHOT);
        assertThat(jobInput.getPluralSeparator()).isEqualTo(pluralSeparator);
        assertThat(jobInput.getSkipTextUnitsWithPattern()).isEqualTo(skipTextUnitPattern);
        assertThat(jobInput.getSkipAssetsWithPathPattern()).isEqualTo(skipAssetPattern);
        assertThat(jobInput.getOptions()).containsExactlyInAnyOrderElementsOf(options);
    }

}
