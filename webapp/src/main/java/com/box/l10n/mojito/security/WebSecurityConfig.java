package com.box.l10n.mojito.security;

import com.box.l10n.mojito.ActuatorHealthLegacyConfig;
import com.box.l10n.mojito.service.security.user.UserService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * @author wyau
 */
@EnableWebSecurity
// TOOD(spring2) we don't use method level security, do we? remove?
@EnableGlobalMethodSecurity(securedEnabled = true, mode = AdviceMode.ASPECTJ)
@Configuration
@ConditionalOnAuthTypes(
    anyOf = {
      SecurityConfig.AuthenticationType.AD,
      SecurityConfig.AuthenticationType.DATABASE,
      SecurityConfig.AuthenticationType.LDAP,
      SecurityConfig.AuthenticationType.OAUTH2
    },
    matchIfMissing = true)
public class WebSecurityConfig {

  static final String LOGIN_PAGE = "/login";

  /** logger */
  static Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

  @Autowired SecurityConfig securityConfig;

  @Autowired LdapConfig ldapConfig;

  @Autowired ActiveDirectoryConfig activeDirectoryConfig;

  @Autowired AuthenticationConfiguration authenticationConfiguration;

  @Autowired ActuatorHealthLegacyConfig actuatorHealthLegacyConfig;

  @Autowired UserDetailsContextMapperImpl userDetailsContextMapperImpl;

  @Autowired UserService userService;

  @Autowired UserDetailsServiceImpl userDetailsService;

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    for (SecurityConfig.AuthenticationType authenticationType :
        securityConfig.getAuthenticationType()) {
      switch (authenticationType) {
        case DATABASE:
          configureDatabase(auth);
          break;
        case LDAP:
          configureLdap(auth);
          break;
        case AD:
          configureActiveDirectory(auth);
          break;
      }
    }
  }

  void configureActiveDirectory(AuthenticationManagerBuilder auth) throws Exception {
    logger.debug("Configuring in active directory authentication");
    ActiveDirectoryAuthenticationProviderConfigurer<AuthenticationManagerBuilder>
        activeDirectoryManagerConfigurer = new ActiveDirectoryAuthenticationProviderConfigurer<>();

    activeDirectoryManagerConfigurer.domain(activeDirectoryConfig.getDomain());
    activeDirectoryManagerConfigurer.url(activeDirectoryConfig.getUrl());
    activeDirectoryManagerConfigurer.rootDn(activeDirectoryConfig.getRootDn());
    activeDirectoryManagerConfigurer.userServiceDetailMapper(userDetailsContextMapperImpl);

    auth.apply(activeDirectoryManagerConfigurer);
  }

  void configureDatabase(AuthenticationManagerBuilder auth) throws Exception {
    logger.debug("Configuring in database authentication");
    auth.userDetailsService(userDetailsService).passwordEncoder(new BCryptPasswordEncoder());
  }

  void configureLdap(AuthenticationManagerBuilder auth) throws Exception {
    logger.debug("Configuring ldap server");
    LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>.ContextSourceBuilder
        contextSourceBuilder =
            auth.ldapAuthentication()
                .userSearchBase(ldapConfig.getUserSearchBase())
                .userSearchFilter(ldapConfig.getUserSearchFilter())
                .groupSearchBase(ldapConfig.getGroupSearchBase())
                .groupSearchFilter(ldapConfig.getGroupSearchFilter())
                .groupRoleAttribute(ldapConfig.getGroupRoleAttribute())
                .userDetailsContextMapper(userDetailsContextMapperImpl)
                .contextSource();

    if (ldapConfig.getPort() != null) {
      contextSourceBuilder.port(ldapConfig.getPort());
    }

    contextSourceBuilder
        .root(ldapConfig.getRoot())
        .url(ldapConfig.getUrl())
        .managerDn(ldapConfig.getManagerDn())
        .managerPassword(ldapConfig.getManagerPassword())
        .ldif(ldapConfig.getLdif());
  }

  static void setAuthorizationRequests(HttpSecurity http, List<String> extraPermitAllPatterns)
      throws Exception {

    var permitMatchers =
        new ArrayList<>(
            List.of(
                "/intl/*",
                "/img/*",
                "/login/**",
                "/favicon.ico",
                "/fonts/*",
                "/cli/**",
                "/js/**",
                "/css/**",
                "/error"));
    if (extraPermitAllPatterns != null) {
      permitMatchers.addAll(extraPermitAllPatterns);
    }

    // matcher order matters - "everything else" mapping must be last
    http.authorizeRequests(
        authorizeRequests ->
            authorizeRequests
                .requestMatchers(permitMatchers.toArray(String[]::new))
                .permitAll()
                // allow deep link creation and retrieval
                .requestMatchers(HttpMethod.GET, "/api/clobstorage", "/api/clobstorage/**")
                .authenticated()
                .requestMatchers(HttpMethod.POST, "/api/clobstorage", "/api/clobstorage/**")
                .authenticated()
                // local access only for rotation management and logger config
                .requestMatchers("/actuator/shutdown", "/actuator/loggers/**", "/api/rotation")
                .hasIpAddress("127.0.0.1")
                // Everyone can access the session endpoint
                .requestMatchers("/api/users/session", "/api/users/me", "/api/users/pw")
                .authenticated()
                // user management is only allowed for ADMINs and PMs
                .requestMatchers("/api/users/**")
                .hasAnyRole("PM", "ADMIN")
                // Read-only access is OK for users
                .requestMatchers(HttpMethod.GET, "/api/textunits/**")
                .authenticated()
                // Searching is also OK for users
                .requestMatchers(HttpMethod.POST, "/api/textunits/search")
                .authenticated()
                // USERs are not allowed to change translations
                .requestMatchers("/api/textunits/**")
                .hasAnyRole("TRANSLATOR", "PM", "ADMIN")
                // Read-only is OK for everyone
                .requestMatchers(HttpMethod.GET, "/api/**")
                .authenticated()
                // Everyone can retrieve & upload images
                .requestMatchers(HttpMethod.GET, "/api/images/**")
                .authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/images/**")
                .authenticated()
                // Everyone can retrieve, upload and delete screenshots
                .requestMatchers(HttpMethod.GET, "/api/screenshots")
                .authenticated()
                .requestMatchers(HttpMethod.POST, "/api/screenshots")
                .authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/screenshots/**")
                .authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/screenshots/**")
                .authenticated()
                // However, all other methods require is PM and ADMIN only unless overwritten above
                .requestMatchers("/api/**")
                .hasAnyRole("PM", "ADMIN")
                // everything else must be authenticated
                .requestMatchers("/**")
                .authenticated());
  }

  @Bean
  @Order(3)
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {
    logger.debug("Configuring web security");

    // TODO should we just enable caching of static assets, this disabling cache control for
    // everything
    // https://docs.spring.io/spring-security/site/docs/current/reference/html5/#headers-cache-control
    http.headers().cacheControl().disable();

    // no csrf on rotation end point - they are accessible only locally
    http.csrf()
        .ignoringRequestMatchers("/actuator/shutdown", "/actuator/loggers/**", "/api/rotation");

    setAuthorizationRequests(
        http, getHealthcheckPatterns(actuatorHealthLegacyConfig.isForwarding()));

    logger.debug("For APIs, we don't redirect to login page. Instead we return a 401");
    http.exceptionHandling()
        .defaultAuthenticationEntryPointFor(
            new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/*"));

    if (securityConfig.getUnauthRedirectTo() != null) {
      logger.debug(
          "Redirect to: {} instead of login page on authorization exceptions",
          securityConfig.getUnauthRedirectTo());
      http.exceptionHandling()
          .defaultAuthenticationEntryPointFor(
              new LoginUrlAuthenticationEntryPoint(securityConfig.getUnauthRedirectTo()),
              new AntPathRequestMatcher("/*"));
    }

    for (SecurityConfig.AuthenticationType authenticationType :
        securityConfig.getAuthenticationType()) {
      switch (authenticationType) {
        case OAUTH2:
          logger.debug("Configure OAuth2");
          http.oauth2Login(
              oauth2Login -> {
                oauth2Login.loginPage(LOGIN_PAGE);

                oauth2Login.authorizationEndpoint(
                    authorizationEndpointConfig ->
                        authorizationEndpointConfig.baseUri(
                            LOGIN_PAGE
                                + OAuth2AuthorizationRequestRedirectFilter
                                    .DEFAULT_AUTHORIZATION_REQUEST_BASE_URI));

                oauth2Login.userInfoEndpoint(
                    userInfoEndpoint -> {
                      userInfoEndpoint.userService(
                          new UserDetailImplOAuth2UserService(securityConfig, userService));

                      userInfoEndpoint.oidcUserService(
                          new UserDetailImplOidcUserService(securityConfig, userService));
                    });
              });
          break;
        case AD:
        case LDAP:
        case DATABASE:
          logger.debug("Configure form login for DATABASE, AD or LDAP");
          http.formLogin(
              formLogin ->
                  formLogin
                      .loginPage(LOGIN_PAGE)
                      .successHandler(new ShowPageAuthenticationSuccessHandler()));
          break;
      }
    }

    return http.build();
  }

  /**
   * Returns health entry points.
   *
   * <p>By default it is only the actuator but potentially include the legacy entry point. {@link
   * com.box.l10n.mojito.rest.rotation.ActuatorHealthLegacyWS}
   *
   * @param forwarding
   * @return
   */
  static List<String> getHealthcheckPatterns(boolean forwarding) {
    List<String> patterns = new ArrayList<>();
    patterns.add("/actuator/health");

    if (forwarding) {
      patterns.add("/health");
    }
    return patterns;
  }
}
