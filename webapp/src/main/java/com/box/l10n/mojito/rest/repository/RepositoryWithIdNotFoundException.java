package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.rest.EntityWithIdNotFoundException;

/**
 *
 * @author jeanaurambault
 */
public class RepositoryWithIdNotFoundException extends EntityWithIdNotFoundException {
    
    public RepositoryWithIdNotFoundException(Long id) {
        super("Repository", id);
    }
    
}
