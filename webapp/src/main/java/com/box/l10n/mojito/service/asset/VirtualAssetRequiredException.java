package com.box.l10n.mojito.service.asset;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author jeanaurambault
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class VirtualAssetRequiredException extends VirtualAssetBadRequestException {

  public VirtualAssetRequiredException(String message) {
    super(message);
  }
}
