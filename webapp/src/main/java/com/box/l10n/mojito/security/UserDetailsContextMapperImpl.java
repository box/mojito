package com.box.l10n.mojito.security;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;
import java.util.Collection;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link UserDetailsContextMapper} used for {@link LdapAuthenticationProviderConfigurer}
 * @author wyau
 */
@Component
public class UserDetailsContextMapperImpl implements UserDetailsContextMapper {

    /**
     * logger
     */
    static Logger logger = getLogger(UserDetailsContextMapperImpl.class);

    @Autowired
    UserDetailsServiceImpl userDetailsServiceImpl;

    /**
     * Mapper for custom role
     *
     * @param dirContextOperations
     * @param username
     * @param authorities
     * @return
     */
    @Override
    public UserDetails mapUserFromContext(DirContextOperations dirContextOperations, String username, Collection<? extends GrantedAuthority> authorities) {
        logger.debug("Mapping user from context");

        UserDetails userDetails = null;

        try {
            userDetails = userDetailsServiceImpl.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            // THese are pretty standard LDAP attributes so we can expect them to be here
            // https://tools.ietf.org/html/rfc4519
            String givenName = dirContextOperations.getStringAttribute("givenname");
            String surname = dirContextOperations.getStringAttribute("sn");
            String commonName = dirContextOperations.getStringAttribute("cn");

            userDetails = userDetailsServiceImpl.createBasicUser(username, givenName, surname, commonName);
        }

        return userDetails;
    }

    @Override
    public void mapUserToContext(UserDetails userDetails, DirContextAdapter dirContextAdapter) {
        throw new UnsupportedOperationException("CustomUserDetailsContextMapper only supports reading from a context. Please" +
                "use a subclass if mapUserToContext() is required.");
    }
}
