package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.List;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitRepository extends JpaRepository<TMTextUnit, Long> {

    TMTextUnit findFirstByTmAndMd5(TM tm, String md5);

    List<TMTextUnit> findByTm_id(Long tmId);

    @Query("select new com.box.l10n.mojito.service.tm.TextUnitIdMd5DTO(tu.id, tu.md5) from TMTextUnit tu where tu.asset.id = ?1")
    List<TextUnitIdMd5DTO> getTextUnitIdMd5DTOByAssetId(Long assetId);
 
}
