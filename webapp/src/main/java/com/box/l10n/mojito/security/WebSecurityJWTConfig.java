package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserService;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

@EnableWebSecurity
@Configuration
@ConditionalOnAuthTypes(anyOf = SecurityConfig.AuthenticationType.JWT)
public class WebSecurityJWTConfig {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(WebSecurityJWTConfig.class);

  UserService userService;

  SecurityConfig securityConfig;

  @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
  String issuerUri;

  @Value("${spring.security.oauth2.resourceserver.jwt.audience:}")
  String audience;

  private final DefaultBearerTokenResolver defaultBearerTokenResolver;

  public WebSecurityJWTConfig(UserService userService, SecurityConfig securityConfig) {
    this.userService = userService;
    this.securityConfig = securityConfig;
    this.defaultBearerTokenResolver = new DefaultBearerTokenResolver();
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

    http.securityMatcher(req -> StringUtils.hasText(resolveAccessToken(req)));

    applyStatelessSharedConfig(http);

    http.oauth2ResourceServer(
        oauth ->
            oauth
                .bearerTokenResolver(this::resolveAccessToken)
                .jwt(
                    jwtConfigurer -> {
                      jwtConfigurer.jwtAuthenticationConverter(
                          jwt -> {
                            logger.debug("Getting info from the JWT");
                            User user = ensureUserFromJwt(jwt);

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
                "/ai-translate",
                "/settings/**"));

    // forwarding was for the old implementation and is not needed anymore so
    // hardcoded to false.
    spaSpecificPermitAll.addAll(WebSecurityConfig.getHealthcheckPatterns(false));
    WebSecurityConfig.setAuthorizationRequests(http, spaSpecificPermitAll);
  }

  private User ensureUserFromJwt(Jwt jwt) {
    String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
    IdentityProviderType providerType = resolveProviderType(issuer);

    String email = jwt.getClaimAsString("email");
    String upn = jwt.getClaimAsString("preferred_username");
    String oid = jwt.getClaimAsString("oid");
    String sub = jwt.getSubject();
    String name = jwt.getClaimAsString("name");
    String given = jwt.getClaimAsString("given_name");
    String family = jwt.getClaimAsString("family_name");
    String commonName = jwt.getClaimAsString("common_name");

    String username =
        switch (providerType) {
          case AZURE_AD -> firstNonBlank(localPart(upn), localPart(email), sub, oid);
          case CLOUDFLARE -> firstNonBlank(localPart(email), sub, oid, commonName, localPart(upn));
          case AUTO -> firstNonBlank(localPart(email), localPart(upn), sub, oid);
        };

    if (!StringUtils.hasText(username)) {
      username = firstNonBlank(sub, oid, jwt.getTokenValue());
    }

    if (!StringUtils.hasText(name)) {
      String built = ((given != null ? given : "") + " " + (family != null ? family : "")).trim();
      name = StringUtils.hasText(built) ? built : username;
    }

    return userService.getOrCreateOrUpdateBasicUser(username, given, family, name);
  }

  private String resolveAccessToken(HttpServletRequest request) {
    if (request == null) {
      return null;
    }

    CustomTokenHeader customTokenHeader = getCustomTokenHeader();
    if (customTokenHeader == null) {
      String token = defaultBearerTokenResolver.resolve(request);
      if (StringUtils.hasText(token)) {
        return token;
      }
      return null;
    }

    String headerValue = request.getHeader(customTokenHeader.name());
    if (!StringUtils.hasText(headerValue)) {
      return null;
    }

    String candidate = headerValue.trim();
    if (!StringUtils.hasText(candidate)) {
      return null;
    }

    String prefix = customTokenHeader.prefix();
    if (!StringUtils.hasText(prefix)) {
      return candidate;
    }

    if (candidate.startsWith(prefix)) {
      String stripped = candidate.substring(prefix.length()).trim();
      return StringUtils.hasText(stripped) ? stripped : null;
    }

    return null;
  }

  private CustomTokenHeader getCustomTokenHeader() {
    SecurityConfig.Jwt jwtConfig = securityConfig.getJwt();
    if (jwtConfig == null) {
      return null;
    }

    String headerName = jwtConfig.getTokenHeaderName();
    if (!StringUtils.hasText(headerName)) {
      return null;
    }

    String name = headerName.trim();
    if (!StringUtils.hasText(name)) {
      return null;
    }

    String prefix = jwtConfig.getTokenHeaderPrefix();
    if (StringUtils.hasText(prefix)) {
      prefix = prefix.trim();
      prefix = StringUtils.hasText(prefix) ? prefix : null;
    } else {
      prefix = null;
    }

    return new CustomTokenHeader(name, prefix);
  }

  private IdentityProviderType resolveProviderType(String issuer) {
    if (issuer == null) {
      return IdentityProviderType.AUTO;
    }
    if (issuer.contains("login.microsoftonline.com")) {
      return IdentityProviderType.AZURE_AD;
    }
    if (issuer.contains("cloudflareaccess.com")) {
      return IdentityProviderType.CLOUDFLARE;
    }
    return IdentityProviderType.AUTO;
  }

  private String firstNonBlank(String... values) {
    if (values == null) {
      return null;
    }
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return null;
  }

  private String localPart(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    int at = value.indexOf('@');
    if (at > 0) {
      return value.substring(0, at).trim();
    }
    return value.trim();
  }

  private enum IdentityProviderType {
    AUTO,
    AZURE_AD,
    CLOUDFLARE
  }

  private record CustomTokenHeader(String name, String prefix) {}

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
