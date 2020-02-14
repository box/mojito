package com.box.l10n.mojito.cli.command.extraction;

import java.util.Comparator;

public class TextUnitComparators {

    /**
     * Sort text units by filename, name, source and then comments
     */
    public static final Comparator<AssetExtractorTextUnitWithAssetPath> BY_FILENAME_NAME_SOURCE_COMMENTS = Comparator
            .comparing(AssetExtractorTextUnitWithAssetPath::getAssetPath, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AssetExtractorTextUnitWithAssetPath::getName, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AssetExtractorTextUnitWithAssetPath::getSource, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(AssetExtractorTextUnitWithAssetPath::getComments, Comparator.nullsFirst(Comparator.naturalOrder()));
}
