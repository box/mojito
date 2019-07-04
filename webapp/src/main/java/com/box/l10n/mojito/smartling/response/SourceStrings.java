package com.box.l10n.mojito.smartling.response;

import java.util.List;

public class SourceStrings {
    List<StringInfo> items;
    Integer totalCount;

    public List<StringInfo> getItems() {
        return items;
    }

    public void setItems(List<StringInfo> items) {
        this.items = items;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

}
