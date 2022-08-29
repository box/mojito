package com.box.l10n.mojito.smartling.request;

import java.util.ArrayList;
import java.util.List;

public class Bindings {
  List<Binding> bindings = new ArrayList<>();

  public List<Binding> getBindings() {
    return bindings;
  }

  public void setBindings(List<Binding> bindings) {
    this.bindings = bindings;
  }
}
