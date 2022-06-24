package com.box.l10n.mojito.rest.commit;

import com.box.l10n.mojito.rest.EntityWithNameNotFoundException;

/**
 * @author garion
 */
public class CommitWithNameNotFoundException extends EntityWithNameNotFoundException {
    public CommitWithNameNotFoundException(String commitName) {
        super("Commit", commitName);
    }
}
