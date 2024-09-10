package com.box.l10n.mojito.service.assetcontent;

public class ContentNotFoundException extends IllegalArgumentException {
  public ContentNotFoundException(String message) {
    super(message);
  }
}
