package com.box.l10n.mojito.service.tm.search;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jaurambault
 */
public class NativeColumnEqExpTest {

    @Test
    public void testToSQL() {
        NativeColumnEqExp nativeColumnEqExp = new NativeColumnEqExp("col1", "col2");
        Assert.assertEquals("col1 = col2", nativeColumnEqExp.toSQL());
    }

}
