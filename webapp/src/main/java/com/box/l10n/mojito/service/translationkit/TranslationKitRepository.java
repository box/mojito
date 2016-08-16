package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.TranslationKit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TranslationKitRepository extends JpaRepository<TranslationKit, Long> {

}
