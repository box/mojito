package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * @author aloison
 */
@Service
public class AssetExtractionCleanupService {

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    AssetExtractionRepository assetExtractionRepository;

    @Autowired
    AssetTextUnitRepository assetTextUnitRepository;

    @Autowired
    AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

    /**
     * Removes all {@link AssetExtraction}s and {@link AssetTextUnit}s
     * that are no longer useful. This means everything refering to an
     * extraction that is finished and older than the last successful one.
     */
    public void cleanupOldAssetExtractions() {

        List<Long> assetExtractionIdsToDelete;

        do {
            // Fetching 5 by 5 to avoid locking too many rows.
            // It is also useful to distribute the load across multiple instances.
            PageRequest pageable = new PageRequest(0, 5);
            assetExtractionIdsToDelete = assetExtractionRepository.findFinishedAndOldAssetExtractions(pageable);

            for (Long assetExtractionIdToDelete : assetExtractionIdsToDelete) {
                deleteAssetExtractionAndRelatedEntities(assetExtractionIdToDelete);
            }
        } while (!assetExtractionIdsToDelete.isEmpty());
    }

    /**
     * Deletes all asset extractions as well as the entities that have a relationship
     * with the given entity.
     *
     * @param assetExtractionIdToDelete {@link AssetExtraction#id} to be deleted
     */
    @Transactional
    private void deleteAssetExtractionAndRelatedEntities(Long assetExtractionIdToDelete) {

        assetTextUnitToTMTextUnitRepository.deleteByAssetExtractionId(assetExtractionIdToDelete);
        assetTextUnitRepository.deleteByAssetExtractionId(assetExtractionIdToDelete);
        assetExtractionRepository.delete(assetExtractionIdToDelete);
    }

}
