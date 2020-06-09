package com.box.l10n.mojito.metrics;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.config.NamingConvention;

/**
 * StatsD naming convention config
 *
 * This configuration will be applied to statsd metric names when the top-level statsd reporter is enabled
 */
@Component
@ConfigurationProperties("l10n.management.metrics.export.statsd")
@ConditionalOnProperty(value="management.metrics.export.statsd.enabled", havingValue="true")
public class StatsdNamingConventionConfig {

    private NamingConvention namingConvention = NamingConvention.identity;

    private String tagKeyPrefix = "";

    private String namePrefix = "";

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    public void setNamingConvention(NamingConvention namingConvention) {
        this.namingConvention = namingConvention;
    }

    public String getTagKeyPrefix() {
        return tagKeyPrefix;
    }

    public void setTagKeyPrefix(String tagKeyPrefix) {
        this.tagKeyPrefix = tagKeyPrefix;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public boolean changesNaming(){
        return namingConvention != NamingConvention.identity && namingConvention != NamingConvention.dot;
    }
}

