package com.box.l10n.mojito.smartling.request;

import java.util.List;

public class StringToContextBindingData {
    List<StringToContextBinding> items;
    Integer totalCount;

    public List<StringToContextBinding> getItems() {
        return items;
    }

    public void setItems(List<StringToContextBinding> items) {
        this.items = items;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }

}
