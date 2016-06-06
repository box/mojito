package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * The document integrity checkers will run some checks to make sure
 * a document is valid.
 * A {@link IntegrityCheckException} will be thrown if a test fails.
 *
 * @author aloison
 */
public interface DocumentIntegrityChecker {

    public boolean supportsExtension(String documentExtension);

    public void check(String content) throws IntegrityCheckException;
}
