package com.box.l10n.mojito.service.thirdparty.smartling.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.LocaleMappingHelper;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.repository.RepositoryService;
import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class SmartlingPullBatchFileJobTest {

  @Mock QuartzPollableTaskScheduler quartzPollableTaskSchedulerMock;

  @Mock RepositoryRepository repositoryRepositoryMock;

  @Mock Repository repositoryMock;

  @Mock RepositoryService repositoryServiceMock;

  @Captor ArgumentCaptor<QuartzJobInfo<SmartlingPullLocaleFileJobInput, Void>> quartzJobInfoCaptor;

  @Spy SmartlingPullBatchFileJob smartlingPullBatchFileJob = new SmartlingPullBatchFileJob();

  RepositoryLocale enRepoLocale;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    doReturn(1L).when(smartlingPullBatchFileJob).getParentId();
    smartlingPullBatchFileJob.quartzPollableTaskScheduler = quartzPollableTaskSchedulerMock;
    smartlingPullBatchFileJob.repositoryRepository = repositoryRepositoryMock;
    smartlingPullBatchFileJob.localeMappingHelper = new LocaleMappingHelper();
    smartlingPullBatchFileJob.repositoryService = repositoryServiceMock;
    when(repositoryRepositoryMock.findByName("testRepo")).thenReturn(repositoryMock);
    when(repositoryMock.getName()).thenReturn("testRepo");
    Locale english = new Locale();
    english.setBcp47Tag("en");
    enRepoLocale = new RepositoryLocale();
    enRepoLocale.setLocale(english);
    enRepoLocale.setId(1L);
    Locale french = new Locale();
    french.setBcp47Tag("fr");
    RepositoryLocale frRepoLocale = new RepositoryLocale();
    frRepoLocale.setLocale(french);
    frRepoLocale.setId(2L);
    Locale gaelic = new Locale();
    gaelic.setBcp47Tag("ga");
    RepositoryLocale gaRepoLocale = new RepositoryLocale();
    gaRepoLocale.setLocale(gaelic);
    gaRepoLocale.setId(3L);
    frRepoLocale.setParentLocale(enRepoLocale);
    gaRepoLocale.setParentLocale(enRepoLocale);
    when(repositoryServiceMock.getRepositoryLocalesWithoutRootLocale(isA(Repository.class)))
        .thenReturn(Collections.unmodifiableSet(Sets.newHashSet(frRepoLocale, gaRepoLocale)));
    when(repositoryMock.getRepositoryLocales())
        .thenReturn(
            Collections.unmodifiableSet(Sets.newHashSet(enRepoLocale, frRepoLocale, gaRepoLocale)));
  }

  @Test
  public void testPullLocaleFileJobsAreScheduled() throws Exception {
    SmartlingPullBatchFileJobInput jobInput = new SmartlingPullBatchFileJobInput();
    jobInput.setBatchNumber(1L);
    jobInput.setDeltaPull(false);
    jobInput.setFilePrefix("singular");
    jobInput.setDryRun(false);
    jobInput.setRepositoryName("testRepo");
    jobInput.setProjectId("testProjectId");
    jobInput.setSchedulerName("customScheduler");

    smartlingPullBatchFileJob.call(jobInput);

    // Total repo locales is 3 but translations should not be pulled for source locale, so we expect
    // 2 calls here
    verify(quartzPollableTaskSchedulerMock, times(2)).scheduleJob(quartzJobInfoCaptor.capture());

    List<QuartzJobInfo<SmartlingPullLocaleFileJobInput, Void>> quartzJobInfos =
        quartzJobInfoCaptor.getAllValues();
    assertThat(quartzJobInfos.stream().filter(q -> q.getParentId() == 1L).count()).isEqualTo(2);

    assertThat(quartzJobInfos.stream().map(QuartzJobInfo::getInput))
        .extracting(
            "repositoryName",
            "localeId",
            "smartlingFilePrefix",
            "fileName",
            "pluralSeparator",
            "smartlingLocale",
            "deltaPull",
            "pluralFixForLocale",
            "dryRun",
            "smartlingProjectId",
            "localeBcp47Tag",
            "smartlingLocale")
        .containsExactlyInAnyOrder(
            tuple(
                "testRepo",
                2L,
                "singular",
                "testRepo/00001_singular_source.xml",
                null,
                "fr",
                false,
                false,
                false,
                "testProjectId",
                "fr",
                "fr"),
            tuple(
                "testRepo",
                3L,
                "singular",
                "testRepo/00001_singular_source.xml",
                null,
                "ga",
                false,
                false,
                false,
                "testProjectId",
                "ga",
                "ga"));
  }

  @Test
  public void testNoJobsScheduledIfOnlyParentLocaleExists() throws Exception {
    when(repositoryMock.getRepositoryLocales())
        .thenReturn(Collections.unmodifiableSet(Sets.newHashSet(enRepoLocale)));
    when(repositoryServiceMock.getRepositoryLocalesWithoutRootLocale(isA(Repository.class)))
        .thenReturn(Collections.emptySet());

    SmartlingPullBatchFileJobInput jobInput = new SmartlingPullBatchFileJobInput();
    jobInput.setBatchNumber(1L);
    jobInput.setDeltaPull(false);
    jobInput.setFilePrefix("singular");
    jobInput.setDryRun(false);
    jobInput.setRepositoryName("testRepo");
    jobInput.setProjectId("testProjectId");

    smartlingPullBatchFileJob.call(jobInput);

    verify(quartzPollableTaskSchedulerMock, times(0)).scheduleJob(quartzJobInfoCaptor.capture());
  }

  @Test
  public void testLocaleMapping() throws Exception {
    SmartlingPullBatchFileJobInput jobInput = new SmartlingPullBatchFileJobInput();
    jobInput.setBatchNumber(1L);
    jobInput.setDeltaPull(false);
    jobInput.setFilePrefix("singular");
    jobInput.setDryRun(false);
    jobInput.setRepositoryName("testRepo");
    jobInput.setProjectId("testProjectId");
    jobInput.setLocaleMapping("fr:fr-CA");

    smartlingPullBatchFileJob.call(jobInput);

    verify(quartzPollableTaskSchedulerMock, times(2)).scheduleJob(quartzJobInfoCaptor.capture());

    List<QuartzJobInfo<SmartlingPullLocaleFileJobInput, Void>> quartzJobInfos =
        quartzJobInfoCaptor.getAllValues();
    assertThat(quartzJobInfos.stream().filter(q -> q.getParentId() == 1L).count()).isEqualTo(2);

    assertThat(quartzJobInfos.stream().map(QuartzJobInfo::getInput))
        .extracting("smartlingLocale")
        .containsExactlyInAnyOrder("ga", "fr-CA");
  }
}
