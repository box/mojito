package com.box.l10n.mojito.service.drop;

import com.box.l10n.mojito.entity.Drop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author jaurambault */
@RepositoryRestResource(exported = false)
public interface DropRepository extends JpaRepository<Drop, Long>, JpaSpecificationExecutor<Drop> {}
