package com.box.l10n.mojito.service.pluralform;

import com.box.l10n.mojito.entity.PluralFormForLocale;
import com.box.l10n.mojito.service.assetExtraction.ServiceTestBase;
import java.util.List;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author jeanaurambault
 */
public class PluralFormForLocaleServiceTest extends ServiceTestBase {

    @Autowired
    PluralFormForLocaleService pluralFormForLocaleService;

    @Autowired
    PluralFormForLocaleRepository pluralFormForLocaleRepository;

    @Test
    public void testShowForms() {
        List<PluralFormForLocale> pluralFormsForLocales = pluralFormForLocaleService.getPluralFormsForLocales();
      //  pluralFormForLocaleRepository.save(pluralFormsForLocales);
    }

}
