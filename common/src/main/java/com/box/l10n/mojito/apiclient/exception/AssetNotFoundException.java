package com.box.l10n.mojito.apiclient.exception;

/**
 * @author wyau
 */
public class AssetNotFoundException extends ResourceNotFoundException {
  public AssetNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  public AssetNotFoundException(String message) {
    super(message);
  }
}
