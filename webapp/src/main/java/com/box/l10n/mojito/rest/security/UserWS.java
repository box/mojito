package com.box.l10n.mojito.rest.security;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import static com.box.l10n.mojito.rest.security.UserSpecification.enabledEquals;
import static com.box.l10n.mojito.rest.security.UserSpecification.usernameEquals;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import static com.box.l10n.mojito.specification.Specifications.ifParamNotNull;
import java.util.List;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import static org.springframework.data.jpa.domain.Specifications.where;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author jyi
 */
@RestController
public class UserWS {

    /**
     * logger
     */
    static Logger logger = getLogger(UserWS.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserService userService;

    /**
     * Returns list of {@link User}
     *
     * @param username
     * @return
     */
    @RequestMapping(value = "/api/users", method = RequestMethod.GET)
    public List<User> getUsers(@RequestParam(value = "username", required = false) String username) {
        List<User> users = userRepository.findAll(
                where(ifParamNotNull(usernameEquals(username)))
                        .and(enabledEquals(true)),
                new Sort(Sort.Direction.ASC, "username"));
        return users;
    }

    /**
     * Creates a {@link User}
     *
     * @param user
     * @return
     */
    @RequestMapping(value = "api/users", method = RequestMethod.POST)
    public ResponseEntity<User> createUser(@RequestBody User user) {

        User existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser != null) {
            logger.error("User with username " + user.getUsername() + " already exists");
            return new ResponseEntity<>(HttpStatus.CONFLICT);
        }

        logger.info("Creating user");

        Role role;
        if (user.getAuthorities() == null || user.getAuthorities().isEmpty()) {
            role = Role.USER;
        } else {
            Authority authority = user.getAuthorities().iterator().next();
            role = Role.valueOf(authority.getAuthority());
        }

        User createdUser = userService.createUserWithRole(user.getUsername(), user.getPassword(), role,
                user.getGivenName(), user.getSurname(), user.getCommonName(), false);

        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Deletes a {@link User}
     *
     * @param userId
     * @return
     */
    @RequestMapping(value = "/api/users/{userId}", method = RequestMethod.DELETE)
    public void deleteUserByUserId(@PathVariable Long userId) throws UserWithIdNotFoundException {
        logger.info("Deleting user [{}]", userId);
        User user = userRepository.findOne(userId);

        if (user == null) {
            throw new UserWithIdNotFoundException(userId);
        }

        userService.deleteUser(user);
    }

    /**
     * Updates {@link User}
     *
     * @param userId
     * @param user
     * @return
     */
    @RequestMapping(value = "/api/users/{userId}", method = RequestMethod.PATCH)
    public void updateUserByUserId(@PathVariable Long userId, @RequestBody User user) throws UserWithIdNotFoundException {
        logger.info("Updating user [{}]", userId);
        User userToUpdate = userRepository.findOne(userId);

        if (userToUpdate == null) {
            throw new UserWithIdNotFoundException(userId);
        }

        Role role = null;
        if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
            Authority authority = user.getAuthorities().iterator().next();
            role = Role.valueOf(authority.getAuthority());
        }
        
        userService.saveUserWithRole(userToUpdate, user.getPassword(), role, user.getGivenName(), user.getSurname(), user.getCommonName(), false);
    }

}
