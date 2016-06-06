package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.AssetTextUnitToTMTextUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author aloison
 */
@RepositoryRestResource(exported = false)
public interface AssetTextUnitToTMTextUnitRepository extends JpaRepository<AssetTextUnitToTMTextUnit, Long> {

    public void deleteByAssetExtractionId(Long assetExtractionId);

}
