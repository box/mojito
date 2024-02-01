package com.box.l10n.mojito.rest.client.exception;

/**
 * @author jyi
 */
public class ResourceNotUpdatedException extends RestClientException {

  public ResourceNotUpdatedException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceNotUpdatedException(String message) {
    super(message);
  }
}
