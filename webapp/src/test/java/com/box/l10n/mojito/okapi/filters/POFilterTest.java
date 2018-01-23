package com.box.l10n.mojito.okapi.filters;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jeanaurambault
 */
public class POFilterTest {

    @Test
    public void loadMsgIDPluralFromParent() {
        POFilter poFilter = new POFilter();
        poFilter.loadMsgIDPluralFromParent();
        assertNull(poFilter.msgIDPlural);
    }

    @Test
    public void loadMsgIDFromParent() {
        POFilter poFilter = new POFilter();
        poFilter.loadMsgIDFromParent();
        assertNull(poFilter.msgID);
    }

}
