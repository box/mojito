package com.box.l10n.mojito.monitoring;

import static org.junit.Assert.*;

import io.micrometer.core.instrument.config.NamingConvention;
import org.junit.Test;

public class NamingConventionConverterTest {

  @Test
  public void testConvert() {
    NamingConventionConverter namingConventionConverter = new NamingConventionConverter();
    NamingConvention snakeCase = namingConventionConverter.convert("snakeCase");
    assertEquals(NamingConvention.snakeCase.getClass().getName(), snakeCase.getClass().getName());
  }

  @Test
  public void testConvertNull() {
    NamingConventionConverter namingConventionConverter = new NamingConventionConverter();
    NamingConvention namingConvention = namingConventionConverter.convert(null);
    assertNull(namingConvention);
  }

  @Test(expected = RuntimeException.class)
  public void testConvertInvalid() {
    NamingConventionConverter namingConventionConverter = new NamingConventionConverter();
    NamingConvention snakeCase = namingConventionConverter.convert("invalid");
  }
}
