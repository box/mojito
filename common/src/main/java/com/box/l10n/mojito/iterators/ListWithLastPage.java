package com.box.l10n.mojito.iterators;

import java.util.List;

public class ListWithLastPage<T> {
  List<T> list;
  long lastPage;

  public List<T> getList() {
    return list;
  }

  public void setList(List<T> list) {
    this.list = list;
  }

  public long getLastPage() {
    return lastPage;
  }

  public void setLastPage(long lastPage) {
    this.lastPage = lastPage;
  }
}
