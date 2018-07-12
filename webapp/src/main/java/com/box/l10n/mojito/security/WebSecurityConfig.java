package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.Http401AuthenticationEntryPoint;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;

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
        http.authorizeRequests()
                // TODO (move img to images)
                // TODO (move intl to js/intl)
                .antMatchers("/intl/*", "/img/*", "/fonts/*", "/webjars/**", "/cli/**", "/health").permitAll()
                .regexMatchers("/login\\?.*").permitAll()
                .anyRequest().fullyAuthenticated()
                .and()
                .formLogin()
                .loginPage("/login").permitAll()
                .successHandler(new ShowPageAuthenticationSuccessHandler())
                .and()
                .logout().logoutSuccessUrl("/login?logout").permitAll();

        http.exceptionHandling().defaultAuthenticationEntryPointFor(new Http401AuthenticationEntryPoint("API_UNAUTHORIZED"), new AntPathRequestMatcher("/api/*"));
        http.exceptionHandling().defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login"), new AntPathRequestMatcher("/*"));
    }

    @Bean
    protected UserDetailsServiceImpl getUserDetailsService() {
        return new UserDetailsServiceImpl();
    }
}
