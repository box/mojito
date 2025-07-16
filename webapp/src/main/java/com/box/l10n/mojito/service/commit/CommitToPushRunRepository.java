package com.box.l10n.mojito.service.commit;

import com.box.l10n.mojito.entity.CommitToPushRun;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author garion
 */
@RepositoryRestResource(exported = false)
public interface CommitToPushRunRepository extends JpaRepository<CommitToPushRun, Long> {
  Optional<CommitToPushRun> findByCommitId(Long commitId);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete ctpr
          from push_run pr
          join commit_to_push_run ctpr on ctpr.push_run_id = pr.id
          where pr.created_date < :beforeDate
          """)
  void deleteAllByPushRunWithCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from CommitToPushRun
           where pushRun.createdDate between :startDate and :endDate
             and pushRun.id not in :latestPushRunIdsPerAsset
          """)
  void deleteByPushRunsNotLatestPerAsset(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      @Param("latestPushRunIdsPerAsset") List<Long> latestPushRunIdsPerAsset);
}
