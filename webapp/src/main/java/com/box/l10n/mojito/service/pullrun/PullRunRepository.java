package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
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
public interface PullRunRepository extends JpaRepository<PullRun, Long> {
  Optional<PullRun> findByName(String name);

  @Query(
      value =
          """
          select p from PullRun p
          left join CommitToPullRun cpr on cpr.pullRun = p
          left join Commit c on c = cpr.commit
          where c.name in :commitNames and c.repository.id = :repositoryId
          and p.repository.id = :repositoryId order by p.createdDate desc
          """)
  List<PullRun> findLatestByCommitNames(
      @Param("commitNames") List<String> commitNames,
      @Param("repositoryId") Long repositoryId,
      Pageable pageable);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete pr
          from pull_run pr
          where pr.created_date < :beforeDate
          """)
  void deleteAllByCreatedDateBefore(@Param("beforeDate") ZonedDateTime beforeDate);

  @Query(
      """
    select MAX(pr.id) as max_id
      from PullRun pr
      join pr.pullRunAssets pra
      join (select pra2.asset as asset,
                   MAX(pr2.createdDate) as max_created_date
              from PullRun pr2
              join pr2.pullRunAssets pra2
             where pr2.createdDate between :startDate and :endDate
             group by pra2.asset) latest_pr
        on pra.asset = latest_pr.asset
       and pr.createdDate = latest_pr.max_created_date
     group by pra.asset
""")
  List<Long> getLatestPullRunIdsPerAsset(
      @Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          delete from PullRun
           where createdDate between :startDate and :endDate
             and id not in :latestPullRunIdsPerAsset
          """)
  void deletePullRunsNotLatestPerAsset(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      @Param("latestPullRunIdsPerAsset") List<Long> latestPullRunIdsPerAsset);
}
