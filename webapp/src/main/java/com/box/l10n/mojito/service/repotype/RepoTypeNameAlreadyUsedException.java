package com.box.l10n.mojito.service.repotype;

/**
 * Thrown when creating or renaming a {@link com.box.l10n.mojito.entity.RepoType} would violate the
 * unique name constraint.
 *
 * <p>Mapped by the REST layer to HTTP 409 Conflict.
 */
public class RepoTypeNameAlreadyUsedException extends Exception {

  /**
   * @param message human-readable reason, typically including the conflicting name
   */
  public RepoTypeNameAlreadyUsedException(String message) {
    super(message);
  }

  /**
   * @param cause underlying failure (e.g. DB unique constraint violation)
   */
  public RepoTypeNameAlreadyUsedException(Throwable cause) {
    super(cause);
  }
}
