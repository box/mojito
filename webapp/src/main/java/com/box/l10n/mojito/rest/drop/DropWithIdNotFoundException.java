package com.box.l10n.mojito.rest.drop;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

/**
 * @author jyi
 */
public class DropWithIdNotFoundException extends EntityWithIdNotFoundException {

  public DropWithIdNotFoundException(Long id) {
    super("Drop", id);
  }
}
