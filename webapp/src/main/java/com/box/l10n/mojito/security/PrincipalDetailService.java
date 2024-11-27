package com.box.l10n.mojito.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public interface PrincipalDetailService extends UserDetailsService {
  UserDetails loadServiceWithName(String serviceName) throws UsernameNotFoundException;
}
