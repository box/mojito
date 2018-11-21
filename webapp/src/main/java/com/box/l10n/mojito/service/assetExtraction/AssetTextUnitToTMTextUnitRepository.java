package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetTextUnitToTMTextUnitRepository extends JpaRepository<AssetTextUnitToTMTextUnit, Long> {

    @Transactional
    int deleteByAssetExtractionId(Long assetExtractionId);

    void deleteByAssetTextUnitId(Long assetTextUnitId);

}
