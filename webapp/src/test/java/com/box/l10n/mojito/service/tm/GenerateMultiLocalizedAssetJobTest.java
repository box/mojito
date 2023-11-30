package com.box.l10n.mojito.service.tm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.rest.asset.LocaleInfo;
import com.box.l10n.mojito.rest.asset.LocalizedAssetBody;
import com.box.l10n.mojito.rest.asset.MultiLocalizedAssetBody;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class GenerateMultiLocalizedAssetJobTest {

  @Mock QuartzPollableTaskScheduler quartzPollableTaskSchedulerMock;

  @Mock PollableFuture<LocalizedAssetBody> pollableFutureMock;

  @Mock PollableTask pollableTaskMock;

  @Mock AssetRepository assetRepositoryMock;

  @Mock RepositoryLocale repoLocaleMock;

  @Mock Locale localeMock;

  @Mock Asset assetMock;

  MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @Mock RepositoryLocaleRepository repositoryLocaleRepositoryMock;

  @Mock Repository repositoryMock;

  @Captor ArgumentCaptor<QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody>> quartzJobInfoCaptor;

  MultiLocalizedAssetBody multiLocalizedAssetBody;

  @Spy
  GenerateMultiLocalizedAssetJob generateMultiLocalizedAssetJob =
      new GenerateMultiLocalizedAssetJob();

  @Before
  public void setUp() {
    generateMultiLocalizedAssetJob.assetRepository = assetRepositoryMock;
    generateMultiLocalizedAssetJob.repositoryLocaleRepository = repositoryLocaleRepositoryMock;
    doReturn(1L).when(generateMultiLocalizedAssetJob).getParentId();
    when(repositoryLocaleRepositoryMock.findByRepositoryIdAndLocaleId(
            isA(Long.class), isA(Long.class)))
        .thenReturn(repoLocaleMock);
    when(repoLocaleMock.getLocale()).thenReturn(localeMock);
    when(localeMock.getBcp47Tag()).thenReturn("fr-FR").thenReturn("ga-IE");
    when(repositoryMock.getId()).thenReturn(1L);
    when(assetMock.getRepository()).thenReturn(repositoryMock);
    when(repositoryMock.getName()).thenReturn("testRepo");
    when(assetRepositoryMock.findById(isA(Long.class)))
        .thenReturn(java.util.Optional.of(assetMock));
    when(pollableFutureMock.getPollableTask()).thenReturn(pollableTaskMock);
    when(pollableTaskMock.getId()).thenReturn(1L).thenReturn(2L);
    when(quartzPollableTaskSchedulerMock.scheduleJob(isA(QuartzJobInfo.class)))
        .thenReturn(pollableFutureMock);
    generateMultiLocalizedAssetJob.quartzPollableTaskScheduler = quartzPollableTaskSchedulerMock;
    generateMultiLocalizedAssetJob.meterRegistry = meterRegistry;
    multiLocalizedAssetBody = new MultiLocalizedAssetBody();
    List<LocaleInfo> localeInfos = new ArrayList<>();
    LocaleInfo localeInfo = new LocaleInfo();
    localeInfo.setLocaleId(1L);
    LocaleInfo localeInfo2 = new LocaleInfo();
    localeInfo2.setLocaleId(2L);
    localeInfos.add(localeInfo);
    localeInfos.add(localeInfo2);
    multiLocalizedAssetBody.setLocaleInfos(localeInfos);
    multiLocalizedAssetBody.setSourceContent("sourceContent");
    multiLocalizedAssetBody.setAssetId(1L);
    multiLocalizedAssetBody.setSchedulerName("schedulerName");
  }

  @Test
  public void testMultipleLocalisedAssetJobsScheduled() throws Exception {
    MultiLocalizedAssetBody output = generateMultiLocalizedAssetJob.call(multiLocalizedAssetBody);
    verify(quartzPollableTaskSchedulerMock, times(2)).scheduleJob(quartzJobInfoCaptor.capture());
    List<QuartzJobInfo<LocalizedAssetBody, LocalizedAssetBody>> allValues =
        quartzJobInfoCaptor.getAllValues();
    assertThat(allValues.stream().filter(q -> q.getParentId() == 1L).count()).isEqualTo(2);
    assertThat(allValues.stream().map(QuartzJobInfo::getInput))
        .extracting("localeId")
        .containsExactlyInAnyOrder(1L, 2L);
    assertThat(output.getGenerateLocalizedAssetJobIds().size()).isEqualTo(2);
    assertThat(output.getGenerateLocalizedAssetJobIds().get("fr-FR")).isEqualTo(1L);
    assertThat(output.getGenerateLocalizedAssetJobIds().get("ga-IE")).isEqualTo(2L);
  }
}
