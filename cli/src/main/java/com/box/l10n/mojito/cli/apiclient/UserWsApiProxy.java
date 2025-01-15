package com.box.l10n.mojito.cli.apiclient;

import com.box.l10n.mojito.cli.model.PageUser;
import com.box.l10n.mojito.cli.model.Pageable;
import com.box.l10n.mojito.cli.model.User;
import com.box.l10n.mojito.rest.client.exception.ResourceNotCreatedException;
import com.box.l10n.mojito.rest.client.exception.ResourceNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class UserWsApiProxy {
  /** logger */
  static Logger logger = LoggerFactory.getLogger(UserWsApiProxy.class);

  @Autowired private UserWsApi userClient;

  public User createUser(User body) throws ResourceNotCreatedException {
    logger.debug("Creating user with username [{}]", body.getUsername());
    try {
      return this.userClient.createUser(body);
    } catch (HttpClientErrorException exception) {
      if (exception.getStatusCode().equals(HttpStatus.CONFLICT)) {
        throw new ResourceNotCreatedException(
            "User with username [" + body.getUsername() + "] already exists");
      } else {
        throw exception;
      }
    }
  }

  private User getUserByUsername(String username) throws ResourceNotFoundException {
    Pageable pageable = new Pageable();
    pageable.setPage(0);
    pageable.setSize(Integer.MAX_VALUE);
    pageable.setSort(List.of());
    PageUser pageUser = this.userClient.getUsers(pageable, username, null);
    if (pageUser.getContent().isEmpty()) {
      throw new ResourceNotFoundException("User with username [" + username + "] is not found");
    }
    return pageUser.getContent().getFirst();
  }

  public void deleteUserByUsername(String username) throws ResourceNotFoundException {
    logger.debug("Deleting user by username = [{}]", username);
    this.userClient.deleteUserByUserId(this.getUserByUsername(username).getId());
  }

  public void updateUserByUsername(User body, String username) throws ResourceNotFoundException {
    logger.debug("Updating user by username = [{}]", username);
    User userToUpdate = this.getUserByUsername(username);
    userToUpdate.setPassword(body.getPassword());
    userToUpdate.setSurname(body.getSurname());
    userToUpdate.setGivenName(body.getGivenName());
    userToUpdate.setCommonName(body.getCommonName());
    userToUpdate.setAuthorities(body.getAuthorities());

    this.userClient.updateUserByUserId(userToUpdate, userToUpdate.getId());
  }
}
