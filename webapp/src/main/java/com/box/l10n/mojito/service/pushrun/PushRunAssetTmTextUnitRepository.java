package com.box.l10n.mojito.service.pushrun;

import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.PushRunAsset;
import com.box.l10n.mojito.entity.PushRunAssetTmTextUnit;
import com.box.l10n.mojito.entity.TMTextUnit;
import java.time.ZonedDateTime;
import java.util.List;
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
public interface PushRunAssetTmTextUnitRepository
    extends JpaRepository<PushRunAssetTmTextUnit, Long> {

  @Transactional
  void deleteByPushRunAsset(PushRunAsset pushRunAsset);

  List<PushRunAssetTmTextUnit> findByPushRunAsset(PushRunAsset pushRunAsset, Pageable pageable);

  @Query(
      """
      select prattu.tmTextUnit from PushRunAssetTmTextUnit prattu
      inner join prattu.pushRunAsset pra
      inner join pra.pushRun pr where pr = :pushRun
      """)
  List<TMTextUnit> findByPushRun(@Param("pushRun") PushRun pushRun, Pageable pageable);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete push_run_asset_tm_text_unit
          from push_run_asset_tm_text_unit
            join (select prattu.id as id
              from push_run pr
                join push_run_asset pra on pra.push_run_id = pr.id
                join push_run_asset_tm_text_unit prattu on prattu.push_run_asset_id = pra.id
              where pr.created_date < :beforeDate
              limit :batchSize
            ) todelete on todelete.id = push_run_asset_tm_text_unit.id
          """)
  int deleteAllByPushRunWithCreatedDateBefore(
      @Param("beforeDate") ZonedDateTime beforeDate, @Param("batchSize") int batchSize);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          """
          delete push_run_asset_tm_text_unit
            from push_run_asset_tm_text_unit
            join (select prattu.id as id
                    from push_run pr
                    join push_run_asset pra
                      on pra.push_run_id = pr.id
                    join push_run_asset_tm_text_unit prattu
                      on prattu.push_run_asset_id = pra.id
                   where pr.created_date between :startDate and :endDate
                     and pr.id not in :latestPushRunIdsPerAsset
                   limit :batchSize) todelete
              on todelete.id = push_run_asset_tm_text_unit.id
          """)
  int deleteByPushRunsNotLatestPerAsset(
      @Param("startDate") ZonedDateTime startDate,
      @Param("endDate") ZonedDateTime endDate,
      @Param("latestPushRunIdsPerAsset") List<Long> latestPushRunIdsPerAsset,
      @Param("batchSize") int batchSize);
}
