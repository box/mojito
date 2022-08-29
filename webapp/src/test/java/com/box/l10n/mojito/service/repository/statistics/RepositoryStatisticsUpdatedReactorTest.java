package com.box.l10n.mojito.service.repository.statistics;

import static org.junit.Assert.*;

import java.time.Duration;
import org.junit.Test;
import org.mockito.Mockito;

public class RepositoryStatisticsUpdatedReactorTest {

  @Test
  public void generateEvent() throws InterruptedException {
    RepositoryStatisticsJobScheduler mockRepositoryStatisticsJobScheduler =
        Mockito.mock(RepositoryStatisticsJobScheduler.class);
    RepositoryStatisticsUpdatedReactor repositoryStatisticsUpdatedReactor =
        new RepositoryStatisticsUpdatedReactor(mockRepositoryStatisticsJobScheduler);

    repositoryStatisticsUpdatedReactor.createProcessor(Duration.ofMillis(100));

    repositoryStatisticsUpdatedReactor.generateEvent(1L);
    repositoryStatisticsUpdatedReactor.generateEvent(1L);
    repositoryStatisticsUpdatedReactor.generateEvent(2L);
    repositoryStatisticsUpdatedReactor.generateEvent(2L);

    Mockito.verify(mockRepositoryStatisticsJobScheduler, Mockito.timeout(200).times(1))
        .schedule(1L);
    Mockito.verify(mockRepositoryStatisticsJobScheduler, Mockito.times(1)).schedule(2L);
  }
}
