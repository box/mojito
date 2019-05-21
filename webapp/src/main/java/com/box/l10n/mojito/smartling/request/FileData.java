package com.box.l10n.mojito.smartling.request;

import java.util.List;

public class FileData {
    List<File> items;
    Integer totalCount;

    public List<File> getItems() {
        return items;
    }

    public void setItems(List<File> items) {
        this.items = items;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}
