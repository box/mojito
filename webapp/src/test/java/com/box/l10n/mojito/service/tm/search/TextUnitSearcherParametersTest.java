package com.box.l10n.mojito.service.tm.search;

import static org.junit.Assert.assertEquals;

import com.box.l10n.mojito.service.NormalizationUtils;
import org.junit.Test;

public class TextUnitSearcherParametersTest {

  @Test
  public void regexSearchStillNormalizes() {
    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
    parameters.setSearchType(SearchType.REGEX);
    String raw = "A\u030A.*";
    String normalized = NormalizationUtils.normalize(raw);

    parameters.setSource(raw);
    parameters.setTarget(raw);

    assertEquals(normalized, parameters.getSource());
    assertEquals(normalized, parameters.getTarget());
  }

  @Test
  public void nonRegexSearchNormalizesByDefault() {
    TextUnitSearcherParameters parameters = new TextUnitSearcherParameters();
    parameters.setSearchType(SearchType.EXACT);
    String raw = "A\u030A";
    String normalized = NormalizationUtils.normalize(raw);

    parameters.setSource(raw);
    parameters.setTarget(raw);

    assertEquals(normalized, parameters.getSource());
    assertEquals(normalized, parameters.getTarget());
  }
}
