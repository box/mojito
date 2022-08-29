package com.box.l10n.mojito.service.tm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class PluralNameParserTest {

  PluralNameParser pluralNameParser = new PluralNameParser();

  @Test
  public void testGetPrefix() {
    assertThat(pluralNameParser.getPrefix("name_zero", "_")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name_one", "_")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name_two", "_")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name_few", "_")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name_many", "_")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name_other", "_")).isEqualTo("name");

    assertThat(pluralNameParser.getPrefix("name _zero", " _")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name _one", " _")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name _two", " _")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name _few", " _")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name _many", " _")).isEqualTo("name");
    assertThat(pluralNameParser.getPrefix("name _other", " _")).isEqualTo("name");

    assertThat(pluralNameParser.getPrefix("#@#name _zero", " _")).isEqualTo("#@#name");
    assertThat(pluralNameParser.getPrefix("$%name@ _one", " _")).isEqualTo("$%name@");
    assertThat(pluralNameParser.getPrefix("_name_ _two", " _")).isEqualTo("_name_");
    assertThat(pluralNameParser.getPrefix("name_few", " _")).isEqualTo("name_few");
    assertThat(pluralNameParser.getPrefix("name%many", "_")).isEqualTo("name%many");
  }
}
