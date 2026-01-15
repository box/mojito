package com.box.l10n.mojito.rest.textunit;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;

public class TextUnitWSSearchValidationTest {

  TextUnitWS textUnitWS = new TextUnitWS();

  @Test
  public void mapsTranslationCreatedDates() throws Exception {
    TextUnitSearchBody body = new TextUnitSearchBody();
    body.setRepositoryIds(new ArrayList<>(Arrays.asList(1L)));
    body.setLocaleTags(new ArrayList<>(Arrays.asList("en")));
    body.setTmTextUnitVariantCreatedBefore(ZonedDateTime.parse("2024-01-01T00:00:00Z"));
    body.setTmTextUnitVariantCreatedAfter(ZonedDateTime.parse("2023-12-01T00:00:00Z"));

    TextUnitSearcherParameters parameters =
        textUnitWS.textUnitSearchBodyToTextUnitSearcherParameters(body);

    assertEquals(
        ZonedDateTime.parse("2024-01-01T00:00:00Z"),
        parameters.getTmTextUnitVariantCreatedBefore());
    assertEquals(
        ZonedDateTime.parse("2023-12-01T00:00:00Z"), parameters.getTmTextUnitVariantCreatedAfter());
  }
}
