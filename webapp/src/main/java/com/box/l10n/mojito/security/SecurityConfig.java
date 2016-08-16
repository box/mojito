package com.box.l10n.mojito.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Contains the security configuration.
 *
 * @author jaurambault
 */
@Component
@ConfigurationProperties(prefix = "l10n.security")
public class SecurityConfig {

    AuthenticationType authenticationType = AuthenticationType.DATABASE;

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(AuthenticationType authenticationType) {
        this.authenticationType = authenticationType;
    }

    /**
     * Types of authentication available
     */
    public enum AuthenticationType {
        LDAP,
        DATABASE
    }

}
