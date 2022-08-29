package com.box.l10n.mojito.service.cache;

import com.box.l10n.mojito.entity.ApplicationCacheType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/** @author garion */
@RepositoryRestResource(exported = false)
public interface ApplicationCacheTypeRepository extends JpaRepository<ApplicationCacheType, Short> {
  ApplicationCacheType findByName(@Param("name") String name);
}
