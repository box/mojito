package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author wyau
 */
public class UserDetailsImpl implements UserDetails {

    private final User user;
    private final Collection<? extends GrantedAuthority> grantedAuthorities;

    public UserDetailsImpl(User user) {
        this.grantedAuthorities = getGrantedAuthority(user);
        this.user = user;
    }

    /**
     * Get {@link org.springframework.security.core.GrantedAuthority} for a given {@link com.box.l10n.mojito.entity.security.user.User}
     *
     * @param user User to get {@link org.springframework.security.core.GrantedAuthority} for
     * @return The set of {@link org.springframework.security.core.GrantedAuthority} extracted from the {@link com.box.l10n.mojito.entity.security.user.User}
     */
    protected Set<GrantedAuthority> getGrantedAuthority(User user) {
        Set<Authority> authorities = user.getAuthorities();
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        for (Authority authority : authorities) {
            GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(authority.getAuthority());

            grantedAuthorities.add(grantedAuthority);
        }

        return grantedAuthorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return grantedAuthorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled();
    }

    public User getUser() {
        return user;
    }
}
