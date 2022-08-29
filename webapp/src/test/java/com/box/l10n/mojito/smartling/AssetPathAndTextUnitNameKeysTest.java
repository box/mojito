package com.box.l10n.mojito.smartling;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AssetPathAndTextUnitNameKeysTest {

  @Test
  public void toKey() {
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys = new AssetPathAndTextUnitNameKeys();
    assertEquals("a1#@#t1", assetPathAndTextUnitNameKeys.toKey("a1", "t1"));
  }

  @Test
  public void parse() {
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys = new AssetPathAndTextUnitNameKeys();
    AssetPathAndTextUnitNameKeys.Key parse = assetPathAndTextUnitNameKeys.parse("a2#@#t2");
    assertEquals("a2", parse.getAssetPath());
    assertEquals("t2", parse.getTextUnitName());
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseInvalid() {
    AssetPathAndTextUnitNameKeys assetPathAndTextUnitNameKeys = new AssetPathAndTextUnitNameKeys();
    AssetPathAndTextUnitNameKeys.Key parse = assetPathAndTextUnitNameKeys.parse("a2invalid");
  }
}
