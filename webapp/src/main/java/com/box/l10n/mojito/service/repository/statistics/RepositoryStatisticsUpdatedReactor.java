package com.box.l10n.mojito.service.repository.statistics;

import com.google.common.collect.Sets;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.time.Duration;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.ReplayProcessor;

/**
 * This class aggregates events that requires repository statistics re-computation and saves at most
 * one record for a repository per second.
 *
 * @author jaurambault
 */
@Component
public class RepositoryStatisticsUpdatedReactor {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsUpdatedReactor.class);

  RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler;

  ReplayProcessor<Long> replayProcessor;

  @Autowired MeterRegistry meterRegistry;

  @Value("${l10n.repositoryStatisticsUpdatedReactor.bufferDuration:PT1S}")
  Duration bufferDuration;

  @Autowired
  public RepositoryStatisticsUpdatedReactor(
      RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler) {
    this.repositoryStatisticsJobScheduler = repositoryStatisticsJobScheduler;
  }

  @PostConstruct
  public void init() {
    createProcessor();
  }

  void createProcessor() {
    replayProcessor = ReplayProcessor.create();
    replayProcessor
        .buffer(bufferDuration)
        .subscribe(
            repositoryIds -> {
              for (Long repositoryId : Sets.newHashSet(repositoryIds)) {
                meterRegistry
                    .counter(
                        "repositoryStatisticsUpdatedReactor.scheduleRepoStatsJob",
                        Tags.of("repositoryId", String.valueOf(repositoryId)))
                    .increment();
                repositoryStatisticsJobScheduler.schedule(repositoryId);
              }
            });
  }

  /**
   * Generates event that the repository statistics is outdated and needs re-computation.
   *
   * @param repositoryId
   */
  public synchronized void generateEvent(Long repositoryId) {
    replayProcessor.onNext(repositoryId);
  }
}
