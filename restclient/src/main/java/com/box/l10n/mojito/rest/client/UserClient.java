package com.box.l10n.mojito.rest.client;

import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotFoundException;
import com.box.l10n.mojito.rest.entity.Authority;
import com.box.l10n.mojito.rest.entity.Page;
import com.box.l10n.mojito.rest.entity.Role;
import com.box.l10n.mojito.rest.entity.User;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author jyi
 */
@Component
public class UserClient extends BaseClient {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserClient.class);

  @Override
  public String getEntityName() {
    return "users";
  }

  /**
   * Get a list of {@link User}s.
   *
   * @param username
   * @return List of {@link User}s
   */
  public List<User> getUsers(String username) {

    Map<String, String> filterParams = new HashMap<>();

    if (username != null) {
      filterParams.put("username", username);
    }

    filterParams.put("size", "128");

    List<User> result = new ArrayList<>();
    for (int p = 0; true; p++) {
      filterParams.put("page", String.valueOf(p));

      ResponseEntity<Page<User>> responseEntity =
              authenticatedRestTemplate.getForEntityWithQueryParams(
                      getBasePathForEntity(), new ParameterizedTypeReference<Page<User>>() {}, filterParams
              );

      Page<User> page = responseEntity.getBody();
      if (page == null) {
        break;
      }

      result.addAll(page.getContent());

      if (page.isLast()) {
        break;
      }
    }

    return result;
  }

  /**
   * Creates a {@link User}
   *
   * @param username
   * @param password
   * @param role
   * @param surname
   * @param givenName
   * @param commonName
   * @return
   * @throws com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException
   */
  public User createUser(
      String username,
      String password,
      Role role,
      String surname,
      String givenName,
      String commonName)
      throws ResourceNotCreatedException {
    logger.debug("Creating user with username [{}]", username);

    User userToCreate = new User();
    userToCreate.setUsername(username);
    userToCreate.setPassword(password);
    userToCreate.setSurname(surname);
    userToCreate.setGivenName(givenName);
    userToCreate.setCommonName(commonName);

    if (role != null) {
      Authority authority = new Authority();
      authority.setAuthority(role.toString());
      userToCreate.setAuthorities(Sets.newHashSet(authority));
    }

    try {
      return authenticatedRestTemplate.postForObject(
          getBasePathForEntity(), userToCreate, User.class);
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotCreatedException(
            "User with username [" + username + "] already exists");
      } else {
        throw exception;
      }
    }
  }

  /**
   * Deletes a {@link User} by the {@link User#username}
   *
   * @param username
   * @throws com.box.l10n.mojito.rest.client.exception.ResourceNotFoundException
   */
  public void deleteUserByUsername(String username) throws ResourceNotFoundException {
    logger.debug("Deleting user by username = [{}]", username);
    List<User> users = getUsers(username);
    if (users.isEmpty()) {
      throw new ResourceNotFoundException("User with username [" + username + "] is not found");
    } else {
      authenticatedRestTemplate.delete(getBasePathForEntity() + "/" + users.get(0).getId());
    }
  }

  /**
   * Updates a {@link User} by the {@link User#username}
   *
   * @param username
   * @param password
   * @param role
   * @param surname
   * @param givenName
   * @param commonName
   * @throws ResourceNotFoundException
   */
  public void updateUserByUsername(
      String username,
      String password,
      Role role,
      String surname,
      String givenName,
      String commonName)
      throws ResourceNotFoundException {
    logger.debug("Updating user by username = [{}]", username);

    List<User> users = getUsers(username);
    if (users.isEmpty()) {
      throw new ResourceNotFoundException("User with username [" + username + "] is not found");
    } else {
      User user = users.get(0);
      user.setPassword(password);
      user.setSurname(surname);
      user.setGivenName(givenName);
      user.setCommonName(commonName);

      Set<Authority> authorities = new HashSet<>();
      if (role != null) {
        Authority authority = new Authority();
        authority.setAuthority(role.toString());
        authorities.add(authority);
      }
      user.setAuthorities(authorities);

      authenticatedRestTemplate.patch(getBasePathForResource(user.getId()), user);
    }
  }
}
