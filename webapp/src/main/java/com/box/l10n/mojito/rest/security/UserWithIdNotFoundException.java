package com.box.l10n.mojito.rest.security;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

/** @author jeanaurambault */
public class UserWithIdNotFoundException extends EntityWithIdNotFoundException {

  public UserWithIdNotFoundException(Long id) {
    super("User", id);
  }
}
