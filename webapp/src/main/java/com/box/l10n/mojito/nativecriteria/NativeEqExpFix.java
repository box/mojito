package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;

/**
 * @author jeanaurambault
 */
public class NativeEqExpFix implements NativeExp {
  /** Column name. */
  private String columnName;

  /** Variable name. */
  private String varName;

  /** Compared value. */
  private Object value;

  /**
   * @param columnName the column name
   * @param value the value
   */
  public NativeEqExpFix(String columnName, Object value) {
    if (Strings.isBlank(columnName)) throw new IllegalStateException("columnName is null!");
    if (value == null) throw new IllegalStateException("value is null!");

    this.columnName = columnName;
    this.value = value;
  }

  @Override
  public String toSQL() {
    varName = VarGenerator.gen(columnName);
    return columnName + " = :" + varName;
  }

  @Override
  public void setValues(NativeQuery query) {
    query.setParameter(varName, value);
  }
}
