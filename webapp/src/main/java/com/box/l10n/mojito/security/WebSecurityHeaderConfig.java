package com.box.l10n.mojito.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

@ConditionalOnAuthTypes(anyOf = SecurityConfig.AuthenticationType.HEADER)
@Configuration
class WebSecurityHeaderConfig {

  public static final String X_FORWARDED_USER = "x-forwarded-user";

  @Bean
  PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider(
      UserDetailsServiceCreatePartialImpl uds) {

    UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> wrapper =
        new UserDetailsByNameServiceWrapper<>(uds);

    PreAuthenticatedAuthenticationProvider p = new PreAuthenticatedAuthenticationProvider();
    p.setPreAuthenticatedUserDetailsService(wrapper);
    return p;
  }

  @Bean
  RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter(
      PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider) {
    RequestHeaderAuthenticationFilter f = new RequestHeaderAuthenticationFilter();
    f.setPrincipalRequestHeader(X_FORWARDED_USER);
    f.setCredentialsRequestHeader(X_FORWARDED_USER);
    f.setExceptionIfHeaderMissing(false);
    f.setAuthenticationManager(new ProviderManager(preAuthenticatedAuthenticationProvider));
    return f;
  }

  @Bean
  protected UserDetailsServiceCreatePartialImpl getUserDetailsServiceCreatePartial() {
    return new UserDetailsServiceCreatePartialImpl();
  }

  @Bean
  @Order(1)
  SecurityFilterChain headerPreAuthenticated(
      HttpSecurity http,
      RequestHeaderAuthenticationFilter headerFilter,
      PreAuthenticatedAuthenticationProvider preauthProvider)
      throws Exception {

    http.securityMatcher(req -> req.getHeader(X_FORWARDED_USER) != null);

    WebSecurityJWTConfig.applyStatelessSharedConfig(http);

    http.authenticationProvider(preauthProvider);
    http.addFilterBefore(headerFilter, BearerTokenAuthenticationFilter.class);

    return http.build();
  }
}
