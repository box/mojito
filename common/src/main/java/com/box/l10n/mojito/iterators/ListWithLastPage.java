package com.box.l10n.mojito.iterators;

import java.util.List;

public class ListWithLastPage<T> {
  List<T> list;
  int lastPage;

  public List<T> getList() {
    return list;
  }

  public void setList(List<T> list) {
    this.list = list;
  }

  public int getLastPage() {
    return lastPage;
  }

  public void setLastPage(int lastPage) {
    this.lastPage = lastPage;
  }
}
