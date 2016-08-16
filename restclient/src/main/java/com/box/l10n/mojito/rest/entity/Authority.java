package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * Entity that describes a user.
 * This entity mirrors: com.box.l10n.mojito.entity.security.user.Authority
 * 
 * @author jyi
 */
public class Authority {
    
    private String authority;
    
    @JsonBackReference
    private User user;

    public String getAuthority() {
        return authority;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

}
