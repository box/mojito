package com.box.l10n.mojito.service.assetExtraction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author aloison
 */
@Profile("!disablescheduling")
@Component
public class AssetExtractionCleanupScheduler {

    @Autowired
    AssetExtractionCleanupService assetExtractionCleanupService;

    /**
     * @see AssetExtractionCleanupService#cleanupOldAssetExtractions()
     * This function is triggered every 5 minutes (= 300,000 milliseconds).
     */
    @Scheduled(fixedDelay = 300000)
    private void scheduleCleanupOldAssetExtractions() {
        assetExtractionCleanupService.cleanupOldAssetExtractions();
    }
}
