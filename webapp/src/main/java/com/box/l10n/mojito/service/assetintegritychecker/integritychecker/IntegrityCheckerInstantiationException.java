package com.box.l10n.mojito.service.assetintegritychecker.integritychecker;

/**
 * Thrown if it is not possible to create an instance of a {@link DocumentIntegrityChecker} or
 * {@link TextUnitIntegrityChecker} via reflection
 *
 * @author aloison
 */
public class IntegrityCheckerInstantiationException extends RuntimeException {

  public IntegrityCheckerInstantiationException(String message, Throwable cause) {
    super(message, cause);
  }
}
