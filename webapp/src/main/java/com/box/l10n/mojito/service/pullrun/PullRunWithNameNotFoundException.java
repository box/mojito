package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.rest.EntityWithNameNotFoundException;

public class PullRunWithNameNotFoundException extends EntityWithNameNotFoundException {
  public PullRunWithNameNotFoundException(String pullRunName) {
    super("PullRun", pullRunName);
  }
}
