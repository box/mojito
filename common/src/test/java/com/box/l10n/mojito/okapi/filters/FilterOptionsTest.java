package com.box.l10n.mojito.okapi.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class FilterOptionsTest {

  @Test
  public void testNullGivesEmptyMap() {
    FilterOptions filterOptions = new FilterOptions(null);
    assertTrue(filterOptions.options.isEmpty());
  }

  @Test
  public void testSplit() {
    FilterOptions filterOptions =
        new FilterOptions(Arrays.asList("option1=value1", "option2=value2"));
    AtomicBoolean called = new AtomicBoolean(false);
    filterOptions.getString(
        "option1",
        v -> {
          called.set(true);
          assertEquals("value1", v);
        });
    filterOptions.getString(
        "option2",
        v -> {
          called.set(true);
          assertEquals("value2", v);
        });
    assertTrue(called.get());
  }

  @Test
  public void testGetBoolean() {
    FilterOptions filterOptions = new FilterOptions(Arrays.asList("option1=true", "option2=false"));
    AtomicBoolean called = new AtomicBoolean(false);
    filterOptions.getBoolean(
        "option1",
        v -> {
          called.set(true);
          assertTrue(v);
        });

    filterOptions.getBoolean(
        "option2",
        v -> {
          called.set(true);
          assertFalse(v);
        });
    assertTrue(called.get());
  }

  Object forTestSetObjectVariable = null;

  @Test
  public void testSetObjectVariable() {
    FilterOptions filterOptions = new FilterOptions(Arrays.asList("option1=someobjectvalue"));
    filterOptions.getString("option1", v -> forTestSetObjectVariable = v);
    assertEquals("someobjectvalue", forTestSetObjectVariable);
  }
}
