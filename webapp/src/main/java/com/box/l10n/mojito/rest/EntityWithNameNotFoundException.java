package com.box.l10n.mojito.rest;

import com.ibm.icu.text.MessageFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** @author garion */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EntityWithNameNotFoundException extends Exception {

  public EntityWithNameNotFoundException(String entity, String name) {
    super(getMessage(entity, name));
  }

  static String getMessage(String entity, String name) {
    return MessageFormat.format("{0} with id: {1} not found", entity, name);
  }
}
