package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.LocaleNotFoundException;
import com.box.l10n.mojito.rest.entity.Locale;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** @author wyau */
@Component
public class LocaleClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LocaleClient.class);

  @Override
  public String getEntityName() {
    return "locales";
  }

  /**
   * Get a list of {@link Locale} in the system
   *
   * @return
   */
  public List<Locale> getLocales() {
    logger.debug("Getting all locales");
    return authenticatedRestTemplate.getForObjectAsList(getBasePathForEntity(), Locale[].class);
  }

  /**
   * Get the {@link Locale} associated to the given BCP47 tag
   *
   * @param bcp47Tag The BCP47 tag of the locale
   * @return The locale associated to the given tag or {@code null} if none found
   */
  public Locale getLocaleByBcp47Tag(String bcp47Tag) throws LocaleNotFoundException {
    logger.debug("Getting locale for BCP47 tag: {}", bcp47Tag);

    Map<String, String> params = new HashMap<>();
    params.put("bcp47Tag", bcp47Tag);

    List<Locale> localeAsList =
        authenticatedRestTemplate.getForObjectAsListWithQueryStringParams(
            getBasePathForEntity(), Locale[].class, params);

    if (localeAsList.size() != 1) {
      throw new LocaleNotFoundException("Could not find locale with BCP47 tag: " + bcp47Tag);
    }

    return localeAsList.get(0);
  }
}
