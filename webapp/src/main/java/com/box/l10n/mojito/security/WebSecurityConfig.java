package com.box.l10n.mojito.security;

import com.box.l10n.mojito.ActuatorHealthLegacyConfig;
import com.box.l10n.mojito.service.security.user.UserService;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.Filter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author wyau
 */
@EnableWebSecurity
//TOOD(spring2) we don't use method level security, do we? remove?
@EnableGlobalMethodSecurity(securedEnabled = true, mode = AdviceMode.ASPECTJ)
@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    static final String LOGIN_PAGE = "/login";
    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(WebSecurityConfig.class);

    @Autowired
    SecurityConfig securityConfig;

    @Autowired
    LdapConfig ldapConfig;

    @Autowired
    ActiveDirectoryConfig activeDirectoryConfig;

    @Autowired
    ActuatorHealthLegacyConfig actuatorHealthLegacyConfig;

    @Autowired
    UserDetailsContextMapperImpl userDetailsContextMapperImpl;

    @Autowired(required = false)
    @Qualifier("oauth2Filter")
    Filter oauth2Filter;

    @Autowired(required = false)
    RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter;

    @Autowired(required = false)
    PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider;

    @Autowired
    UserService userService;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        for (SecurityConfig.AuthenticationType authenticationType : securityConfig.getAuthenticationType()) {
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
                case HEADER:
                    configureHeaderAuth(auth);
                    break;
            }
        }
    }

    void configureActiveDirectory(AuthenticationManagerBuilder auth) throws Exception {
        logger.debug("Configuring in active directory authentication");
        ActiveDirectoryAuthenticationProviderConfigurer<AuthenticationManagerBuilder> activeDirectoryManagerConfigurer = new ActiveDirectoryAuthenticationProviderConfigurer<>();

        activeDirectoryManagerConfigurer.domain(activeDirectoryConfig.getDomain());
        activeDirectoryManagerConfigurer.url(activeDirectoryConfig.getUrl());
        activeDirectoryManagerConfigurer.rootDn(activeDirectoryConfig.getRootDn());
        activeDirectoryManagerConfigurer.userServiceDetailMapper(userDetailsContextMapperImpl);

        auth.apply(activeDirectoryManagerConfigurer);
    }

    void configureDatabase(AuthenticationManagerBuilder auth) throws Exception {
        logger.debug("Configuring in database authentication");
        auth.userDetailsService(getUserDetailsService()).passwordEncoder(new BCryptPasswordEncoder());
    }

    void configureLdap(AuthenticationManagerBuilder auth) throws Exception {
        logger.debug("Configuring ldap server");
        LdapAuthenticationProviderConfigurer<AuthenticationManagerBuilder>.ContextSourceBuilder contextSourceBuilder = auth.ldapAuthentication()
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

    void configureHeaderAuth(AuthenticationManagerBuilder auth) {
        Preconditions.checkNotNull(preAuthenticatedAuthenticationProvider, "The preAuthenticatedAuthenticationProvider must be configured");
        logger.debug("Configuring in pre authentication");
        auth.authenticationProvider(preAuthenticatedAuthenticationProvider);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.debug("Configuring web security");

        // TODO should we just enable caching of static assets, this disabling cache control for everything
        // https://docs.spring.io/spring-security/site/docs/current/reference/html5/#headers-cache-control
        http.headers().cacheControl().disable();

        // no csrf on rotation end point - they are accessible only locally
        http.csrf().ignoringAntMatchers("/actuator/shutdown", "/api/rotation");

        // matcher order matters - "everything else" mapping must be last
        http.authorizeRequests(authorizeRequests -> authorizeRequests.
                antMatchers("/intl/*", "/img/*", "/login/**", "/favicon.ico",
                        "/fonts/*", "/cli/**", "/js/**", "/css/**").permitAll(). // always accessible to serve the frontend
                antMatchers(getHeathcheckPatterns()).permitAll(). // allow health entry points
                antMatchers("/actuator/shutdown", "/api/rotation").hasIpAddress("127.0.0.1"). // local access only for rotation management
                antMatchers("/**").authenticated() // everything else must be authenticated
        );

        logger.debug("For APIs, we don't redirect to login page. Instead we return a 401");
        http.exceptionHandling().defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED), new AntPathRequestMatcher("/api/*"));

        if (securityConfig.getUnauthRedirectTo() != null) {
            logger.debug("Redirect to: {} instead of login page on authorization exceptions", securityConfig.getUnauthRedirectTo());
            http.exceptionHandling().defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(securityConfig.getUnauthRedirectTo()), new AntPathRequestMatcher("/*"));
        }

        for (SecurityConfig.AuthenticationType authenticationType : securityConfig.getAuthenticationType()) {
            switch (authenticationType) {
                case HEADER:
                    Preconditions.checkNotNull(requestHeaderAuthenticationFilter, "The requestHeaderAuthenticationFilter must be configured");
                    logger.debug("Add request header Auth filter");
                    requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager());
                    http.addFilterBefore(requestHeaderAuthenticationFilter, BasicAuthenticationFilter.class);
                    break;
                case OAUTH2:
                    logger.debug("Configure OAuth2");
                    http.oauth2Login(oauth2Login -> {
                        oauth2Login.loginPage(LOGIN_PAGE);

                        oauth2Login.authorizationEndpoint(authorizationEndpointConfig -> authorizationEndpointConfig.
                                baseUri(LOGIN_PAGE + OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI));

                        oauth2Login.userInfoEndpoint(userInfoEndpoint -> {
                            userInfoEndpoint.userService(new UserDetailImplOAuth2UserService(securityConfig, userService));
                        });
                    });
                    break;
                case AD:
                case LDAP:
                case DATABASE:
                    logger.debug("Configure form login for DATABASE, AD or LDAP");
                    http.formLogin(formLogin -> formLogin.
                            loginPage(LOGIN_PAGE).
                            successHandler(new ShowPageAuthenticationSuccessHandler())
                    );
                    break;
            }
        }
    }

    /**
     * Returns health entry points.
     *
     * By default it is only the actuator but potentially include the legacy entry point.
     * {@link com.box.l10n.mojito.rest.rotation.ActuatorHealthLegacyWS}
     * @return
     */
    String[] getHeathcheckPatterns() {
        List<String> patterns = new ArrayList<>();
        patterns.add("/actuator/health");
        if(actuatorHealthLegacyConfig.isForwarding()) {
            patterns.add("/health");
        }
        return patterns.toArray(new String[patterns.size()]);
    }

    @Primary
    @Bean
    protected UserDetailsServiceImpl getUserDetailsService() {
        return new UserDetailsServiceImpl();
    }

}
