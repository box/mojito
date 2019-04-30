package com.box.l10n.mojito.smartling.request;

import java.util.List;

public class StringData {
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
