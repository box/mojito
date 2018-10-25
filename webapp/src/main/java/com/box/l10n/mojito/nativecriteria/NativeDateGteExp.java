package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp; 
import com.github.pnowy.nc.utils.Strings;
import org.joda.time.DateTime;

/**
 *
 * @author jeanaurambault
 */
public class NativeDateGteExp implements NativeExp {
    /**
     * Column name.
     */
    private String columnName;

    /**
     * Variable name.
     */
    private String varName;

    /**
     * Compared value.
     */
    private DateTime value;

    /**
     * @param columnName the column name
     * @param value      the value
     */
    public NativeDateGteExp(String columnName, DateTime value) {
        if (Strings.isBlank(columnName))
            throw new IllegalStateException("columnName is null!");
        if (value == null)
            throw new IllegalStateException("value is null!");

        this.columnName = columnName;
        this.value = value;
    }

    @Override
    public String toSQL() {
        varName = VarGenerator.gen(columnName);
        return columnName + " >= :" + varName;
    }

    @Override
    public void setValues(NativeQuery query) {
        
        if (query instanceof JpaNativeQuery) {
            JpaNativeQuery jpaNativeQuery = (JpaNativeQuery) query;
            jpaNativeQuery.setParameter(varName, value);
        } else {
            throw new RuntimeException("Must but used with proper JpaNativeQuery");
        } 
    }

}
