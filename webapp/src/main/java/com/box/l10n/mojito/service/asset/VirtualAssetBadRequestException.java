package com.box.l10n.mojito.service.asset;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author jeanaurambault
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VirtualAssetBadRequestException extends Exception {

  public VirtualAssetBadRequestException(String message) {
    super(message);
  }
}
