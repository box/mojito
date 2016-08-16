package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 *
 * @author jaurambault
 */
public class CompositeFormatIntegrityCheckerException extends RegexCheckerException {
    
    public CompositeFormatIntegrityCheckerException(RegexCheckerException rce) {
        super(rce.getMessage());
    }
    
}
