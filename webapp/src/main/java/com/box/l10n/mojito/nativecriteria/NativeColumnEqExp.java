package com.box.l10n.mojito.nativecriteria;

import com.github.pnowy.nc.core.NativeQuery;
import com.github.pnowy.nc.core.expressions.NativeExp;
import com.github.pnowy.nc.utils.Strings;

/**
 * Add support to compare columns
 *
 * @author jaurambault
 */
public class NativeColumnEqExp implements NativeExp {

    /**
     * Right column name.
     */
    private String right;

    /**
     * Left column name.
     */
    private String left;

    /**
     *
     * @param right right column name
     * @param left left column name
     */
    public NativeColumnEqExp(String right, String left) {
        if (Strings.isBlank(right)) {
            throw new IllegalStateException("columnName is null!");
        }
        if (Strings.isBlank(left)) {
            throw new IllegalStateException("value is null!");
        }

        this.right = right;
        this.left = left;
    }

    @Override
    public String toSQL() {

        return right + " = " + left;
    }

    @Override
    public void setValues(NativeQuery query) {
    }

}
