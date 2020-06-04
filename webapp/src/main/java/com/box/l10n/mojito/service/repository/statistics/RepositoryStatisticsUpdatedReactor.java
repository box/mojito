package com.box.l10n.mojito.service.repository.statistics;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.ReplayProcessor;

import javax.annotation.PostConstruct;
import java.time.Duration;

/**
 * This class aggregates events that requires repository statistics re-computation
 * and saves at most one record for a repository per second.
 *
 * @author jaurambault
 */
@Component
public class RepositoryStatisticsUpdatedReactor {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(RepositoryStatisticsUpdatedReactor.class);

    @Autowired
    RepositoryStatisticsJobScheduler repositoryStatisticsJobScheduler;

    ReplayProcessor<Long> replayProcessor;

    @PostConstruct
    private void createProcessor() {
        //TODO(spring2) this was rewritten -- add tests
        //TODO(spring2) also see StreamConfig
        replayProcessor = ReplayProcessor.create();
        replayProcessor.buffer(Duration.ofSeconds(1)).subscribe(repositoryIds -> {
            for (Long repositoryId : Sets.newHashSet(repositoryIds)) {
                repositoryStatisticsJobScheduler.schedule(repositoryId);
            }
        });
    }

    /**
     * Generates event that the repository statistics is outdated and needs re-computation.
     *
     * @param repositoryId
     */
    public void generateEvent(Long repositoryId) {
        replayProcessor.onNext(repositoryId);
    }
}
