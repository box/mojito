package com.box.l10n.mojito.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class HeaderPreAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
  protected HeaderSecurityConfig headerSecurityConfig;

  static Logger logger = LoggerFactory.getLogger(HeaderPreAuthFilter.class);

  public HeaderPreAuthFilter(HeaderSecurityConfig headerSecurityConfig) {
    this.headerSecurityConfig = headerSecurityConfig;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest request) {
    String forwardedUser = request.getHeader(headerSecurityConfig.userIdentifyingHeader);
    if (forwardedUser != null && !forwardedUser.contains(headerSecurityConfig.servicePrefix)) {
      logger.debug("Forwarded user: {}", forwardedUser);
      if (!forwardedUser.isEmpty()) {
        return forwardedUser;
      }
    }

    String forwardedServiceSpiffe =
        request.getHeader(headerSecurityConfig.serviceIdentifyingHeader);
    if (forwardedServiceSpiffe != null) {
      logger.debug("Forwarded service: {}", forwardedServiceSpiffe);
      if (!forwardedServiceSpiffe.isEmpty()) {
        return forwardedServiceSpiffe;
      }
    }

    logger.info(
        "No auth principal could be found using headers '{}' and '{}'",
        headerSecurityConfig.userIdentifyingHeader,
        headerSecurityConfig.serviceIdentifyingHeader);

    return null;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
    return getPreAuthenticatedPrincipal(request);
  }
}
