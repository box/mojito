package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import java.util.Map;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * {@link OAuth2User} based on {@link UserDetailsImpl} so that it can be reused in the rest of the
 * app
 */
public class OAuth2UserDetailsImpl extends UserDetailsImpl implements OAuth2User {

  private final Map<String, Object> oAuth2Attributes;

  public OAuth2UserDetailsImpl(User user, Map<String, Object> oAuth2Attributes) {
    super(user);
    this.oAuth2Attributes = oAuth2Attributes;
  }

  @Override
  public Map<String, Object> getAttributes() {
    return oAuth2Attributes;
  }

  @Override
  public String getName() {
    return getUsername();
  }
}
