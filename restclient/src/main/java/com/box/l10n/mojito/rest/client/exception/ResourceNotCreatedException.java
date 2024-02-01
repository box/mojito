package com.box.l10n.mojito.rest.client.exception;

/**
 * @author wyau
 */
public class ResourceNotCreatedException extends RestClientException {

  public ResourceNotCreatedException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceNotCreatedException(String message) {
    super(message);
  }
}
