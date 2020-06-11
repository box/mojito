package com.box.l10n.mojito.monitoring;


import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.statsd.StatsdConfig;
import io.micrometer.statsd.StatsdMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Conditional(PrefixHierarchicalNameMapperStatsdConfig.OnStatsdAndOnPrefixHierarchicalNameMapper.class)
@Configuration
public class PrefixHierarchicalNameMapperStatsdConfig {

    static Logger logger = LoggerFactory.getLogger(PrefixHierarchicalNameMapperStatsdConfig.class);

    @Autowired
    PrefixHierarchicalNameMapperStatsdConfigurationProperties prefixHierarchicalNameMapperStatsdConfigurationProperties;

    @Bean
    public StatsdMeterRegistry statsdMeterRegistry(StatsdConfig config, Clock clock) {
        logger.info("Configure statsdMeterRegistry with a PrefixHierarchicalNameMapper");
        PrefixHierarchicalNameMapper prefixHierarchicalNameMapper = new PrefixHierarchicalNameMapper(
                prefixHierarchicalNameMapperStatsdConfigurationProperties.getNamePrefix(),
                prefixHierarchicalNameMapperStatsdConfigurationProperties.getTagKeyPrefix(),
                prefixHierarchicalNameMapperStatsdConfigurationProperties.getNamingConventionOverride());
        return new StatsdMeterRegistry(config, prefixHierarchicalNameMapper, clock);
    }

    /**
     * In addition to the feature flag, make sure that Stats is enabled.
     *
     * If not StatsdConfig won't be available and startup will fails. While it doesn't make sense to enable the mapper
     * if Statsd is not enabled, it makes the configuration more flexible
     */
    static class OnStatsdAndOnPrefixHierarchicalNameMapper extends AllNestedConditions {

        public OnStatsdAndOnPrefixHierarchicalNameMapper() {
            super(ConfigurationPhase.PARSE_CONFIGURATION);
        }

        @ConditionalOnProperty(value = "management.metrics.export.statsd.enabled", havingValue = "true")
        public class OnStatsd {
        }

        @ConditionalOnProperty(value = "l10n.management.metrics.export.statsd.prefix-hierarchical-name-mapper.enabled", havingValue = "true")
        public class OnPrefixHierarchicalNameMapper {
        }
    }
}
