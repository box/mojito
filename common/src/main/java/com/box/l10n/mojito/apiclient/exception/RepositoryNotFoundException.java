package com.box.l10n.mojito.apiclient.exception;

/**
 * @author wyau
 */
public class RepositoryNotFoundException extends ResourceNotFoundException {
  public RepositoryNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public RepositoryNotFoundException(String message) {
    super(message);
  }
}
