package com.box.l10n.mojito.service.thirdparty.smartling;

import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SmartlingOptions {

    public static final String PLURAL_FIX = "smartling-plural-fix";
    public static final String PLACEHOLDER_FORMAT = "smartling-placeholder-format";
    public static final String PLACEHOLDER_FORMAT_CUSTOM = "smartling-placeholder-format-custom";
    public static final String DRY_RUN = "dry-run";

    private final Set<String> pluralFixForLocales;
    private final String placeholderFormat;
    private final String customPlaceholderFormat;
    private final boolean dryRun;

    public SmartlingOptions(Set<String> pluralFixForLocales,
                            String placeholderFormat,
                            String customPlaceholderFormat,
                            boolean dryRun) {
        this.pluralFixForLocales = pluralFixForLocales;
        this.placeholderFormat = placeholderFormat;
        this.customPlaceholderFormat = customPlaceholderFormat;
        this.dryRun = dryRun;
    }

    public static SmartlingOptions parseList(List<String> options) {

        Map<String, String> map = options.stream()
                .map(str -> str.split("=", 2))
                .filter(arr -> arr.length == 2)
                .collect(Collectors.toMap(a -> a[0], a -> a[1]));

        String pluralFixStr = map.getOrDefault(PLURAL_FIX, "");
        String dryRunStr = map.getOrDefault(DRY_RUN, "");
        String placeholderFormat = map.getOrDefault(PLACEHOLDER_FORMAT, null);
        String customPlaceholderFormat = map.getOrDefault(PLACEHOLDER_FORMAT_CUSTOM, null);

        return new SmartlingOptions(
                pluralFixStr.isEmpty() ? Collections.emptySet() : ImmutableSet.copyOf(pluralFixStr.split(",")),
                placeholderFormat,
                customPlaceholderFormat,
                "true".equalsIgnoreCase(dryRunStr));
    }

    public Set<String> getPluralFixForLocales() {
        return pluralFixForLocales;
    }

    public String getPlaceholderFormat() {
        return placeholderFormat;
    }

    public String getCustomPlaceholderFormat() {
        return customPlaceholderFormat;
    }

    public boolean isDryRun() {
        return dryRun;
    }

}
