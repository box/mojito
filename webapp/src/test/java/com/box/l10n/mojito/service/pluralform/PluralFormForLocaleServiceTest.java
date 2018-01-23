package com.box.l10n.mojito.service.pluralform;

import com.box.l10n.mojito.entity.PluralFormForLocale;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jeanaurambault
 */
public class PluralFormForLocaleServiceTest extends ServiceTestBase {

    static Logger logger = LoggerFactory.getLogger(PluralFormForLocaleServiceTest.class);

    @Autowired
    PluralFormForLocaleService pluralFormForLocaleService;

    @Autowired
    PluralFormForLocaleRepository pluralFormForLocaleRepository;

    @Test
    public void testShowNewFormsSQL() {
        
        StringBuilder sb = new StringBuilder();
             
        List<PluralFormForLocale> pluralFormsForLocales = pluralFormForLocaleService.getPluralFormsForLocales();
        for (PluralFormForLocale pluralFormsForLocale : pluralFormsForLocales) {
            PluralFormForLocale findByLocale = pluralFormForLocaleRepository.findByLocaleAndPluralForm(pluralFormsForLocale.getLocale(), pluralFormsForLocale.getPluralForm());

            if (findByLocale == null) {
                PluralFormForLocale savedPluralFormForLocale = pluralFormForLocaleRepository.save(pluralFormsForLocale);

                sb.append("insert into plural_form_for_locale (locale_id, plural_form_id) values (").
                        append(savedPluralFormForLocale.getLocale().getId()).
                        append(",").
                        append(savedPluralFormForLocale.getPluralForm().getId()).
                        append(");\n");

                logger.info("insert into plural_form_for_locale (locale_id, plural_form_id) values ({}, {});",
                        savedPluralFormForLocale.getLocale().getId(),
                        savedPluralFormForLocale.getPluralForm().getId());
            }
        }
        
        logger.info(sb.toString());
    }

}
