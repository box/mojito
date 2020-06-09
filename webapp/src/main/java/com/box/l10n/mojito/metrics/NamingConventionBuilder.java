package com.box.l10n.mojito.metrics;

import com.google.common.base.Strings;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.lang.Nullable;

public class NamingConventionBuilder {

    public static NamingConvention build(StatsdNamingConventionConfig config) {

        NamingConvention convention = config.getNamingConvention();
        String namePrefix;

        if (Strings.isNullOrEmpty(config.getNamePrefix()) || config.getNamePrefix().endsWith(".")) {
            namePrefix = config.getNamePrefix();
        } else {
            namePrefix = config.getNamePrefix() + ".";
        }

        return new NamingConvention() {

            @Override
            public String name(String name, Meter.Type type, @Nullable String baseUnit) {
                return namePrefix + convention.name(name, type, baseUnit);
            }

            @Override
            public String tagKey(String key) {
                return config.getTagKeyPrefix() + convention.tagKey(key);
            }
        };
    }
}
