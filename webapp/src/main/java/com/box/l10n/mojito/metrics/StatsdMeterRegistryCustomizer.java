package com.box.l10n.mojito.metrics;

import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import io.micrometer.statsd.StatsdMeterRegistry;

@Component
public class StatsdMeterRegistryCustomizer {

    @Bean
    @ConditionalOnProperty(value="l10n.management.metrics.export.statsd.transform-name", havingValue="true")
    public MeterRegistryCustomizer<StatsdMeterRegistry> customizer(StatsdNamingConventionConfig config) {

        return registry -> {
            if (config.changesNaming()){
                registry.config().namingConvention(NamingConventionBuilder.build(config));
            }
        };
    }
}
