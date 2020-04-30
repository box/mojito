package com.box.l10n.mojito.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

// This must in sync with {@link com.box.l10n.mojito.security.SecurityConfig.AuthenticationType#HEADER}
@ConditionalOnExpression("'${l10n.security.authenticationType:}'.toUpperCase().contains('HEADER')")
@Configuration
class WebSecurityHeaderConfig {

    @Bean
    RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter() throws Exception {
        RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter = new RequestHeaderAuthenticationFilter();
        requestHeaderAuthenticationFilter.setPrincipalRequestHeader("x-forwarded-user");
        requestHeaderAuthenticationFilter.setExceptionIfHeaderMissing(false);
        requestHeaderAuthenticationFilter.setAuthenticationManager(new AuthenticationManager() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                throw new RuntimeException("This must be overridden with the actual authentication manager of the app - it is not injectable yet with this config style");
            }
        });

        return requestHeaderAuthenticationFilter;
    }

    @Bean
    PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider() {
        PreAuthenticatedAuthenticationProvider preAuthenticatedAuthenticationProvider = new PreAuthenticatedAuthenticationProvider();
        UserDetailsByNameServiceWrapper userDetailsByNameServiceWrapper = new UserDetailsByNameServiceWrapper(getUserDetailsServiceCreatePartial());
        preAuthenticatedAuthenticationProvider.setPreAuthenticatedUserDetailsService(userDetailsByNameServiceWrapper);
        return preAuthenticatedAuthenticationProvider;
    }

    @Bean
    protected UserDetailsServiceCreatePartialImpl getUserDetailsServiceCreatePartial() {
        return new UserDetailsServiceCreatePartialImpl();
    }
}
