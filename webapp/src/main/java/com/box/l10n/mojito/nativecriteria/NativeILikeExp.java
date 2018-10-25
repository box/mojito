package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;

/**
 * ILIKE expression.
 */
public class NativeILikeExp implements NativeExp {

    private String columnName;
    private String varName;
    private String value;

    /**
     * @param columnName the column name
     * @param value the value
     */
    public NativeILikeExp(String columnName, String value) {
        
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
        return "lower(" + columnName + ") LIKE :" + varName;
    }

    @Override
    public void setValues(NativeQuery query) {
        query.setString(varName, value.toLowerCase());
    }

}
