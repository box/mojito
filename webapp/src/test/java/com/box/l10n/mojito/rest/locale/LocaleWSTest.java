package com.box.l10n.mojito.rest.locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.box.l10n.mojito.rest.WSTestBase;
import com.box.l10n.mojito.rest.client.LocaleClient;
import com.box.l10n.mojito.rest.client.exception.LocaleNotFoundException;
import com.box.l10n.mojito.rest.entity.Locale;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author aloison
 */
public class LocaleWSTest extends WSTestBase {

  @Autowired LocaleClient localeClient;

  @Autowired LocaleRepository localeRepository;

  @Test
  public void testGetLocales() {
    List<Locale> locales = localeClient.getLocales();
    List<com.box.l10n.mojito.entity.Locale> expectedLocales = localeRepository.findAll();

    assertEquals(expectedLocales.size(), locales.size());

    List<String> expectedBcp47Tags = new ArrayList<>();
    for (com.box.l10n.mojito.entity.Locale expectedLocale : expectedLocales) {
      expectedBcp47Tags.add(expectedLocale.getBcp47Tag());
    }

    for (Locale locale : locales) {
      assertTrue(expectedBcp47Tags.contains(locale.getBcp47Tag()));
    }
  }

  @Test
  public void testGetLocaleByBcp47TagWithValidTag() throws LocaleNotFoundException {
    Locale locale = localeClient.getLocaleByBcp47Tag("ja-JP");
    assertEquals("ja-JP", locale.getBcp47Tag());
  }

  @Test(expected = LocaleNotFoundException.class)
  public void testGetLocaleByBcp47TagWithInvalidTag() throws LocaleNotFoundException {
    localeClient.getLocaleByBcp47Tag("invalid-Tag");
  }
}
