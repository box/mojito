package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks that if there is ellipsis character '…' in the source and target
 * content.
 *
 * @author jyi
 */
public class EllipsisIntegrityChecker implements TextUnitIntegrityChecker {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(EllipsisIntegrityChecker.class);

    @Override
    public void check(String sourceContent, String targetContent) throws EllipsisIntegrityCheckerException {

        if (sourceContent.contains("…") && targetContent.contains("...")) {
            throw new EllipsisIntegrityCheckerException("Ellipsis in source and target are different");
        }
    }
}
