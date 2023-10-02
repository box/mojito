package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class SmartlingPullTranslationsJobTest {

  @Mock QuartzPollableTaskScheduler quartzPollableTaskSchedulerMock;

  @Mock RepositoryRepository repositoryRepositoryMock;

  @Captor ArgumentCaptor<QuartzJobInfo<SmartlingPullBatchFileJobInput, Void>> quartzJobInfoCaptor;

  @Spy
  SmartlingPullTranslationsJob smartlingPullTranslationsJob = new SmartlingPullTranslationsJob();

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doReturn(1L).when(smartlingPullTranslationsJob).getParentId();
    smartlingPullTranslationsJob.quartzPollableTaskScheduler = quartzPollableTaskSchedulerMock;
    smartlingPullTranslationsJob.repositoryRepository = repositoryRepositoryMock;
  }

  @Test
  public void testBatchFileJobsAreScheduled() throws Exception {
    SmartlingPullTranslationsJobInput jobInput = new SmartlingPullTranslationsJobInput();
    jobInput.setSingularCount(1);
    jobInput.setPluralCount(1);
    jobInput.setRepositoryName("testRepo");
    jobInput.setProjectId("testProjectId");
    jobInput.setSchedulerName("customScheduler");
    jobInput.setLocaleMapping("fr:fr-CA");
    jobInput.setBatchSize(10);
    jobInput.setPluralFixForLocale("ja-JP");
    jobInput.setDeltaPull(false);

    smartlingPullTranslationsJob.call(jobInput);

    verify(quartzPollableTaskSchedulerMock, times(2)).scheduleJob(quartzJobInfoCaptor.capture());
    List<QuartzJobInfo<SmartlingPullBatchFileJobInput, Void>> quartzJobInfos =
        quartzJobInfoCaptor.getAllValues();
    assertThat(quartzJobInfos.stream().filter(q -> q.getParentId() != 1L).count()).isEqualTo(0);
    assertThat(quartzJobInfos.stream().map(QuartzJobInfo::getInput))
        .extracting(
            "batchNumber",
            "projectId",
            "isDeltaPull",
            "filePrefix",
            "repositoryName",
            "pluralFixForLocale",
            "localeMapping")
        .containsExactlyInAnyOrder(
            tuple(0L, "testProjectId", false, "singular", "testRepo", "ja-JP", "fr:fr-CA"),
            tuple(0L, "testProjectId", false, "plural", "testRepo", "ja-JP", "fr:fr-CA"));
  }

  @Test
  public void testMultipleBatchFileJobsAreScheduled() throws Exception {
    SmartlingPullTranslationsJobInput jobInput = new SmartlingPullTranslationsJobInput();
    jobInput.setSingularCount(100);
    jobInput.setPluralCount(100);
    jobInput.setRepositoryName("testRepo");
    jobInput.setProjectId("testProjectId");
    jobInput.setSchedulerName("customScheduler");
    jobInput.setBatchSize(20);
    jobInput.setPluralFixForLocale("ja-JP");
    jobInput.setLocaleMapping("fr:fr-CA");
    jobInput.setDeltaPull(false);
    jobInput.setSchedulerName("customScheduler");

    smartlingPullTranslationsJob.call(jobInput);

    verify(quartzPollableTaskSchedulerMock, times(10)).scheduleJob(quartzJobInfoCaptor.capture());
    List<QuartzJobInfo<SmartlingPullBatchFileJobInput, Void>> quartzJobInfos =
        quartzJobInfoCaptor.getAllValues();
    assertThat(quartzJobInfos.stream().filter(q -> q.getParentId() != 1L).count()).isEqualTo(0);
    assertThat(quartzJobInfos.stream().map(QuartzJobInfo::getInput))
        .extracting(
            "batchNumber",
            "projectId",
            "isDeltaPull",
            "filePrefix",
            "repositoryName",
            "pluralFixForLocale",
            "localeMapping",
            "schedulerName")
        .containsExactlyInAnyOrder(
            tuple(
                0L,
                "testProjectId",
                false,
                "singular",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                1L,
                "testProjectId",
                false,
                "singular",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                2L,
                "testProjectId",
                false,
                "singular",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                3L,
                "testProjectId",
                false,
                "singular",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                4L,
                "testProjectId",
                false,
                "singular",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                0L,
                "testProjectId",
                false,
                "plural",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                1L,
                "testProjectId",
                false,
                "plural",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                2L,
                "testProjectId",
                false,
                "plural",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                3L,
                "testProjectId",
                false,
                "plural",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"),
            tuple(
                4L,
                "testProjectId",
                false,
                "plural",
                "testRepo",
                "ja-JP",
                "fr:fr-CA",
                "customScheduler"));
  }

  @Test
  public void testNoBatchJobsIfNoTextUnitsExist() throws Exception {
    SmartlingPullTranslationsJobInput jobInput = new SmartlingPullTranslationsJobInput();
    jobInput.setSingularCount(0);
    jobInput.setPluralCount(0);
    jobInput.setRepositoryName("testRepo");
    jobInput.setProjectId("testProjectId");
    jobInput.setSchedulerName("customScheduler");
    jobInput.setBatchSize(20);
    jobInput.setPluralFixForLocale("ja-JP");
    jobInput.setLocaleMapping("fr:fr-CA");
    jobInput.setDeltaPull(false);

    smartlingPullTranslationsJob.call(jobInput);

    verify(quartzPollableTaskSchedulerMock, times(0)).scheduleJob(quartzJobInfoCaptor.capture());
  }
}
