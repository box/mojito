package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TM;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jaurambault */
@RepositoryRestResource(exported = false)
public interface TMRepository extends JpaRepository<TM, Long> {}
