package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * @author jaurambault
 */
public class MessageFormatIntegrityCheckerException extends IntegrityCheckException {

  public MessageFormatIntegrityCheckerException(String message) {
    super(message);
  }

  public MessageFormatIntegrityCheckerException(String message, Throwable cause) {
    super(message, cause);
  }
}
