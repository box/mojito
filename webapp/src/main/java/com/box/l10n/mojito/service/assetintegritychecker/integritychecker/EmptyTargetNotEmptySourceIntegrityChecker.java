package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Check that the target must not be empty when the source is not empty.
 */
public class EmptyTargetNotEmptySourceIntegrityChecker extends AbstractTextUnitIntegrityChecker {

    @Override
    public void check(String sourceContent, String targetContent) throws IntegrityCheckException {
        if (targetContent.isEmpty() && !sourceContent.isEmpty()) {
            throw new EmptyTargetNotEmptySourceIntegrityCheckerException("Empty target is rejected when the source is not empty");
        }
    }
}
