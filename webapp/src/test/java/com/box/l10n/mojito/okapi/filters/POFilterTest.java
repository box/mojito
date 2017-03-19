package com.box.l10n.mojito.okapi.filters;

import net.sf.okapi.common.LocaleId;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jeanaurambault
 */
public class POFilterTest {

    @Test
    public void testNeedsExtraPluralNull() {
        POFilter poFilter = new POFilter();
        assertTrue(poFilter.needsExtraPluralForm(null));
    }
    
    @Test
    public void testNeedsExtraPluralEmpty() {
        POFilter poFilter = new POFilter();
        assertTrue(poFilter.needsExtraPluralForm(LocaleId.EMPTY));
    }

    @Test
    public void testNeedsExtraPluralArAr() {
        POFilter poFilter = new POFilter();
        assertTrue(poFilter.needsExtraPluralForm(LocaleId.fromBCP47("ar-AR")));
    }

    @Test
    public void testNeedsExtraPluralRuRu() {
        POFilter poFilter = new POFilter();
        assertTrue(poFilter.needsExtraPluralForm(LocaleId.fromBCP47("ru-RU")));
    }

    @Test
    public void testNeedsExtraPluralFrFr() {
        POFilter poFilter = new POFilter();
        assertFalse(poFilter.needsExtraPluralForm(LocaleId.fromBCP47("fr-FR")));
    }

}
