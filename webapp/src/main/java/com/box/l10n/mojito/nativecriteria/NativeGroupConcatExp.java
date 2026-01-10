package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;

public class NativeGroupConcatExp implements NativeExp {

  String columnName;
  String alias;

  public NativeGroupConcatExp(String columnName, String alias) {
    if (Strings.isBlank(columnName)) {
      throw new IllegalStateException("columnName is null!");
    }
    this.columnName = columnName;
    this.alias = alias;
  }

  @Override
  public String toSQL() {
    StringBuilder sb = new StringBuilder();
    sb.append("GROUP_CONCAT(").append(columnName).append(")");
    if (alias != null && !alias.isBlank()) {
      sb.append(" as ").append(alias);
    }
    return sb.toString();
  }

  @Override
  public void setValues(NativeQuery query) {}
}
