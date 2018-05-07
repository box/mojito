package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jaurambault
 */
public class NativeInExpFix implements NativeExp {

    private static final Logger log = LoggerFactory.getLogger(NativeInExpFix.class);
    private String columnName;
    @SuppressWarnings("unchecked")
    private Collection values;
    private Object[] arrValues;
    private String varName;

    /**
     * @param columnName the column name
     * @param values the values
     */
    @SuppressWarnings("unchecked")
    public NativeInExpFix(String columnName, Collection values) {
        if (Strings.isBlank(columnName)) {
            throw new IllegalStateException("columnName is null!");
        }
        if (values == null) {
            throw new IllegalStateException("values is null!");
        }

        this.columnName = columnName;
        this.values = values;
    }

    /**
     * @param columnName the column name
     * @param values the values
     */
    public NativeInExpFix(String columnName, Object[] values) {
        if (Strings.isBlank(columnName)) {
            throw new IllegalStateException("columnName is null!");
        }
        if (values == null) {
            throw new IllegalStateException("values is null!");
        }

        this.columnName = columnName;
        this.arrValues = values;
    }

    @Override
    public String toSQL() {
        varName = VarGenerator.gen(columnName);
        return columnName + " IN (:" + varName + ")";
    }

    @Override
    public void setValues(NativeQuery query) {
        if (values != null) {
            query.setParameterList(varName, values);
        } else if (arrValues != null) {
            query.setParameterList(varName, arrValues);
        }
    }
}
