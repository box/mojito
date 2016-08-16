package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.transaction.Transactional;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @author wyau
 */
@Component
public class UserService {

    /**
     * logger
     */
    static Logger logger = getLogger(UserService.class);

    public static final String SYSTEM_USERNAME = "system";
    private final String rolePrefix = "ROLE_";

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthorityRepository authorityRepository;

    /**
     * Create a {@link com.box.l10n.mojito.entity.security.user.User}. This does
     * not check if there is already a user with the provided username
     *
     * @param username Username for the new user
     * @param password Password must not be null
     * @param role The basic role for the new user
     * @param givenName The given name (first name)
     * @param surname The surname (last name)
     * @param commonName The common name (givenName surname)
     * @return The newly created user
     */
    public User createUserWithRole(String username, String password, Role role, String givenName, String surname, String commonName) {
        logger.info("Creating user entry for: {}", username);
        Preconditions.checkNotNull(password, "password must not be null");
        Preconditions.checkState(!password.isEmpty(), "password must not be empty");

        User user = new User();
        user.setEnabled(true);
        user.setUsername(username);
        return saveUserWithRole(user, password, role, givenName, surname, commonName);
    }
    
    /**
     * Saves a {@link com.box.l10n.mojito.entity.security.user.User}
     * @param user
     * @param password
     * @param role
     * @param givenName
     * @param surname
     * @param commonName
     * @return 
     */
    @Transactional
    public User saveUserWithRole(User user, String password, Role role, String givenName, String surname, String commonName) {

        if (!StringUtils.isEmpty(givenName)) {
            user.setGivenName(givenName);
        }

        if (!StringUtils.isEmpty(surname)) {
            user.setSurname(surname);
        }

        if (!StringUtils.isEmpty(commonName)) {
            user.setCommonName(commonName);
        }

        if (!StringUtils.isEmpty(password)) {
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            user.setPassword(bCryptPasswordEncoder.encode(password));
        }

        userRepository.save(user);
        user = saveAuthorities(user, role);

        return user;
    }
    
    /**
     * Saves a {@link Role} for {@link User}
     * 
     * @param user
     * @param role
     * @return 
     */
    @Transactional
    private User saveAuthorities(User user, Role role) {
        if (role != null) {
            Authority authority = authorityRepository.findByUser(user);
            if (authority == null) {
                authority = new Authority();
            }
            authority.setUser(user);
            authority.setAuthority(createAuthorityName(role));
            authorityRepository.save(authority);
            user.setAuthorities(Sets.newHashSet(authority));
        }
        return user;
    }

    /**
     * Create a {@link com.box.l10n.mojito.entity.security.user.User}.
     *
     * @param username Username for the new user
     * @param password Password must not be null
     * @param role The basic role for the new user
     * @return The newly created user
     */
    public User createUserWithRole(String username, String password, Role role) {
        return createUserWithRole(username, password, role, null, null, null);
    }

    /**
     * Create authority name to be used for authority
     *
     * @param role
     * @return
     */
    public String createAuthorityName(Role role) {
        String roleName = role.getRoleName().toUpperCase();
        return rolePrefix + roleName;
    }

    /**
     * @return The System User
     */
    public User findSystemUser() {
        return userRepository.findByUsername(SYSTEM_USERNAME);
    }

    /**
     * Update {@link User#createdByUser}.  This is useful for when the {@link User} was created without an
     * authenticated context, hence, the {@link org.springframework.data.domain.AuditorAware} will return null by default.
     *
     * @param userToUpdate The {@link User} to set {@link User#createdByUser} for
     */
    @Transactional
    public void updateCreatedByUserToSystemUser(User userToUpdate) {
        logger.debug("Updating CreatedByUser to System User");
        User systemUser = findSystemUser();

        userToUpdate.setCreatedByUser(systemUser);

        Set<Authority> authorities = userToUpdate.getAuthorities();
        for (Authority authority : authorities) {
            authority.setCreatedByUser(systemUser);
        }

        authorityRepository.save(authorities);
        userRepository.save(userToUpdate);
    }
    
    /**
     * Deletes a {@link User} by the {@link User#id}. It performs
     * logical delete.
     *
     * @param user
     */
    @Transactional
    public void deleteUser(User user) {

        logger.debug("Delete a user with username: {}", user.getUsername());

        // rename the deleted username so that the username can be reused to create new user
        String name = "deleted__" + System.currentTimeMillis() + "__" + user.getUsername();
        user.setUsername(StringUtils.abbreviate(name, User.NAME_MAX_LENGTH));
        user.setEnabled(false);
        userRepository.save(user);

        logger.debug("Deleted user with username: {}", user.getUsername());
    }
    
}
