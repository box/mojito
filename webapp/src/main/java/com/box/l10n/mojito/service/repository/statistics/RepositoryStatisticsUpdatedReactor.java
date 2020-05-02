package com.box.l10n.mojito.service.repository.statistics;

import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

//import reactor.Environment;
//import reactor.core.processor.RingBufferProcessor;
//import reactor.fn.Consumer;
//import reactor.rx.Stream;
//import reactor.rx.Streams;

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

    private Processor<Long, Long> processor;

    //TODO(spring2)
//    @PostConstruct
//    private void createProcessor() {
//        processor = RingBufferProcessor.create();
//        Stream stream = Streams.wrap(processor);
//        stream.buffer(1, TimeUnit.SECONDS).consume(new Consumer<List<Long>>() {
//            @Override
//            public void accept(List<Long> repositoryIds) {
//                for (Long repositoryId : Sets.newHashSet(repositoryIds)) {
//                    repositoryStatisticsJobScheduler.schedule(repositoryId);
//                }
//            }
//        });
//    }

    /**
     * Generates event that the repository statistics is outdated and needs re-computation.
     *
     * @param repositoryId
     */
    public void generateEvent(Long repositoryId) {
        processor.onNext(repositoryId);
    }
}
