package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.userdetails.UserDetailsContextMapper;
import org.springframework.stereotype.Component;

import java.util.Collection;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * {@link UserDetailsContextMapper} used for {@link LdapAuthenticationProviderConfigurer}
 *
 * @author wyau
 */
@Component
public class UserDetailsContextMapperImpl implements UserDetailsContextMapper {

    /**
     * logger
     */
    static Logger logger = getLogger(UserDetailsContextMapperImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

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
        User user = getOrCreateUser(dirContextOperations, username);
        return new UserDetailsImpl(user);
    }

    User getOrCreateUser(DirContextOperations dirContextOperations, String username) {

        // These are pretty standard LDAP attributes so we can expect them to be here
        // https://tools.ietf.org/html/rfc4519
        String givenName = dirContextOperations.getStringAttribute("givenname");
        String surname = dirContextOperations.getStringAttribute("sn");
        String commonName = dirContextOperations.getStringAttribute("cn");

        User user = userService.getOrCreateOrUpdateBasicUser(username, givenName, surname, commonName);
        return user;
    }

    @Override
    public void mapUserToContext(UserDetails userDetails, DirContextAdapter dirContextAdapter) {
        throw new UnsupportedOperationException("CustomUserDetailsContextMapper only supports reading from a context. Please" +
                "use a subclass if mapUserToContext() is required.");
    }
}
