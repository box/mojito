package com.box.l10n.mojito.security;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * @author wyau
 */
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * logger
     */
    static Logger logger = getLogger(UserDetailsServiceImpl.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User [" + username + "] is not found in the database.");
        }

        return new UserDetailsImpl(user);
    }

    /**
     * Create user with the username
     *
     * @param username username name of the user to be created
     * @param givenName
     * @param surname
     * @param commonName
     */
    protected UserDetails createBasicUser(String username, String givenName, String surname, String commonName) {
        logger.info("Creating user: {}", username);

        String randomPassword = RandomStringUtils.randomAlphanumeric(15);
        User userWithRole = userService.createUserWithRole(username, randomPassword, Role.USER, givenName, surname, commonName);

        logger.debug("Manually setting created by user to system user because at this point, there isn't an authenticated user context");
        userService.updateCreatedByUserToSystemUser(userWithRole);

        return new UserDetailsImpl(userWithRole);
    }
}
