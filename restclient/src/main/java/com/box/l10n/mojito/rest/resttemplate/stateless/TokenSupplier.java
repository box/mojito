package com.box.l10n.mojito.rest.resttemplate.stateless;

public interface TokenSupplier {

  /**
   * Returns an access token string suitable for use in an HTTP Authorization header.
   * Implementations may perform interactive flows (e.g., device code) on first use and should
   * leverage local caching for subsequent calls.
   */
  String getAccessToken();
}
