package com.box.l10n.mojito.cli.command.extraction;

import java.util.Set;

class Diff {
    Set<String> addedFiles;
    Set<String> removedFiles;
    Set<TextUnitWithAssetPath> addedTextUnits;
    Set<TextUnitWithAssetPath> removedTextUnits;

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

    public Set<TextUnitWithAssetPath> getAddedTextUnits() {
        return addedTextUnits;
    }

    public void setAddedTextUnits(Set<TextUnitWithAssetPath> addedTextUnits) {
        this.addedTextUnits = addedTextUnits;
    }

    public Set<TextUnitWithAssetPath> getRemovedTextUnits() {
        return removedTextUnits;
    }

    public void setRemovedTextUnits(Set<TextUnitWithAssetPath> removedTextUnits) {
        this.removedTextUnits = removedTextUnits;
    }
}
