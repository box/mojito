package com.box.l10n.mojito.monitoring;


import com.google.common.base.Strings;

import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.config.NamingConvention;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

import static com.google.common.base.Preconditions.checkNotNull;

public class PrefixHierarchicalNameMapper implements HierarchicalNameMapper {

    String namePrefix;
    String tagKeyPrefix;
    String stripCharacters;
    NamingConvention nameConventionOverride;

    public PrefixHierarchicalNameMapper(String namePrefix,
                                        String tagKeyPrefix,
                                        String stripCharacters,
                                        @Nullable NamingConvention nameConventionOverride) {
        this.namePrefix = checkNotNull(namePrefix);
        this.tagKeyPrefix = checkNotNull(tagKeyPrefix);
        this.stripCharacters = stripCharacters;
        this.nameConventionOverride = nameConventionOverride;
    }

    /**
     * Pretty much a copy of {@link HierarchicalNameMapper.DEFAULT} with prefixes added.
     */
    @Override
    public String toHierarchicalName(Meter.Id id, NamingConvention namingConvention) {

        namingConvention = Optional.ofNullable(nameConventionOverride).orElse(namingConvention);

        String prefix = Strings.isNullOrEmpty(namePrefix) ? "" : (namingConvention.name(namePrefix, Meter.Type.OTHER) + ".");

        return prefix +
                id.getConventionName(namingConvention) +
                id.getConventionTags(namingConvention)
                  .stream()
                  .map(this::formatTag)
                  .map(nameSegment -> nameSegment.replace(" ", "_"))
                  .collect(Collectors.joining(""));
    }

    String formatTag(Tag tag) {
        return "." + tagKeyPrefix + stripCharacters(tag.getKey()) + "." + stripCharacters(tag.getValue());
    }

    /**
     * Removes a list of characters from a String.
     * Stripping does not depends on the ordering or format, it just removes
     * all of the characters listed in {@link stripCharacters} from the input String
     *
     * @param input the input String
     * @return the string with characters stripped from it
     */
    String stripCharacters(String input){
        return Optional.ofNullable(Strings.emptyToNull(stripCharacters))
                       .map(pattern -> input.replaceAll("[" + pattern + "]", ""))
                       .orElse(input);
    }
}
