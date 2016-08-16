package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.List;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface PollableTaskRepository extends JpaRepository<PollableTask, Long> {

    /**
     * Retrieves pollable tasks that have not finished yet and
     * have exceeded the maximum execution time.
     */
    @Query(
            "select pt from #{#entityName} pt " +
            "where pt.finishedDate is null " +
            "and (unix_timestamp(pt.createdDate) + pt.timeout) < unix_timestamp()"
    )
    List<PollableTask> findZombiePollableTasks(Pageable pageable);
}
