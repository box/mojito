package com.box.l10n.mojito;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.Environment;

/**
 * Global reactor configuration.
 *
 * @author jaurambault
 */
@Configuration
public class StreamConfig {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(StreamConfig.class);

    @Bean
    Environment streamEnvironment() {
        return Environment.initializeIfEmpty()
                .assignErrorJournal();
    }

}
