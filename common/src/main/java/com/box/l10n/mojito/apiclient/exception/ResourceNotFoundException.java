package com.box.l10n.mojito.apiclient.exception;

/**
 * @author wyau
 */
public class ResourceNotFoundException extends RestClientException {

  public ResourceNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
