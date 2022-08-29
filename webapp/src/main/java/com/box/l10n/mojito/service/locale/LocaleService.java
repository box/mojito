package com.box.l10n.mojito.service.locale;

import com.box.l10n.mojito.entity.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** @author jaurambault */
@Service
public class LocaleService {

  public static final String DEFAULT_LOCALE_BCP47_TAG = "en";

  @Autowired LocaleRepository localeRepository;

  /** @return Map BCP47 => Locale. The map will be cached. */
  @Cacheable("locales")
  private Map<String, Locale> getLocalesBcp47TagMap() {

    Map<String, Locale> localesMap = new HashMap<>();
    List<Locale> allLocales = localeRepository.findAll();

    for (Locale locale : allLocales) {
      // Okapi uses lowercase BCP47 tags (fr-FR is converted to fr-fr)
      String lowercaseBcp47Tag = locale.getBcp47Tag().toLowerCase();
      localesMap.put(lowercaseBcp47Tag, locale);
    }

    return localesMap;
  }

  /** @return Map ID => Locale. The map will be cached. */
  @Cacheable("locales")
  private Map<Long, Locale> getLocalesIdMap() {

    Map<Long, Locale> localesMap = new HashMap<>();
    List<Locale> allLocales = localeRepository.findAll();

    for (Locale locale : allLocales) {
      localesMap.put(locale.getId(), locale);
    }

    return localesMap;
  }

  /**
   * Returns the locale for the given BCP47 tag. It searches in the locales map to find a
   * correspondance.
   *
   * @param bcp47Tag The BCP47 tag of the locale
   * @return The corresponding locale or {@code null} if none found
   */
  public Locale findByBcp47Tag(String bcp47Tag) {
    // Okapi uses lowercase BCP47 tags (fr-FR is converted to fr-fr)
    return getLocalesBcp47TagMap().get(bcp47Tag.toLowerCase());
  }

  /**
   * Returns the locale for the given ID. It searches in the locales map to find a correspondance.
   *
   * @param localeId The ID of the locale
   * @return The corresponding locale or {@code null} if none found
   */
  public Locale findById(Long localeId) {
    return getLocalesIdMap().get(localeId);
  }

  public Locale getDefaultLocale() {
    return findByBcp47Tag(DEFAULT_LOCALE_BCP47_TAG);
  }
}
