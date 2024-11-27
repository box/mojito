package com.box.l10n.mojito.service.security.user;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.pagerduty.PagerDutyClient;
import com.box.l10n.mojito.pagerduty.PagerDutyException;
import com.box.l10n.mojito.pagerduty.PagerDutyIntegrationService;
import com.box.l10n.mojito.pagerduty.PagerDutyPayload;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.security.HeaderSecurityConfig;
import com.box.l10n.mojito.security.Role;
import com.box.l10n.mojito.security.ServiceDisambiguator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author wyau
 */
@Component
public class UserService {

  /** logger */
  static Logger logger = getLogger(UserService.class);

  public static final String SYSTEM_USERNAME = "system";

  @Autowired UserRepository userRepository;

  @Autowired AuthorityRepository authorityRepository;

  @Autowired AuditorAwareImpl auditorAwareImpl;

  @Autowired MeterRegistry meterRegistry;

  @Autowired(required = false)
  ServiceDisambiguator serviceDisambiguator;

  @Autowired(required = false)
  HeaderSecurityConfig headerSecurityConfig;

  @Autowired(required = false)
  PagerDutyIntegrationService pagerDutyIntegrationService;

  /**
   * Allow PMs and ADMINs to create / edit users. However, a PM user can not create / edit ADMIN
   * users.
   */
  private void checkPermissionsForRole(Role role) {
    final Optional<User> currentUser = auditorAwareImpl.getCurrentAuditor();
    if (currentUser.isEmpty()) {
      // Can't happen in the webapp because only authenticated users may use
      // the API endpoints. However, allow this for tests
      return;
    }
    final String currentAuthority =
        currentUser.get().getAuthorities().iterator().next().getAuthority();
    final Role currentRole = createRoleFromAuthority(currentAuthority);

    switch (currentRole) {
      case ROLE_PM -> {
        if (role == Role.ROLE_ADMIN) {
          throw new AccessDeniedException(
              "Access denied! PMs are not allowed to edit / create ADMINs");
        }
      }
      case ROLE_ADMIN -> {
        // There is nothing above admin
      }
      case ROLE_TRANSLATOR, ROLE_USER ->
          throw new AccessDeniedException(
              "Access denied! Users and Translators are not allowed to to edit / create users");
    }
  }

  /**
   * Create a {@link com.box.l10n.mojito.entity.security.user.User}. This does not check if there is
   * already a user with the provided username
   *
   * @param username Username for the new user
   * @param password Password must not be null
   * @param role The basic role for the new user
   * @param givenName The given name (first name)
   * @param surname The surname (last name)
   * @param commonName The common name (givenName surname)
   * @return The newly created user
   */
  public User createUserWithRole(
      String username,
      String password,
      Role role,
      String givenName,
      String surname,
      String commonName,
      boolean partiallyCreated) {
    logger.debug("Creating user entry for: {}", username);
    Preconditions.checkNotNull(password, "password must not be null");
    Preconditions.checkState(!password.isEmpty(), "password must not be empty");

    // Only PMs and ADMINs can create new users and PMs can not create ADMIN users (privilege
    // escalation)
    checkPermissionsForRole(role);

    User user = new User();
    user.setEnabled(true);
    user.setUsername(username);

    return saveUserWithRole(user, password, role, givenName, surname, commonName, partiallyCreated);
  }

  /**
   * Saves a {@link com.box.l10n.mojito.entity.security.user.User}
   *
   * @param user
   * @param password
   * @param role
   * @param givenName
   * @param surname
   * @param commonName
   * @param partiallyCreated
   * @return
   */
  @Transactional
  public User saveUserWithRole(
      User user,
      String password,
      Role role,
      String givenName,
      String surname,
      String commonName,
      boolean partiallyCreated) {

    // Only PMs and ADMINs can edit users and PMs can not edit ADMIN users (privilege escalation)
    if (!user.getAuthorities().isEmpty()) {
      Role userRole =
          createRoleFromAuthority(user.getAuthorities().iterator().next().getAuthority());
      checkPermissionsForRole(userRole);
    }
    checkPermissionsForRole(role == null ? Role.ROLE_PM : role);

    if (givenName != null) {
      user.setGivenName(givenName);
    }

    if (surname != null) {
      user.setSurname(surname);
    }

    if (commonName != null) {
      user.setCommonName(commonName);
    }

    if (!StringUtils.isEmpty(password)) {
      BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
      user.setPassword(bCryptPasswordEncoder.encode(password));
    }

    user.setPartiallyCreated(partiallyCreated);

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

  @Transactional
  public User updatePassword(String currentPassword, String newPassword) {
    Objects.requireNonNull(currentPassword);
    Objects.requireNonNull(newPassword);

    User user = auditorAwareImpl.getCurrentAuditor().orElseThrow();

    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
    if (!bCryptPasswordEncoder.matches(currentPassword, user.getPassword())) {
      throw new AccessDeniedException("Invalid current password");
    }

    user.setPassword(bCryptPasswordEncoder.encode(newPassword));
    userRepository.save(user);

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
    return createUserWithRole(username, password, role, null, null, null, false);
  }

  /**
   * Create authority name to be used for authority
   *
   * @param role
   * @return
   */
  public String createAuthorityName(Role role) {
    return role.name();
  }

  /** Reverses {@link #createAuthorityName(Role)} */
  public Role createRoleFromAuthority(String auth) {
    return Role.valueOf(auth);
  }

  /**
   * @return The System User
   */
  public User findSystemUser() {
    return userRepository.findByUsername(SYSTEM_USERNAME);
  }

  /**
   * Update {@link User#createdByUser}. This is useful for when the {@link User} was created without
   * an authenticated context, hence, the {@link org.springframework.data.domain.AuditorAware} will
   * return null by default.
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

    authorityRepository.saveAll(authorities);
    userRepository.save(userToUpdate);
  }

  /**
   * Deletes a {@link User} by the {@link User#id}. It performs logical delete.
   *
   * @param user
   */
  @Transactional
  public void deleteUser(User user) {
    // Only PMs and ADMINs can delete users and PMs can not delete ADMIN users
    Role userRole = createRoleFromAuthority(user.getAuthorities().iterator().next().getAuthority());
    checkPermissionsForRole(userRole);

    logger.debug("Delete a user with username: {}", user.getUsername());

    // rename the deleted username so that the username can be reused to create new user
    String name = "deleted__" + System.currentTimeMillis() + "__" + user.getUsername();
    user.setUsername(StringUtils.abbreviate(name, User.NAME_MAX_LENGTH));
    user.setEnabled(false);
    userRepository.save(user);

    logger.debug("Deleted user with username: {}", user.getUsername());
  }

  public User createBasicUser(
      String username,
      String givenName,
      String surname,
      String commonName,
      boolean partiallyCreated) {
    logger.debug("Creating user: {}", username);

    String randomPassword = RandomStringUtils.randomAlphanumeric(15);
    User userWithRole =
        createUserWithRole(
            username,
            randomPassword,
            Role.ROLE_USER,
            givenName,
            surname,
            commonName,
            partiallyCreated);

    logger.debug(
        "Manually setting created by user to system user because at this point, there isn't an authenticated user context");
    updateCreatedByUserToSystemUser(userWithRole);

    return userWithRole;
  }

  public User createOrUpdateBasicUser(
      User user, String username, String givenName, String surname, String commonName) {

    if (user == null) {
      logger.debug(
          "create with username: {}, giveName:{}, surname: {}, commonName: {}",
          username,
          givenName,
          surname,
          commonName);
      user = createBasicUser(username, givenName, surname, commonName, false);
    } else {
      logger.debug(
          "update with username: {}, giveName:{}, surname: {}, commonName: {}",
          username,
          givenName,
          surname,
          commonName);
      user = saveUserWithRole(user, null, null, givenName, surname, commonName, false);
    }

    return user;
  }

  /**
   * Gets a user by name and if it doesn't exist create a "partially" created user.
   *
   * <p>This is to be used by job that does something on behalf of a user is not yet in the system.
   * The partially created attribute is then check during user login to update the information if
   * required.
   *
   * @param username
   * @return
   */
  public User getOrCreatePartialBasicUser(String username) {

    User user = userRepository.findByUsername(username);

    if (user == null) {
      return createBasicUser(username, null, null, null, true);
    }
    return user;
  }

  public User getOrCreateOrUpdateBasicUser(
      String username, String givenName, String surname, String commonName) {

    User user = userRepository.findByUsername(username);

    if (user == null || user.getPartiallyCreated()) {
      user = createOrUpdateBasicUser(user, username, givenName, surname, commonName);
    }

    return user;
  }

  /**
   * Cannot use an EntityGraph with pagination as it triggers the following warning: HHH90003004:
   * firstResult/maxResults specified with collection fetch; applying in memory
   *
   * @param username
   * @param search
   * @param pageable
   */
  public Page<User> findByUsernameOrName(String username, String search, Pageable pageable) {
    final Page<User> users = userRepository.findByUsernameOrName(username, search, pageable);
    users.forEach(
        u -> {
          Hibernate.initialize(u.getAuthorities());
          u.getAuthorities()
              .forEach(
                  a -> {
                    Hibernate.initialize(a.getUser());
                    Hibernate.initialize(a.getCreatedByUser());
                  });
          Hibernate.initialize(u.getCreatedByUser());
        });
    return users;
  }

  /**
   * Gets a service by name and if it doesn't exist create a created user.
   *
   * <p>All admin service accounts should already be created beforehand. Get the exact service
   * account or any service account which is the ancestor service to the account
   *
   * @param serviceName
   * @return
   */
  public User getServiceAccountUser(String serviceName) {
    if (serviceDisambiguator == null) {
      logger.debug("Service Disambiguator is null. Falling back to regular exact match logic");
      return userRepository.findByUsername(serviceName);
    }

    if (headerSecurityConfig == null) {
      logger.debug("Header config does not exist. Falling back to regular exact match logic");
      return userRepository.findByUsername(serviceName);
    }

    List<User> users =
        userRepository.findServicesByServiceNameAndPrefix(
            serviceName, headerSecurityConfig.getServicePrefix());
    if (users.isEmpty()) {
      logger.debug("No matching services found as per DB query");
      sendPagerDutyNotification(serviceName, PagerDutyTypeNotificationType.CreateIncident);
      logger.error(
          "Service '{}' attempted and failed authentication. No matching services found.",
          serviceName);
      return null;
    }

    User matchingUser = serviceDisambiguator.getServiceWithCommonAncestor(users, serviceName);
    if (matchingUser == null) {
      logger.debug("No matching services found as per ServiceDisambiguator");
      sendPagerDutyNotification(serviceName, PagerDutyTypeNotificationType.CreateIncident);
      logger.error(
          "Service '{}' attempted and failed authentication. No services could be fuzzy matched",
          serviceName);
      throw new UsernameNotFoundException("Service with name '" + serviceName + "' was not found");
    } else {
      sendPagerDutyNotification(serviceName, PagerDutyTypeNotificationType.ResolveIncident);
    }

    logger.debug("Matching service found: {}", matchingUser.getUsername());
    return matchingUser;
  }

  enum PagerDutyTypeNotificationType {
    CreateIncident,
    ResolveIncident
  }

  private void sendPagerDutyNotification(
      String serviceName, PagerDutyTypeNotificationType notificationType) {
    if (!headerSecurityConfig.isPagerDutyEnabled()) {
      return;
    }

    if (pagerDutyIntegrationService == null) {
      logger.error(
          "Pager Duty Integration service is null but configured to send notifications for service auth");
      incrementPagerDutyNotConfiguredCounter(serviceName);
      return;
    }

    Optional<PagerDutyClient> configuredPagerDutyClient =
        Optional.of(headerSecurityConfig)
            .map(HeaderSecurityConfig::getPagerDutyIntegrationName)
            .map(
                integrationName ->
                    pagerDutyIntegrationService.getPagerDutyClient((integrationName)))
            .orElse(pagerDutyIntegrationService.getDefaultPagerDutyClient());
    if (configuredPagerDutyClient.isEmpty()) {
      logger.error(
          "No PagerDuty client configured with name {}",
          Optional.of(headerSecurityConfig.getPagerDutyIntegrationName())
              .map(name -> "'" + name + "'")
              .orElse("null"));
      incrementPagerDutyNotConfiguredCounter(serviceName);
      return;
    }

    PagerDutyClient pagerDutyClient = configuredPagerDutyClient.get();
    String dedupKey = buildUnrecognizedServiceDedupKey(serviceName);
    try {
      if (notificationType == PagerDutyTypeNotificationType.CreateIncident) {
        pagerDutyClient.triggerIncident(
            dedupKey,
            new PagerDutyPayload(
                "Mojito unrecognized service attempting authentication: '" + serviceName + "'",
                serviceName,
                PagerDutyPayload.Severity.ERROR,
                ImmutableMap.of("serviceName", serviceName)));
      }

      if (notificationType == PagerDutyTypeNotificationType.ResolveIncident) {
        pagerDutyClient.resolveIncident(dedupKey);
      }
    } catch (PagerDutyException e) {
      logger.error("Unable to trigger pager duty incident for unrecognized service auth", e);
    }
  }

  private String buildUnrecognizedServiceDedupKey(String serviceName) {
    String normalizedServiceName =
        serviceName
            .replaceAll(headerSecurityConfig.getServicePrefix(), "")
            .replaceAll(headerSecurityConfig.getServiceDelimiter(), ":");
    return "mojito:auth:unrecognized:service:account:" + normalizedServiceName;
  }

  private void incrementPagerDutyNotConfiguredCounter(String serviceName) {
    meterRegistry
        .counter(
            "UserService.sendPagerDutyNotification.pagerDutyClientNotConfigured",
            Tags.of(
                "serviceName",
                serviceName,
                "clientIntegrationName",
                Optional.of(headerSecurityConfig)
                    .map(HeaderSecurityConfig::getPagerDutyIntegrationName)
                    .orElse("null")))
        .increment();
  }
}
