package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * @author jaurambault
 */
public class URLIntegrityCheckerException extends RegexCheckerException {

  public URLIntegrityCheckerException(RegexCheckerException rce) {
    super(rce.getMessage());
  }
}
