package com.box.l10n.mojito.cli.command.extraction;

import java.util.Set;

class Diff {
    Set<String> addedFiles;
    Set<String> removedFiles;
    Set<AssetExtractorTextUnitWithAssetPath> addedTextUnits;
    Set<AssetExtractorTextUnitWithAssetPath> removedTextUnits;

    public Set<String> getAddedFiles() {
        return addedFiles;
    }

    public void setAddedFiles(Set<String> addedFiles) {
        this.addedFiles = addedFiles;
    }

    public Set<String> getRemovedFiles() {
        return removedFiles;
    }

    public void setRemovedFiles(Set<String> removedFiles) {
        this.removedFiles = removedFiles;
    }

    public Set<AssetExtractorTextUnitWithAssetPath> getAddedTextUnits() {
        return addedTextUnits;
    }

    public void setAddedTextUnits(Set<AssetExtractorTextUnitWithAssetPath> addedTextUnits) {
        this.addedTextUnits = addedTextUnits;
    }

    public Set<AssetExtractorTextUnitWithAssetPath> getRemovedTextUnits() {
        return removedTextUnits;
    }

    public void setRemovedTextUnits(Set<AssetExtractorTextUnitWithAssetPath> removedTextUnits) {
        this.removedTextUnits = removedTextUnits;
    }
}
