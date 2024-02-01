package com.box.l10n.mojito.service.pluralform;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralFormForLocale;
import com.box.l10n.mojito.service.locale.LocaleRepository;
import com.ibm.icu.text.PluralRules;
import com.ibm.icu.util.ULocale;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author jaurambault
 */
@Service
public class PluralFormForLocaleService {

  static Logger logger = LoggerFactory.getLogger(PluralFormForLocaleService.class);

  @Autowired PluralFormService pluralFormService;

  @Autowired LocaleRepository localeRepository;

  @Autowired PluralFormForLocaleRepository pluralFormForLocaleRepository;

  public List<PluralFormForLocale> getPluralFormsForLocales() {

    List<PluralFormForLocale> pluralFormForLocales = new ArrayList<>();

    List<Locale> locales = localeRepository.findAll();

    for (Locale locale : locales) {

      ULocale forLanguageTag = ULocale.forLanguageTag(locale.getBcp47Tag());

      PluralRules pluralRules = PluralRules.forLocale(forLanguageTag);
      for (String keyword : pluralRules.getKeywords()) {
        logger.debug("{} : {} : {}", locale.getId(), locale.getBcp47Tag(), keyword);

        PluralFormForLocale pluralFormForLocale = new PluralFormForLocale();
        pluralFormForLocale.setPluralForm(pluralFormService.findByPluralFormString(keyword));
        pluralFormForLocale.setLocale(locale);

        pluralFormForLocales.add(pluralFormForLocale);
      }
    }

    return pluralFormForLocales;
  }
}
