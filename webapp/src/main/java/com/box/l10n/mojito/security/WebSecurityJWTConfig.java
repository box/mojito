package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserService;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@EnableWebSecurity
@Configuration
@ConditionalOnAuthTypes(anyOf = SecurityConfig.AuthenticationType.JWT)
public class WebSecurityJWTConfig {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(WebSecurityJWTConfig.class);

  UserService userService;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  String issuerUri;

  @Value("${spring.security.oauth2.resourceserver.jwt.audience:}")
  String audience;

  public WebSecurityJWTConfig(UserService userService) {
    this.userService = userService;
  }

  @PostConstruct
  public void validateStatelessConfig() {
    if (!StringUtils.hasText(issuerUri)) {
      throw new IllegalStateException(
          "Stateless mode is enabled but 'spring.security.oauth2.resourceserver.jwt.issuer-uri' is not set");
    }
    if (!StringUtils.hasText(audience)) {
      throw new IllegalStateException(
          "Stateless mode is enabled but 'spring.security.oauth2.resourceserver.jwt.audience' is not set");
    }
  }

  @Bean
  @Order(2)
  SecurityFilterChain security(HttpSecurity http) throws Exception {

    http.securityMatcher(
        req -> {
          String h = req.getHeader("Authorization");
          return h != null && h.startsWith("Bearer ");
        });

    applyStatelessSharedConfig(http);

    http.oauth2ResourceServer(
        oauth ->
            oauth.jwt(
                jwtConfigurer -> {
                  jwtConfigurer.jwtAuthenticationConverter(
                      jwt -> {
                        logger.debug("Getting info from the JWT");
                        String oid = jwt.getClaimAsString("oid");
                        String upn = jwt.getClaimAsString("preferred_username");
                        String name = jwt.getClaimAsString("name");
                        String given = jwt.getClaimAsString("given_name");
                        String family = jwt.getClaimAsString("family_name");

                        String username =
                            (upn != null && upn.contains("@"))
                                ? upn.substring(0, upn.indexOf('@'))
                                : oid;
                        User user =
                            userService.getOrCreateOrUpdateBasicUser(username, given, family, name);

                        List<SimpleGrantedAuthority> authoritiesFromUser =
                            user.getAuthorities().stream()
                                .map(Authority::getAuthority)
                                .map(SimpleGrantedAuthority::new)
                                .toList();

                        logger.debug("Authorities from User: {}", authoritiesFromUser);
                        var userDetails = new UserDetailsImpl(user);
                        return new StatelessAuthenticationToken(
                            userDetails, authoritiesFromUser, jwt);
                      });
                }));

    return http.build();
  }

  public static void applyStatelessSharedConfig(HttpSecurity http) throws Exception {

    http.headers(h -> h.cacheControl(HeadersConfigurer.CacheControlConfig::disable))
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

    // Make sure all these URLs are also declared in ReactAppController.
    //
    // Stateful mode:
    //   Accessing these URLs triggers a 302 redirect to the authentication endpoint.
    //   After successful login, the SPA client loads and navigates to the intended page.
    //
    // Stateless mode:
    //   Without this allowlist, requests to these URLs return 401 immediately and the flow stops.
    //   By allowlisting them, the server returns 200 so the SPA can load and handle navigation.
    List<String> spaSpecificPermitAll =
        new ArrayList<>(
            List.of(
                "/",
                "/auth/callback",
                "/repositories",
                "/project-requests",
                "/workbench",
                "/branches",
                "/screenshots",
                "/screenshots-legacy",
                "/settings/**"));

    // forwarding was for the old implementation and is not needed anymore so
    // hardcoded to false.
    spaSpecificPermitAll.addAll(WebSecurityConfig.getHealthcheckPatterns(false));
    WebSecurityConfig.setAuthorizationRequests(http, spaSpecificPermitAll);
  }

  static class StatelessAuthenticationToken extends AbstractAuthenticationToken {

    UserDetailsImpl userDetails;

    public StatelessAuthenticationToken(
        UserDetailsImpl userDetails, List<SimpleGrantedAuthority> authorities, Jwt jwt) {
      super(authorities);
      this.userDetails = userDetails;
      this.setAuthenticated(true);
      this.setDetails(jwt);
    }

    @Override
    public Object getCredentials() {
      return null;
    }

    @Override
    public Object getPrincipal() {
      return userDetails;
    }
  }
}
