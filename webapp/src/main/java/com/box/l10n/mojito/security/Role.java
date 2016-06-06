package com.box.l10n.mojito.security;

/**
 * @author wyau
 */
public enum Role {
    /**
     * Project Manager
     */
    PM("PM"),

    /**
     * Translator in Mojito
     */
    TRANSLATOR("TRANSLATOR"),

    /**
     * Administrator of Mojito.
     */
    ADMIN("ADMIN"),

    /**
     * User does not have much authorities.  Any new user who is logging in for the first time will have this role
     */
    USER("USER");

    String roleName;

    private Role(String roleName) {
        this.roleName = roleName;
    }

    public String getRoleName() {
        return roleName;
    }

}
