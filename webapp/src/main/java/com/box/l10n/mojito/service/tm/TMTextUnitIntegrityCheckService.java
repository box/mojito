package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wyau
 */
@Component
public class TMTextUnitIntegrityCheckService {
    /**
     * logger
     */
    static Logger logger = getLogger(TMTextUnitIntegrityCheckService.class);

    @Autowired
    IntegrityCheckerFactory integrityCheckerFactory;

    @Autowired
    AssetRepository assetRepository;

    @Autowired
    TMTextUnitRepository tmTextUnitRepository;

    /**
     * Checks the integrity of the content given the {@link com.box.l10n.mojito.entity.TMTextUnit#id}
     *
     * @throws IntegrityCheckException
     */
    public void checkTMTextUnitIntegrity(Long tmTextUnitId, String contentToCheck) throws IntegrityCheckException {
        logger.debug("Checking Integrity of the TMTextUnit");

        TMTextUnit tmTextUnit = tmTextUnitRepository.findOne(tmTextUnitId);
        Asset asset = tmTextUnit.getAsset();

        TextUnitIntegrityChecker textUnitChecker = integrityCheckerFactory.getTextUnitChecker(asset);

        if (textUnitChecker != null) {
            textUnitChecker.check(tmTextUnit.getContent(), contentToCheck);
        } else {
            logger.debug("No designated checker for this asset.  Nothing to do");
        }
    }
}
