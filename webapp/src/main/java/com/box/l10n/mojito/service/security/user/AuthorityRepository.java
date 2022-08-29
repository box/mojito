package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.Authority;
import com.box.l10n.mojito.entity.security.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author wyau */
@RepositoryRestResource(exported = false)
public interface AuthorityRepository
    extends JpaRepository<Authority, Long>, JpaSpecificationExecutor<Authority> {

  Authority findByUser(User user);
}
