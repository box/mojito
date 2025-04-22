package com.box.l10n.mojito.service.evolve;

public class EvolveSyncException extends RuntimeException {
  public EvolveSyncException(String message) {
    super(message);
  }

  public EvolveSyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
