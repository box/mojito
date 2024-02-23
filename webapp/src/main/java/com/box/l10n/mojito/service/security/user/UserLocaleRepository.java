package com.box.l10n.mojito.service.security.user;

import com.box.l10n.mojito.entity.security.user.UserLocale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface UserLocaleRepository
    extends JpaRepository<UserLocale, Long>, JpaSpecificationExecutor<UserLocale> {}
