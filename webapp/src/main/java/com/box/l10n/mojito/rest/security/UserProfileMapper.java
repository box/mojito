package com.box.l10n.mojito.rest.security;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.entity.security.user.UserLocale;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserService;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserProfileMapper {

  @Autowired UserService userService;

  public UserProfile toUserProfile(User user) {
    UserProfile u = new UserProfile();
    u.setUsername(user.getUsername());
    u.setGivenName(user.getGivenName());
    u.setSurname(user.getSurname());
    u.setCommonName(user.getCommonName());
    u.setCanTranslateAllLocales(user.getCanTranslateAllLocales());
    u.setUserLocales(
        user.getUserLocales().stream()
            .map(UserLocale::getLocale)
            .map(Locale::getBcp47Tag)
            .collect(Collectors.toList()));

    Role role =
        user.getAuthorities().stream()
            .findFirst()
            .map(Authority::getAuthority)
            .map(userService::createRoleFromAuthority)
            .orElse(Role.ROLE_USER);
    u.setRole(role);
    return u;
  }
}
