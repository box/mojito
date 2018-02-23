package com.box.l10n.mojito.rest.textunit;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

/**
 *
 * @author jeanaurambault
 */
public class AssetTextUnitWithIdNotFoundException extends EntityWithIdNotFoundException {

    public AssetTextUnitWithIdNotFoundException(Long id) {
        super("AssetTextUnit", id);
    }

}
