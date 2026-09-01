package com.box.l10n.mojito.service.repotype;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a {@link com.box.l10n.mojito.entity.RepoType} create or update is rejected because a
 * field is missing or too long (including a null checker or required checker fields).
 *
 * <p>{@link ResponseStatus} maps this to HTTP 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RepoTypeInvalidException extends RuntimeException {

  /**
   * @param message human-readable reason (e.g. name is required)
   */
  public RepoTypeInvalidException(String message) {
    super(message);
  }
}
