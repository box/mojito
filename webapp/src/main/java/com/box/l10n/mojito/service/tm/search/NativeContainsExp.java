package com.box.l10n.mojito.service.tm.search;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;
import com.github.pnowy.nc.utils.VarGenerator;

/**
 * Contains expression (behave likes a "like" expression but wraps the value
 * to be search in %% to do a contains, ie ).
 */
public class NativeContainsExp implements NativeExp {

    private String columnName;
    private String varName;
    private String value;

    /**
     * @param columnName the column name
     * @param value the value
     */
    public NativeContainsExp(String columnName, String value) {
        
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
        return columnName + " LIKE :" + varName;
    }

    @Override
    public void setValues(NativeQuery query) {
        query.setString(varName, escapeAndWrapValue(value));
    }
    
    String escapeAndWrapValue(String value) {
        String escaped = value.replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

}
