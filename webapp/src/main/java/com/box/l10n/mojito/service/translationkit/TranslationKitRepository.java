package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.Drop;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.TranslationKit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TranslationKitRepository extends JpaRepository<TranslationKit, Long> {

  @Override
  @EntityGraph(value = "TranslationKit.legacy", type = EntityGraphType.FETCH)
  Optional<TranslationKit> findById(Long aLong);

  List<TranslationKit> findByDropId(Long dropId);

  TranslationKit findByDropAndLocale(Drop drop, Locale locale);
}
