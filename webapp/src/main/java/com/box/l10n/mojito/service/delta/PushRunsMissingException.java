package com.box.l10n.mojito.service.delta;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** @author garion */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PushRunsMissingException extends Exception {
  public PushRunsMissingException(String message) {
    super(message);
  }
}
