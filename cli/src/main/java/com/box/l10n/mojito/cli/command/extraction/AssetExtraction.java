package com.box.l10n.mojito.cli.command.extraction;

import com.box.l10n.mojito.okapi.extractor.TextUnit;

import java.util.List;

public class AssetExtraction {

    List<TextUnit> textUnits;

    List<String> filterOptions;

    String fileType;

    String name;

    public List<TextUnit> getTextUnits() {
        return textUnits;
    }

    public void setTextUnits(List<TextUnit> textUnits) {
        this.textUnits = textUnits;
    }

    public List<String> getFilterOptions() {
        return filterOptions;
    }

    public void setFilterOptions(List<String> filterOptions) {
        this.filterOptions = filterOptions;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
