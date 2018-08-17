package com.box.l10n.mojito.rest.textunit;

import java.util.List;

public class TextUnitWithUsage {
    List<String> usages;

    String textUnitName;

    Long textUnitId;

    public List<String> getUsages() {
        return usages;
    }

    public void setUsages(List<String> usages) {
        this.usages = usages;
    }

    public String getTextUnitName() {
        return textUnitName;
    }

    public void setTextUnitName(String textUnitName) {
        this.textUnitName = textUnitName;
    }

    public Long getTextUnitId() {
        return textUnitId;
    }

    public void setTextUnitId(Long textUnitId) {
        this.textUnitId = textUnitId;
    }
}
