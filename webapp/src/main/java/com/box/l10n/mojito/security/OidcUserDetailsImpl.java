package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public class OidcUserDetailsImpl extends UserDetailsImpl implements OidcUser {

  private final OidcUser oidcUser;

  public OidcUserDetailsImpl(User user, OidcUser oidcUser) {
    super(user);
    this.oidcUser = oidcUser;
  }

  @Override
  public Map<String, Object> getClaims() {
    return oidcUser.getClaims();
  }

  @Override
  public OidcUserInfo getUserInfo() {
    return oidcUser.getUserInfo();
  }

  @Override
  public OidcIdToken getIdToken() {
    return oidcUser.getIdToken();
  }

  @Override
  public Map<String, Object> getAttributes() {
    return oidcUser.getAttributes();
  }

  @Override
  public String getName() {
    return oidcUser.getName();
  }
}
