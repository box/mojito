package com.box.l10n.mojito.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To get user information from a payload that looks like
 *
 * <pre>
 *  {'user': {
 *         "username": "xyz",
 *         "first_name": "X",
 *         "last_name": "Y",
 *         "email": "xyz@test.com"
 *  }}
 * </pre>
 *
 * {@see org.springframework.security.oauth2.client.userinfo.CustomUserTypesOAuth2UserService}
 * {@see }
 */
public class UsernameInUserOAuth2User implements OAuth2User {

    Map<String, Object> user = new HashMap<>();

    List<GrantedAuthority> grantedAuthorityList = new ArrayList<>();

    @Override
    public Map<String, Object> getAttributes() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorityList;
    }

    @Override
    public String getName() {
        return String.valueOf(user.get("username"));
    }


    public Map<String, Object> getUser() {
        return user;
    }

    public void setUser(Map<String, Object> user) {
        this.user = user;
    }
}
