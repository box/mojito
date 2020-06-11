package com.box.l10n.mojito.monitoring;


import io.micrometer.core.instrument.Clock;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(value = "l10n.management.metrics.export.statsd.prefix-hierarchical-name-mapper.enabled", havingValue = "true")
@Configuration
public class PrefixHierarchicalNameMapperStatsdConfig {

    static Logger logger = LoggerFactory.getLogger(PrefixHierarchicalNameMapperStatsdConfig.class);

    @Bean
    public StatsdMeterRegistry statsdMeterRegistry(StatsdConfig config,
                                                   Clock clock,
                                                   PrefixHierarchicalNameMapperStatsdConfigurationProperties phnmscp) {

        logger.info("Configure statsdMeterRegistry with a PrefixHierarchicalNameMapper");
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper(
                phnmscp.getNamePrefix(),
                phnmscp.getTagKeyPrefix(),
                phnmscp.getNamingConventionOverride());
        return new StatsdMeterRegistry(config, prefixHierarchicalNameMapper, clock);
    }
}
