package com.box.l10n.mojito.security;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author wyau
 */
@Primary
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

  /** logger */
  static Logger logger = getLogger(UserDetailsServiceImpl.class);

  @Autowired UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    logger.debug("Attempting user database authentication for username: {}", username);
    User user = userRepository.findByUsername(username);

    if (user == null) {
      logger.debug("User not found: {}", username);
      throw new UsernameNotFoundException("User [" + username + "] is not found in the database.");
    }

    logger.debug("Database authentication user found: {}", user.getUsername());
    return new UserDetailsImpl(user);
  }
}
