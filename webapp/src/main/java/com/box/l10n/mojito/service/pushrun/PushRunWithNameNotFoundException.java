package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.rest.EntityWithNameNotFoundException;

public class PushRunWithNameNotFoundException extends EntityWithNameNotFoundException {
    public PushRunWithNameNotFoundException(String pushRunName) {
        super("PushRun", pushRunName);
    }
}