package com.box.l10n.mojito.rest.security;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.service.security.user.UserRepository;
import com.box.l10n.mojito.service.security.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import org.slf4j.Logger;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jyi
 */
@RestController
public class UserWS {

  /** logger */
  static Logger logger = getLogger(UserWS.class);

  @Autowired UserRepository userRepository;

  @Autowired UserService userService;

  /**
   * Returns list of {@link User}
   *
   * @param username
   * @return
   */
  @Operation(summary = "Get paginated Users")
  @RequestMapping(value = "/api/users", method = RequestMethod.GET)
  public Page<User> getUsers(
      @RequestParam(value = "username", required = false) String username,
      @RequestParam(value = "search", required = false) String search,
      @ParameterObject @PageableDefault(sort = "username", direction = Sort.Direction.ASC)
          Pageable pageable) {
    return userService.findByUsernameOrName(username, search, pageable);
  }

  /**
   * Endpoint to verify if the user session is active
   *
   * @return a 200 response if the user session is active.
   */
  @Operation(summary = "Check if session is active")
  @RequestMapping(value = "/api/users/session", method = RequestMethod.GET)
  public ResponseEntity<Void> isSessionActive() {
    return ResponseEntity.ok().build();
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
      role = Role.ROLE_USER;
    } else {
      Authority authority = user.getAuthorities().iterator().next();
      role = Role.valueOf(authority.getAuthority());
    }

    User createdUser =
        userService.createUserWithRole(
            user.getUsername(),
            user.getPassword(),
            role,
            user.getGivenName(),
            user.getSurname(),
            user.getCommonName(),
            false);

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
    User user = userRepository.findById(userId).orElse(null);

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
  public void updateUserByUserId(@PathVariable Long userId, @RequestBody User user)
      throws UserWithIdNotFoundException {
    logger.info("Updating user [{}]", userId);
    User userToUpdate = userRepository.findById(userId).orElse(null);

    if (userToUpdate == null) {
      throw new UserWithIdNotFoundException(userId);
    }

    Role role = null;
    if (user.getAuthorities() != null && !user.getAuthorities().isEmpty()) {
      Authority authority = user.getAuthorities().iterator().next();
      role = Role.valueOf(authority.getAuthority());
    }

    userService.saveUserWithRole(
        userToUpdate,
        user.getPassword(),
        role,
        user.getGivenName(),
        user.getSurname(),
        user.getCommonName(),
        false);
  }

  @Operation(summary = "Update a user password")
  @RequestMapping(value = "/api/users/pw", method = RequestMethod.POST)
  public ResponseEntity<User> changePassword(@RequestBody PasswordChangeRequest requestDTO) {
    User user = userService.updatePassword(requestDTO.currentPassword(), requestDTO.newPassword());
    logger.info("Updated password for user [{}]", user.getId());
    return new ResponseEntity<>(user, HttpStatus.ACCEPTED);
  }
}
