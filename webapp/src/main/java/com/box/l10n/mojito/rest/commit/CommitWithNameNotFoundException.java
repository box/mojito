package com.box.l10n.mojito.rest.commit;

import com.box.l10n.mojito.rest.EntityWithNameNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** @author garion */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CommitWithNameNotFoundException extends EntityWithNameNotFoundException {
  public CommitWithNameNotFoundException(String commitName) {
    super("Commit", commitName);
  }
}
