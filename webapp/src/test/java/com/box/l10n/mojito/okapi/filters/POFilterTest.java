package com.box.l10n.mojito.okapi.filters;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jeanaurambault
 */
public class POFilterTest {

    @Test
    public void getPoPluralFormFromTextUnitName() {
        POFilter poFilter = new POFilter();
        POFilter.PoPluralsHolder pluralsHolder = poFilter. new PoPluralsHolder() ;
        assertEquals("0", pluralsHolder.getPoPluralFormFromTextUnitName("[bla-0]"));
        assertEquals("1", pluralsHolder.getPoPluralFormFromTextUnitName("[bla-1]"));
        assertEquals("2", pluralsHolder.getPoPluralFormFromTextUnitName("[bla-2] some context"));
    }
    
    @Test
    public void getNewTextUnitName() {
        POFilter poFilter = new POFilter();
        POFilter.PoPluralsHolder pluralsHolder = poFilter. new PoPluralsHolder() ;
        assertEquals("[bla-one]", pluralsHolder.getNewTextUnitName("[bla-1]", "one"));   
        assertEquals("[bla-many] some context", pluralsHolder.getNewTextUnitName("[bla-2] some context", "many"));   
    }
        
    @Test
    public void loadMsgIDPluralFromParent() {
        POFilter poFilter = new POFilter();
        poFilter.loadMsgIDPluralFromParent();
        assertNull(poFilter.msgIDPlural);
    }

}
