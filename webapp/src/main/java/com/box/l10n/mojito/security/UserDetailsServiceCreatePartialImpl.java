package com.box.l10n.mojito.security;

import static org.slf4j.LoggerFactory.getLogger;

import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.service.security.user.UserService;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class UserDetailsServiceCreatePartialImpl implements PrincipalDetailService {

  /** logger */
  static Logger logger = getLogger(UserDetailsServiceCreatePartialImpl.class);

  @Autowired UserService userService;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userService.getOrCreatePartialBasicUser(username);
    return new UserDetailsImpl(user);
  }

  @Override
  public UserDetails loadServiceWithName(String serviceName) throws UsernameNotFoundException {
    User user = userService.getServiceAccountUser(serviceName);
    return new UserDetailsImpl(user);
  }
}
