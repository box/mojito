package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.PushRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface PushRunRepository extends JpaRepository<PushRun, Long> {
    Optional<PushRun> findByName(@Param("name") String name);
}