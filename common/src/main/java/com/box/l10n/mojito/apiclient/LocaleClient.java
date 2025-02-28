package com.box.l10n.mojito.apiclient;

import com.box.l10n.mojito.apiclient.exception.LocaleNotFoundException;
import com.box.l10n.mojito.apiclient.model.Locale;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LocaleClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LocaleClient.class);

  @Autowired private LocaleWsApi localeWsApi;

  public Locale getLocaleByBcp47Tag(String bcp47Tag) throws LocaleNotFoundException {
    logger.debug("Getting locale for BCP47 tag: {}", bcp47Tag);

    List<Locale> locales = this.localeWsApi.getLocales(bcp47Tag);

    if (locales.size() != 1) {
      throw new LocaleNotFoundException("Could not find locale with BCP47 tag: " + bcp47Tag);
    }

    return locales.getFirst();
  }

  public List<Locale> getLocales() {
    logger.debug("Getting all locales");
    return this.localeWsApi.getLocales(null);
  }
}
