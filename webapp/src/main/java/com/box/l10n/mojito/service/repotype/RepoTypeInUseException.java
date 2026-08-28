package com.box.l10n.mojito.service.repotype;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a repo type cannot be deleted because repositories still reference it. */
@ResponseStatus(HttpStatus.CONFLICT)
public class RepoTypeInUseException extends RuntimeException {

  public RepoTypeInUseException(String name) {
    super("RepoType with name [" + name + "] is assigned to one or more repositories");
  }
}
