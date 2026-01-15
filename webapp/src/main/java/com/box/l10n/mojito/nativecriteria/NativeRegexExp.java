package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;

/** REGEXP_LIKE expression for regex searches. */
public class NativeRegexExp implements NativeExp {

  private final String columnName;
  private String varName;
  private final String value;

  public NativeRegexExp(String columnName, String value) {

    if (Strings.isBlank(columnName)) {
      throw new IllegalStateException("columnName is null!");
    }

    if (Strings.isBlank(value)) {
      throw new IllegalStateException("value is null!");
    }

    this.columnName = columnName;
    this.value = value;
  }

  @Override
  public String toSQL() {
    varName = VarGenerator.gen(columnName);
    return "REGEXP_LIKE(" + columnName + ", :" + varName + ")";
  }

  @Override
  public void setValues(NativeQuery query) {
    query.setString(varName, value);
  }
}
