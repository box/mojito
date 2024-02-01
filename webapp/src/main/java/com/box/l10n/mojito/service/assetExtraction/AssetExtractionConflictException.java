package com.box.l10n.mojito.service.assetExtraction;

/**
 * @author jyi
 */
public class AssetExtractionConflictException extends RuntimeException {

  public AssetExtractionConflictException(String message) {
    super(message);
  }

  public AssetExtractionConflictException(String message, Throwable cause) {
    super(message, cause);
  }
}
