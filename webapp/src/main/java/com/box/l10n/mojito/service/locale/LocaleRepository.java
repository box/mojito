package com.box.l10n.mojito.service.locale;

import com.box.l10n.mojito.entity.Locale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jaurambault */
@RepositoryRestResource(exported = false)
public interface LocaleRepository
    extends JpaRepository<Locale, Long>, JpaSpecificationExecutor<Locale> {}
