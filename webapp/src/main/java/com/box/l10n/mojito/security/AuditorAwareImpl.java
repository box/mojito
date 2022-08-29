package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.Optional;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** @author wyau */
@Component
public class AuditorAwareImpl implements AuditorAware<User> {

  @Override
  public Optional<User> getCurrentAuditor() {

    User currentAuditor = null;

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication != null
        && authentication.isAuthenticated()
        && authentication.getPrincipal() instanceof UserDetailsImpl) {
      currentAuditor = ((UserDetailsImpl) authentication.getPrincipal()).getUser();
    }

    return Optional.ofNullable(currentAuditor);
  }
}
