package com.box.l10n.mojito.rest;

import com.box.l10n.mojito.bootstrap.BootstrapConfig;
import com.box.l10n.mojito.rest.annotation.WithDefaultTestUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class WithDefaultTestUserSecurityContextFactory implements
        WithSecurityContextFactory<WithDefaultTestUser> {

    @Autowired
    BootstrapConfig bootstrapConfig;

    UserDetailsService userDetailsService;

    @Autowired
    public WithDefaultTestUserSecurityContextFactory(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public SecurityContext createSecurityContext(WithDefaultTestUser annotation) {
        UserDetails principal = userDetailsService.loadUserByUsername(bootstrapConfig.getDefaultUser().getUsername());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal, principal.getPassword(), principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        return context;
    }
}
