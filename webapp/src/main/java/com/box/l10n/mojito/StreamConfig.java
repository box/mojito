package com.box.l10n.mojito;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import reactor.Environment;
import reactor.core.processor.RingBufferProcessor;
import reactor.fn.Consumer;
import reactor.rx.Stream;
import reactor.rx.Streams;

/**
 * Global reactor configuration.
 *
 * @author jaurambault
 */
@Configuration
@Component
public class StreamConfig {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(StreamConfig.class);

    @Bean
    Environment env() {
        return Environment.initializeIfEmpty()
                .assignErrorJournal();
    }

    // TODO Move that to a RepositoryStatisticReactor class?  
    /**
     * This basically can be used to write at most one record for a repository
     * per second it doesn't matter how many events gets in, it will scale
     */
    @Bean(name = "repositoryStatisticProcessor")
    Processor<Integer, Integer> getRepositoryStatisticProcessor() {
        Processor<Integer, Integer> processor = RingBufferProcessor.create();

        Stream stream = Streams.wrap(processor);

        // java 8 wanted :) !!
        // s.window(1, TimeUnit.SECONDS).consume(window -> window.distinct().consume(this::saveToDB));
        
        stream.window(1, TimeUnit.SECONDS)
                .consume(new Consumer<Stream>() {
                   
                    @Override
                    public void accept(Stream s) {

                        s.distinct().consume(new Consumer<Integer>() {

                            @Override
                            public void accept(Integer i) {
                                logger.error("Update database for: " + i);
                            }
                        });
                    }
                });

        return processor;
    }

    @Scheduled(fixedDelay = 1000)
    public void simulateEventsFromAOPorHibernateOrSomethingElse() {
        for (int i = 0; i < 1000; i++) {
            // non blocking submission and with the RingBufferProcessor it becomes async
            getRepositoryStatisticProcessor().onNext(i % 5);
        }
    }

}
