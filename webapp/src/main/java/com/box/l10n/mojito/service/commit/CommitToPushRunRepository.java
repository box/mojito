package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.CommitToPushRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.util.Optional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface CommitToPushRunRepository extends JpaRepository<CommitToPushRun, Long> {
    Optional<CommitToPushRun> findByCommitId(Long commitId);
}
