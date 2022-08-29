package com.box.l10n.mojito.cli.command;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.beust.jcommander.ParameterException;
import java.util.Map;
import org.junit.Test;

public class AssetMappingConverterTest {

  @Test
  public void testConvertNull() {
    AssetMappingConverter assetMappingConverter = new AssetMappingConverter();
    Map<String, String> convert = assetMappingConverter.convert(null);
    assertNull(convert);
  }

  @Test
  public void testConvertValid() {
    AssetMappingConverter assetMappingConverter = new AssetMappingConverter();
    Map<String, String> convert = assetMappingConverter.convert("1a:1b;2a:2b");
    assertEquals("1b", convert.get("1a"));
    assertEquals("2b", convert.get("2a"));
    assertEquals(2, convert.size());
  }

  @Test(expected = ParameterException.class)
  public void testConvertInvalid() {
    AssetMappingConverter assetMappingConverter = new AssetMappingConverter();
    Map<String, String> convert = assetMappingConverter.convert("dsafdsf");
    assertEquals(0, convert.size());
  }
}
