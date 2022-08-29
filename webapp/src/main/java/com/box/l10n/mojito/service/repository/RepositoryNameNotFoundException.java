package com.box.l10n.mojito.service.repository;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** @author garion */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RepositoryNameNotFoundException extends IllegalArgumentException {
  public RepositoryNameNotFoundException(String message) {
    super(message);
  }
}
