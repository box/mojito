package com.box.l10n.mojito.service.pollableTask;

import com.box.l10n.mojito.entity.PollableTask;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface PollableTaskRepository extends JpaRepository<PollableTask, Long> {

  @Override
  @EntityGraph(value = "PollableTask.legacy", type = EntityGraphType.FETCH)
  Optional<PollableTask> findById(Long aLong);

  /**
   * Retrieves pollable tasks that have not finished yet and have exceeded the maximum execution
   * time.
   *
   * <p>Must pass "now" as parameter due to HSQL persisting ZonedDateTime without TZ info. Comparing
   * ZonedDateTime against unix_timestamp() then fails because of the TZ difference.
   *
   * <p>This does not show if test are running in UTC like on CI
   */
  @Query(
      """
      select pt from #{#entityName} pt
      where pt.finishedDate is null
      and (unix_timestamp(pt.createdDate) + pt.timeout) < unix_timestamp(:now)
      """)
  List<PollableTask> findZombiePollableTasks(@Param("now") ZonedDateTime now, Pageable pageable);
}
