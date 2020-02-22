package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.okapi.extractor.AssetExtractorTextUnit;

import java.util.Comparator;

public class AssetExtractorTextUnitComparators {

    /**
     * Sort text units by filename, name, source and then comments
     */
    public static final Comparator<AssetExtractorTextUnit> BY_FILENAME_NAME_SOURCE_COMMENTS = Comparator
            .comparing(AssetExtractorTextUnit::getName, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AssetExtractorTextUnit::getSource, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AssetExtractorTextUnit::getComments, Comparator.nullsFirst(Comparator.naturalOrder()));
}
