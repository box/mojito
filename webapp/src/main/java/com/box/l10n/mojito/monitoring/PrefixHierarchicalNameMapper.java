package com.box.l10n.mojito.monitoring;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import java.util.stream.Collectors;

public class PrefixHierarchicalNameMapper implements HierarchicalNameMapper {

    String namePrefix;
    String tagKeyPrefix;
    NamingConvention nameConventionOverride;

    public PrefixHierarchicalNameMapper(String namePrefix, String tagKeyPrefix, NamingConvention nameConventionOverride) {
        Preconditions.checkNotNull(namePrefix, tagKeyPrefix, nameConventionOverride);
        this.namePrefix = namePrefix;
        this.tagKeyPrefix = tagKeyPrefix;
        this.nameConventionOverride = nameConventionOverride;
    }

    /**
     * Pretty much a copy of {@link HierarchicalNameMapper.DEFAULT} with prefixes added.
     */
    @Override
    public String toHierarchicalName(Meter.Id id, NamingConvention namingConvention) {

        if (nameConventionOverride != null) {
            namingConvention = nameConventionOverride;
        }

        String prefix = Strings.isNullOrEmpty(namePrefix) ? "" : (namingConvention.name(namePrefix, Meter.Type.OTHER) + ".");

        String hierarchicalName =
                        prefix +
                        id.getConventionName(namingConvention) + id.getConventionTags(namingConvention).stream()
                        .map(t -> "." + tagKeyPrefix + t.getKey() + "." + t.getValue())
                        .map(nameSegment -> nameSegment.replace(" ", "_"))
                        .collect(Collectors.joining(""));

        return hierarchicalName;
    }
}
