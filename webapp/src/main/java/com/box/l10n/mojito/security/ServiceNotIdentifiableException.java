package com.box.l10n.mojito.security;

public class ServiceNotIdentifiableException extends RuntimeException {

  public ServiceNotIdentifiableException(String message) {
    super(message);
  }

  public ServiceNotIdentifiableException(String message, Throwable t) {
    super(message, t);
  }
}
