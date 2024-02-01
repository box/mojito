package com.box.l10n.mojito.service.asset;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author garion
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AssetPathNotFoundException extends IllegalArgumentException {
  public AssetPathNotFoundException(String message) {
    super(message);
  }
}
