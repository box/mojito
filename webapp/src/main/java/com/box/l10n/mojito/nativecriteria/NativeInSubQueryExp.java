package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;

public class NativeInSubQueryExp implements NativeExp {
  private final String columnName;

  private final NativeExp nativeExp;

  public NativeInSubQueryExp(String columnName, NativeExp nativeExp) {
    this.columnName = columnName;
    this.nativeExp = nativeExp;
  }

  @Override
  public String toSQL() {
    return this.columnName + " IN (" + this.nativeExp.toSQL() + ")";
  }

  @Override
  public void setValues(NativeQuery query) {
    this.nativeExp.setValues(query);
  }
}
