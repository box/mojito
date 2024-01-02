package com.box.l10n.mojito.service.pullrun;

import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.PullRunTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import java.time.ZonedDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

/** @author garion */
@RepositoryRestResource(exported = false)
public interface PullRunTextUnitVariantRepository
    extends JpaRepository<PullRunTextUnitVariant, Long> {

  List<PullRunTextUnitVariant> findByPullRunAsset(PullRunAsset pullRunAsset, Pageable pageable);

  List<PullRunTextUnitVariant> findByTmTextUnitVariant_TmTextUnitIdAndLocaleId(
      Long tmTextUnitId, Long localeId);

  @Query(
      "select prtuv.tmTextUnitVariant from PullRunTextUnitVariant prtuv "
          + "inner join prtuv.pullRunAsset pra "
          + "inner join pra.pullRun pr where pr = :pullRun")
  List<TMTextUnitVariant> findByPullRun(@Param("pullRun") PullRun pullRun, Pageable pageable);

  @Transactional
  void deleteByPullRunAsset(PullRunAsset pullRunAsset);

  @Transactional
  @Modifying
  @Query(
      nativeQuery = true,
      value =
          "delete pull_run_text_unit_variant "
              + "from pull_run_text_unit_variant "
              + "join (select prtuv.id as id "
              + "  from pull_run pr "
              + "  join pull_run_asset pra on pra.pull_run_id = pr.id "
              + "  join pull_run_text_unit_variant prtuv on prtuv.pull_run_asset_id = pra.id "
              + "  where pr.created_date < :beforeDate "
              + "  limit :batchSize "
              + ") todelete on todelete.id = pull_run_text_unit_variant.id")
  int deleteAllByPullRunWithCreatedDateBefore(
      @Param("beforeDate") ZonedDateTime beforeDate, @Param("batchSize") int batchSize);
}
