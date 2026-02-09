package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserDeletionService {

  @Autowired UserRepository userRepository;

  @Autowired AuthorityRepository authorityRepository;

  @Autowired UserLocaleRepository userLocaleRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void hardDeleteUser(Long userId) throws DataIntegrityViolationException {
    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return;
    }
    authorityRepository.deleteAll(user.getAuthorities());
    userLocaleRepository.deleteAll(user.getUserLocales());
    userRepository.deleteById(userId);
    userRepository.flush();
  }
}
