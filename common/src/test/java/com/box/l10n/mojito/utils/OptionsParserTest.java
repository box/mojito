package com.box.l10n.mojito.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class OptionsParserTest {

  @Test
  public void testNullGivesEmptyMap() {
    OptionsParser optionsParser = new OptionsParser(null);
    assertTrue(optionsParser.options.isEmpty());
  }

  @Test
  public void testSplit() {
    OptionsParser optionsParser =
        new OptionsParser(Arrays.asList("option1=value1", "option2=value2"));
    AtomicBoolean called = new AtomicBoolean(false);
    optionsParser.getString(
        "option1",
        v -> {
          called.set(true);
          assertEquals("value1", v);
        });
    optionsParser.getString(
        "option2",
        v -> {
          called.set(true);
          assertEquals("value2", v);
        });
    assertTrue(called.get());
  }

  @Test
  public void testGetBoolean() {
    OptionsParser optionsParser = new OptionsParser(Arrays.asList("option1=true", "option2=false"));
    AtomicBoolean called = new AtomicBoolean(false);
    optionsParser.getBoolean(
        "option1",
        v -> {
          called.set(true);
          assertTrue(v);
        });

    optionsParser.getBoolean(
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
    OptionsParser optionsParser = new OptionsParser(Arrays.asList("option1=someobjectvalue"));
    optionsParser.getString("option1", v -> forTestSetObjectVariable = v);
    assertEquals("someobjectvalue", forTestSetObjectVariable);
  }
}
