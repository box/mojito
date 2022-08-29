package com.box.l10n.mojito.smartling;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SmartlingJsonKeysTest {

  SmartlingJsonKeys smartlingJsonKeys = new SmartlingJsonKeys();

  @Test
  public void toKey() {
    assertThat(smartlingJsonKeys.toKey(1L, "name", "asset")).isEqualTo("1#@#asset#@#name");
  }

  @Test
  public void parse() {
    assertThat(smartlingJsonKeys.parse("2#@#asset#@#name"))
        .extracting(
            SmartlingJsonKeys.Key::getTmTextUnitd,
            SmartlingJsonKeys.Key::getAssetPath,
            SmartlingJsonKeys.Key::getTextUnitName)
        .containsExactly(2L, "asset", "name");
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseInvalid() {
    smartlingJsonKeys.parse("3#@#asset");
  }
}
