package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.security.Http401AuthenticationEntryPoint;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.UserInfoTokenServices;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.filter.OAuth2ClientContextFilter;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.servlet.Filter;

/**
 * @author wyau
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true, mode = AdviceMode.ASPECTJ)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

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
    UserDetailsContextMapperImpl userDetailsContextMapperImpl;

    @Autowired
    OAuth2ClientContext oauth2ClientContext;

    @Value("${l10n.security.oauth2.enabled:false}")
    boolean oauth2Enabled = false;

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

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        logger.debug("Configuring web security");

        http.headers().cacheControl().disable();

        http.csrf().ignoringAntMatchers("/shutdown", "/api/rotation");

        http.authorizeRequests()
                .antMatchers("/intl/*", "/img/*", "/fonts/*", "/login/**", "/webjars/**", "/cli/**", "/health").permitAll()
                .antMatchers("/shutdown", "/api/rotation").hasIpAddress("127.0.0.1").anyRequest().permitAll()
                .anyRequest().fullyAuthenticated()
                .and()
                .formLogin()
                .loginPage("/login")
                .successHandler(new ShowPageAuthenticationSuccessHandler())
                .and()
                .logout().logoutSuccessUrl("/login?logout").permitAll();

        if (oauth2Enabled) {
            http.addFilterBefore(oauthFilter(), BasicAuthenticationFilter.class);
        }

        http.exceptionHandling().defaultAuthenticationEntryPointFor(new Http401AuthenticationEntryPoint("API_UNAUTHORIZED"), new AntPathRequestMatcher("/api/*"));
        http.exceptionHandling().defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint(oauth2Enabled ? "/login/oauth" : "/login"), new AntPathRequestMatcher("/*"));
    }

    private Filter oauthFilter() {
        logger.debug("Setup SSO filter for oauth");
        OAuth2ClientAuthenticationProcessingFilter oauth2Filter = new OAuth2ClientAuthenticationProcessingFilter(
                "/login/oauth");
        OAuth2RestTemplate auth2RestTemplate = new OAuth2RestTemplate(oauth2(), oauth2ClientContext);

        oauth2Filter.setRestTemplate(auth2RestTemplate);
        UserInfoTokenServices tokenServices = new UserInfoTokenServices(
                oauth2Resource().getUserInfoUri(),
                oauth2().getClientId());
        tokenServices.setRestTemplate(auth2RestTemplate);

        oauth2Filter.setTokenServices(new MyUserInfoTokenServices(
                oauth2Resource().getUserInfoUri(),
                oauth2().getClientId()));

        return oauth2Filter;
    }

    @Bean
    @ConditionalOnProperty(value = "l10n.security.oauth2.enabled", havingValue = "true")
    @ConfigurationProperties("l10n.security.oauth2.client")
    public AuthorizationCodeResourceDetails oauth2() {
        return new AuthorizationCodeResourceDetails();
    }

    @Bean
    @ConditionalOnProperty(value = "l10n.security.oauth2.enabled", havingValue = "true")
    @ConfigurationProperties("l10n.security.oauth2.resource")
    public ResourceServerProperties oauth2Resource() {
        return new ResourceServerProperties();
    }

    @Bean
    @ConditionalOnProperty(value = "l10n.security.oauth2.enabled", havingValue = "true")
    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(filter);
        registration.setOrder(-100);
        return registration;
    }

    @Bean
    protected UserDetailsServiceImpl getUserDetailsService() {
        return new UserDetailsServiceImpl();
    }

}
