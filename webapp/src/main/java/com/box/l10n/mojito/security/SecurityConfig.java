package com.box.l10n.mojito.security;

import java.util.Arrays;
import java.util.List;
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

    List<AuthenticationType> authenticationType = Arrays.asList(AuthenticationType.DATABASE);

    public List<AuthenticationType> getAuthenticationType() {
        return authenticationType;
    }

    public void setAuthenticationType(List<AuthenticationType> authenticationType) {
        this.authenticationType = authenticationType;
    }

    /**
     * Types of authentication available
     */
    public enum AuthenticationType {
        LDAP,
        DATABASE,
        AD,
    }

}
