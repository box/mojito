package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.ProviderManagerBuilder;
import org.springframework.security.ldap.authentication.ad.ActiveDirectoryLdapAuthenticationProvider;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;

/**
 * Configures Active Directory {@link AuthenticationProvider} in the
 * {@link ProviderManagerBuilder}.
 *
 * @author jaurambault
 * @param <B> the {@link ProviderManagerBuilder} type that this is configuring.
 */
public class ActiveDirectoryAuthenticationProviderConfigurer<B extends ProviderManagerBuilder<B>>
        extends SecurityConfigurerAdapter<AuthenticationManager, B> {

    /**
     * logger
     */
    static Logger logger = LoggerFactory.getLogger(ActiveDirectoryAuthenticationProviderConfigurer.class);

    private String url;
    private String domain;
    private String rootDn;
    private UserDetailsContextMapper userDetailsContextMapper;

    @Override
    public void configure(B builder) throws Exception {
        builder.authenticationProvider(build());
    }

    ActiveDirectoryLdapAuthenticationProvider build() {
        ActiveDirectoryLdapAuthenticationProvider activeDirectoryLdapAuthenticationProvider;

        if (rootDn == null) {
            activeDirectoryLdapAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(domain, url);
        } else {
            activeDirectoryLdapAuthenticationProvider = new ActiveDirectoryLdapAuthenticationProvider(domain, url, rootDn);
        }

        if (userDetailsContextMapper != null) {
            activeDirectoryLdapAuthenticationProvider.setUserDetailsContextMapper(userDetailsContextMapper);
        }

        return activeDirectoryLdapAuthenticationProvider;
    }

    public ActiveDirectoryAuthenticationProviderConfigurer<B> url(String url) {
        this.url = url;
        return this;
    }

    public ActiveDirectoryAuthenticationProviderConfigurer<B> domain(String domain) {
        this.domain = domain;
        return this;
    }

    public ActiveDirectoryAuthenticationProviderConfigurer<B> rootDn(String rootDn) {
        this.rootDn = rootDn;
        return this;
    }

    public ActiveDirectoryAuthenticationProviderConfigurer<B> userServiceDetailMapper(UserDetailsContextMapper userDetailsContextMapper) {
        this.userDetailsContextMapper = userDetailsContextMapper;
        return this;
    }

}
