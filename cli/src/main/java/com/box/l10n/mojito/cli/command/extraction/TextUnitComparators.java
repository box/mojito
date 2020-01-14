package com.box.l10n.mojito.cli.command.extraction;

import java.util.Comparator;

public class TextUnitComparators {

    /**
     * Sort text units by filename, name, source and then comments
     */
    public static final Comparator<TextUnitWithAssetPath> BY_FILENAME_NAME_SOURCE_COMMENTS = Comparator
            .comparing(TextUnitWithAssetPath::getAssetPath, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(TextUnitWithAssetPath::getName, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(TextUnitWithAssetPath::getSource, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(TextUnitWithAssetPath::getComments, Comparator.nullsFirst(Comparator.naturalOrder()));
}
