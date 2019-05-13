package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import java.util.Set;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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

        TMTextUnit tmTextUnit = tmTextUnitRepository.findById(tmTextUnitId).orElse(null);
        Asset asset = tmTextUnit.getAsset();

        Set<TextUnitIntegrityChecker> textUnitCheckers = integrityCheckerFactory.getTextUnitCheckers(asset);

        if (textUnitCheckers.isEmpty()) {
            logger.debug("No designated checker for this asset.  Nothing to do");
        } else {
            for (TextUnitIntegrityChecker textUnitChecker : textUnitCheckers) {
                textUnitChecker.check(tmTextUnit.getContent(), contentToCheck);
            }
        }
    }
}
