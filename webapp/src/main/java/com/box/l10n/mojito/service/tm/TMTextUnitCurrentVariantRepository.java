package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitCurrentVariantRepository
    extends JpaRepository<TMTextUnitCurrentVariant, Long> {

  TMTextUnitCurrentVariant findByLocale_IdAndTmTextUnit_Id(Long localeId, Long tmTextUnitId);

  List<TMTextUnitCurrentVariant> findByTmTextUnit_Id(Long tmTextUnitId);

  List<TMTextUnitCurrentVariant> findByTmTextUnit_Tm_IdAndLocale_Id(Long tmId, Long localeId);

  @Query(
      """
      select new com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantDTO(ttucv.tmTextUnit.id, ttucv.tmTextUnitVariant.id)
      from #{#entityName} ttucv
      where ttucv.asset.id = ?1 and ttucv.locale.id = ?2
      """)
  List<TMTextUnitCurrentVariantDTO> findByAsset_idAndLocale_Id(Long assetId, Long localeId);
}
