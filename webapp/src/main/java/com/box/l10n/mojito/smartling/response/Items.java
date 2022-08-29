package com.box.l10n.mojito.smartling.response;

import java.util.List;

public class Items<T> {
  List<T> items;
  Integer totalCount;

  public List<T> getItems() {
    return items;
  }

  public void setItems(List<T> items) {
    this.items = items;
  }

  public Integer getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(Integer totalCount) {
    this.totalCount = totalCount;
  }
}
