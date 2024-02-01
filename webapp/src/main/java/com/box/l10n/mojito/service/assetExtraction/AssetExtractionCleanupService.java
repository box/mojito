package com.box.l10n.mojito.service.assetExtraction;

import com.box.l10n.mojito.entity.AssetExtraction;
import com.box.l10n.mojito.entity.AssetTextUnit;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetTextUnit.AssetTextUnitRepository;
import com.box.l10n.mojito.service.assetcontent.AssetContentRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * @author aloison
 */
@Service
public class AssetExtractionCleanupService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(AssetExtractionCleanupService.class);

  @Autowired AssetRepository assetRepository;

  @Autowired AssetExtractionRepository assetExtractionRepository;

  @Autowired AssetTextUnitRepository assetTextUnitRepository;

  @Autowired AssetTextUnitToTMTextUnitRepository assetTextUnitToTMTextUnitRepository;

  @Autowired AssetContentRepository assetContentRepository;

  /**
   * Removes all {@link AssetExtraction}s and {@link AssetTextUnit}s that are no longer useful. This
   * means everything refering to an extraction that is finished and older than the last successful
   * one.
   */
  public void cleanupOldAssetExtractions() {
    logger.debug("cleanupOldAssetExtractions");

    List<Long> assetExtractionIdsToDelete;

    do {
      // Fetching 5 by 5 to avoid locking too many rows.
      // It is also useful to distribute the load across multiple instances.
      PageRequest pageable = PageRequest.of(0, 5);
      assetExtractionIdsToDelete =
          assetExtractionRepository.findFinishedAndOldAssetExtractions(pageable);

      for (Long assetExtractionIdToDelete : assetExtractionIdsToDelete) {
        deleteAssetExtractionAndRelatedEntities(assetExtractionIdToDelete);
      }
    } while (!assetExtractionIdsToDelete.isEmpty());
  }

  /**
   * Deletes all asset extractions as well as the entities that have a relationship with the given
   * entity.
   *
   * @param assetExtractionIdToDelete {@link AssetExtraction#id} to be deleted
   */
  private void deleteAssetExtractionAndRelatedEntities(Long assetExtractionIdToDelete) {
    logger.debug(
        "deleteAssetExtractionAndRelatedEntities, assetExtractionId: {}",
        assetExtractionIdToDelete);
    int numberMappingsDeleted =
        assetTextUnitToTMTextUnitRepository.deleteByAssetExtractionId(assetExtractionIdToDelete);
    int numberAssetTextUnitsDeleted =
        assetTextUnitRepository.deleteByAssetExtractionId(assetExtractionIdToDelete);
    int numberAssetContentDeleted = assetContentRepository.deleteByAssetExtractionsIdIsNull();
    assetExtractionRepository.deleteById(assetExtractionIdToDelete);
    logger.debug(
        "For assetExtractionId: {}, deleted {} mappings, {} asset text units, asset content: {}",
        assetExtractionIdToDelete,
        numberMappingsDeleted,
        numberAssetTextUnitsDeleted,
        numberAssetContentDeleted);
  }
}
