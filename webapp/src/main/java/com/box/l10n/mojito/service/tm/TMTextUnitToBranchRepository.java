package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitToBranch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface TMTextUnitToBranchRepository extends JpaRepository<TMTextUnitToBranch, Long> {}
