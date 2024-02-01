package com.box.l10n.mojito.rest.asset;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

/**
 * @author jeanaurambault
 */
public class AssetWithIdNotFoundException extends EntityWithIdNotFoundException {

  public AssetWithIdNotFoundException(Long id) {
    super("AssetTextUnit", id);
  }
}
