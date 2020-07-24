package com.box.l10n.mojito.cli.command;

import com.box.l10n.mojito.cli.CLITestBase;
import com.box.l10n.mojito.cli.utils.PollableTaskJobMatcher;
import com.box.l10n.mojito.cli.utils.TestingJobListener;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.json.ObjectMapper;
import com.box.l10n.mojito.rest.ThirdPartySyncAction;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJob;
import com.box.l10n.mojito.service.thirdparty.ThirdPartySyncJobInput;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobKey;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.Arrays;
import java.util.List;

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
    public void setUp() throws Exception {
        testingJobListener = new TestingJobListener(objectMapper);
        jobMatcher = new PollableTaskJobMatcher<>(ThirdPartySyncJob.class);
        scheduler.getListenerManager().addJobListener(testingJobListener, jobMatcher);
    }

    @Test
    public void execute() throws Exception {

        String repoName = testIdWatcher.getEntityName("thirdpartysync_execute");

        Repository repository = repositoryService.createRepository(repoName, repoName + " description", null, false);
        String projectId = testIdWatcher.getEntityName("projectId");

        // TODO: For a plural separator like " _" this test will fail. The current version we have for
        //  JCommander trims the argument values, even when quoted.
        // https://github.com/cbeust/jcommander/issues/417
        // https://github.com/cbeust/jcommander/commit/4aec38b4a0ea63a8dc6f41636fa81c2ebafddc18
        String pluralSeparator = "_";
        String skipTextUnitPattern = "%skip_text_pattern";
        String skipAssetPattern = "%skip_asset_pattern%";
        List<String> options = Arrays.asList(
                "special-option=value@of%Option",
                "smartling-placeholder-custom=\\{\\{\\}\\}|\\{\\{?.+?\\}\\}?|\\%\\%\\(.+?\\)s|\\%\\(.+?\\)s|\\%\\(.+?\\)d|\\%\\%s|\\%s"
        );

        getL10nJCommander().run("thirdparty-sync",
                "-r", repository.getName(),
                "-p", projectId,
                "-a", "MAP_TEXTUNIT", "PUSH_SCREENSHOT",
                "-ps", pluralSeparator,
                "-st", skipTextUnitPattern,
                "-sa", skipAssetPattern,
                "-o", options.get(0), options.get(1));

        waitForCondition("Ensure ThirdPartySyncJob gets executed",
                () -> testingJobListener.getExecuted().size() > 0);

        ThirdPartySyncJobInput jobInput = testingJobListener.getFirstInputMapAs(ThirdPartySyncJobInput.class);

        assertThat(jobInput).isNotNull();
        assertThat(jobInput.getRepositoryId()).isEqualTo(repository.getId());
        assertThat(jobInput.getThirdPartyProjectId()).isEqualTo(projectId);
        assertThat(jobInput.getActions()).containsExactlyInAnyOrder(ThirdPartySyncAction.MAP_TEXTUNIT, ThirdPartySyncAction.PUSH_SCREENSHOT);
        assertThat(jobInput.getPluralSeparator()).isEqualTo(pluralSeparator);
        assertThat(jobInput.getSkipTextUnitsWithPattern()).isEqualTo(skipTextUnitPattern);
        assertThat(jobInput.getSkipAssetsWithPathPattern()).isEqualTo(skipAssetPattern);
        assertThat(jobInput.getOptions()).containsExactlyInAnyOrderElementsOf(options);
    }

}
