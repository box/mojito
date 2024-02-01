package com.box.l10n.mojito.service.pluralform;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PluralFormForLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface PluralFormForLocaleRepository
    extends JpaRepository<PluralFormForLocale, Long>,
        JpaSpecificationExecutor<PluralFormForLocale> {

  PluralFormForLocale findByLocaleAndPluralForm(Locale locale, PluralForm pluralForm);
}
