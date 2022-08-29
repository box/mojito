package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.rest.EntityWithNameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PushRunWithNameNotFoundException extends EntityWithNameNotFoundException {
  public PushRunWithNameNotFoundException(String pushRunName) {
    super("PushRun", pushRunName);
  }
}
