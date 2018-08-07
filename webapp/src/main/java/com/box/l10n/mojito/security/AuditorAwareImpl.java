package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class AuditorAwareImpl implements AuditorAware<User> {

    @Autowired
    UserRepository userRepository;

    @Override
    public User getCurrentAuditor() {

        User currentAuditor = null;

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            currentAuditor = ((UserDetailsImpl) authentication.getPrincipal()).getUser();
        }

        return currentAuditor;
    }
}



