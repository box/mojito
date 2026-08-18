package com.box.l10n.mojito.rest.repotype;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

/**
 * Thrown when a {@link com.box.l10n.mojito.entity.RepoType} cannot be found by id.
 *
 * <p>Inherited {@link org.springframework.web.bind.annotation.ResponseStatus} maps this to HTTP
 * 404 Not Found.
 */
public class RepoTypeWithIdNotFoundException extends EntityWithIdNotFoundException {

  /**
   * @param id requested repo type id that does not exist
   */
  public RepoTypeWithIdNotFoundException(Long id) {
    super("RepoType", id);
  }
}
