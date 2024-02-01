package com.box.l10n.mojito.rest;

import com.box.l10n.mojito.bootstrap.BootstrapConfig;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.rest.annotation.WithDefaultTestUser;
import com.box.l10n.mojito.security.UserDetailsImpl;
import com.box.l10n.mojito.service.security.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class WithDefaultTestUserSecurityContextFactory
    implements WithSecurityContextFactory<WithDefaultTestUser> {

  @Autowired BootstrapConfig bootstrapConfig;

  @Autowired UserRepository userRepository;

  @Override
  public SecurityContext createSecurityContext(WithDefaultTestUser annotation) {
    User user = userRepository.findByUsername(bootstrapConfig.getDefaultUser().getUsername());
    UserDetails principal = new UserDetailsImpl(user);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            principal, principal.getPassword(), principal.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    return context;
  }
}
