package com.box.l10n.mojito.okapi.filters;

import org.junit.Test;

import static org.junit.Assert.*;

public class FilterOptionsTest {

    Object setObjectVariable = null;

    @Test
    public void testNullGivesEmptyMap() {
        FilterOptions filterOptions = new FilterOptions(null);
        assertTrue(filterOptions.options.isEmpty());
    }

    @Test
    public void testSplit() {
        FilterOptions filterOptions = new FilterOptions("option1=value1;option2=value2");
        filterOptions.getString("option1", v -> assertEquals("value1", v));
        filterOptions.getString("option2", v -> assertEquals("value2", v));
    }

    @Test
    public void testGetBoolean() {
        FilterOptions filterOptions = new FilterOptions("option1=true;option2=false");
        filterOptions.getBoolean("option1", v -> assertTrue(v));
        filterOptions.getBoolean("option2", v -> assertFalse(v));
    }

    @Test
    public void testSetObjectVariable() {
        FilterOptions filterOptions = new FilterOptions("option1=someobjectvalue");
        filterOptions.getString("option1", v -> setObjectVariable = v);
        assertEquals(setObjectVariable, "someobjectvalue");
    }

}