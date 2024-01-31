package com.box.l10n.mojito.service.repository.statistics;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    MeterRegistry meterRegistryMock = Mockito.mock(MeterRegistry.class);
    Counter counterMock = Mockito.mock(Counter.class);
    when(meterRegistryMock.counter(Mockito.anyString(), isA(Iterable.class)))
        .thenReturn(counterMock);
    repositoryStatisticsUpdatedReactor.meterRegistry = meterRegistryMock;

    repositoryStatisticsUpdatedReactor.bufferDuration = Duration.ofMillis(100);
    repositoryStatisticsUpdatedReactor.createProcessor();

    repositoryStatisticsUpdatedReactor.generateEvent(1L);
    repositoryStatisticsUpdatedReactor.generateEvent(1L);
    repositoryStatisticsUpdatedReactor.generateEvent(2L);
    repositoryStatisticsUpdatedReactor.generateEvent(2L);

    Mockito.verify(mockRepositoryStatisticsJobScheduler, Mockito.timeout(200).times(1))
        .schedule(1L);
    Mockito.verify(mockRepositoryStatisticsJobScheduler, Mockito.times(1)).schedule(2L);
    Mockito.verify(meterRegistryMock, Mockito.times(2))
        .counter(Mockito.anyString(), isA(Iterable.class));
    Mockito.verify(counterMock, Mockito.times(2)).increment();
  }
}
