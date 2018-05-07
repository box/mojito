package com.box.l10n.mojito.nativecriteria;

import com.box.l10n.mojito.nativecriteria.NativeColumnNotEqExp;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jaurambault
 */
public class NativeColumnNotEqExpTest {

    @Test
    public void testToSQL() {
        NativeColumnNotEqExp nativeColumnNotEqExp = new NativeColumnNotEqExp("col1", "col2");
        Assert.assertEquals("col1 != col2", nativeColumnNotEqExp.toSQL());
    }

}
