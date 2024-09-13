package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserService;
import java.util.Objects;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class UserDetailImplOidcUserService extends OidcUserService {

  SecurityConfig securityConfig;
  UserService userService;

  public UserDetailImplOidcUserService(SecurityConfig securityConfig, UserService userService) {
    this.securityConfig = Objects.requireNonNull(securityConfig);
    this.userService = Objects.requireNonNull(userService);
  }

  @Override
  public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
    OidcUser oidcUser = super.loadUser(userRequest);

    SecurityConfig.OAuth2 securityConfigOAuth2 =
        securityConfig
            .getoAuth2()
            .getOrDefault(
                userRequest.getClientRegistration().getRegistrationId(),
                new SecurityConfig.OAuth2());

    String username;
    if (securityConfigOAuth2.usernameFromEmail) {
      String email = oidcUser.getEmail();
      if (email == null) {
        throw new RuntimeException(
            "OidcUser's email must not be null since it used to extract username");
      }
      username = email.split("@")[0];
    } else {
      username = oidcUser.getName();
    }

    User user =
        userService.getOrCreateOrUpdateBasicUser(
            username, oidcUser.getGivenName(), oidcUser.getFamilyName(), oidcUser.getFullName());

    return new OidcUserDetailsImpl(user, oidcUser);
  }
}
