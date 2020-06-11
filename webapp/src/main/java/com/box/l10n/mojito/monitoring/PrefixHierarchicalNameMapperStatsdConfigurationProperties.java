package com.box.l10n.mojito.monitoring;

import io.micrometer.core.instrument.config.NamingConvention;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("l10n.management.metrics.export.statsd.prefix-hierarchical-name-mapper")
public class PrefixHierarchicalNameMapperStatsdConfigurationProperties {

    boolean enabled = false;
    String namePrefix = "";
    String tagKeyPrefix = "";
    NamingConvention namingConventionOverride = null;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getNamePrefix() {
        return namePrefix;
    }

    public void setNamePrefix(String namePrefix) {
        this.namePrefix = namePrefix;
    }

    public String getTagKeyPrefix() {
        return tagKeyPrefix;
    }

    public void setTagKeyPrefix(String tagKeyPrefix) {
        this.tagKeyPrefix = tagKeyPrefix;
    }

    public NamingConvention getNamingConventionOverride() {
        return namingConventionOverride;
    }

    public void setNamingConventionOverride(NamingConvention namingConventionOverride) {
        this.namingConventionOverride = namingConventionOverride;
    }
}
