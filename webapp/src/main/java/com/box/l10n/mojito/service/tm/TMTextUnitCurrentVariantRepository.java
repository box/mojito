package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitCurrentVariantRepository extends JpaRepository<TMTextUnitCurrentVariant, Long> {

    TMTextUnitCurrentVariant findByLocale_IdAndTmTextUnit_Id(Long localeId, Long tmTextUnitId);

    List<TMTextUnitCurrentVariant> findByTmTextUnit_Id(Long tmTextUnitId);

    List<TMTextUnitCurrentVariant> findByTmTextUnit_Tm_IdAndLocale_Id(Long tmId, Long localeId);

}
