package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PushRun;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.service.ai.translation.LocaleVariantDTO;
import com.google.common.annotations.VisibleForTesting;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface TMTextUnitVariantRepository extends JpaRepository<TMTextUnitVariant, Long> {

  @VisibleForTesting
  @EntityGraph(value = "TMTextUnitVariant.legacy", type = EntityGraph.EntityGraphType.FETCH)
  List<TMTextUnitVariant> findAllByLocale_IdAndTmTextUnit_Tm_id(Long localeId, Long tmId);

  @EntityGraph(value = "TMTextUnitVariant.legacy", type = EntityGraph.EntityGraphType.FETCH)
  List<TMTextUnitVariant> findAllByTmTextUnit_IdAndLocale_IdOrderByCreatedDateDesc(
      Long tmTextUnitId, Long localeId);

  TMTextUnitVariant findTopByTmTextUnitTmIdOrderByCreatedDateDesc(Long tmId);

  @VisibleForTesting
  @EntityGraph(value = "TMTextUnitVariant.legacy", type = EntityGraph.EntityGraphType.FETCH)
  List<TMTextUnitVariant> findByTmTextUnitTmRepositoriesOrderByContent(Repository repository);

  @VisibleForTesting
  @EntityGraph(value = "TMTextUnitVariant.legacy", type = EntityGraph.EntityGraphType.FETCH)
  List<TMTextUnitVariant> findByTmTextUnitTmRepositoriesAndLocale_Bcp47TagNotOrderByContent(
      Repository repository, String bcp47Tag);

  /**
   * Gets all {@link TMTextUnitVariant} entities that are used across any branch, filtered down by
   * the repository and a list of locales, as well as a date range. Only entities linked to
   * non-deleted assets are included.
   *
   * <p>To note: unused text units are not included, as if a text unit isn't used in at least one
   * branch - it's not useful as this method isn't meant to support different delta snapshots at a
   * specific time for all active branches.
   */
  @Query(
      value =
          """
          select distinct new com.box.l10n.mojito.service.tm.TextUnitVariantDeltaDTO(
             tuv.id,
             tu.name,
             tuv.locale.bcp47Tag,
             tuv.content,
             'UNKNOWN')
          from TMTextUnitVariant tuv
          inner join TMTextUnitCurrentVariant tucv on tucv.tmTextUnitVariant = tuv
          inner join TMTextUnit tu on tu = tucv.tmTextUnit
          inner join AssetTextUnitToTMTextUnit map on map.tmTextUnit = tu
          inner join Asset a on a = tu.asset and a.lastSuccessfulAssetExtraction = map.assetExtraction
          where a.repository = :repository
          and a.deleted = false
          and tuv.locale in :locales
          and tuv.includedInLocalizedFile = true
          and tucv.lastModifiedDate >= :fromDate
          and tucv.lastModifiedDate < :toDate
          order by tuv.id asc
          """)
  Page<TextUnitVariantDeltaDTO> findAllUsedForRepositoryAndLocalesInDateRange(
      @Param("repository") Repository repository,
      @Param("locales") List<Locale> locales,
      @Param("fromDate") ZonedDateTime fromDate,
      @Param("toDate") ZonedDateTime toDate,
      Pageable pageable);

  /**
   * Gets all {@link TMTextUnitVariant} entities for the {@link
   * com.box.l10n.mojito.entity.TMTextUnit} associated with the provided {@link PushRun} that have
   * different values than the {@link TMTextUnitVariant} entities from the associated {@link
   * PullRun}.
   *
   * <p>Additional filtering done by {@link Repository} ID and a list of {@link Locale} IDs.
   *
   * <p>To note: unused text units and deleted assets are also included, as this method is indented
   * to provided deltas relevant for a specific snapshot.
   *
   * @param translationsFromDate A java.sql.Date type needs to be provided for native queries,
   *     otherwise, if Joda Time's ZonedDateTime is used, that parameter is passed in as a VARBINARY
   *     to the SQL query and will silently mis-behave.
   */
  @Query(
      nativeQuery = true,
      value =
          """
          select distinct base_tu.name as textUnitName,
             l.bcp47_tag as bcp47Tag,
             latest_tuv.content as content,
             case
                when previous_tuv.id is null then 'NEW_TRANSLATION'
                else 'UPDATED_TRANSLATION'
             end as deltaType
          from tm_text_unit_variant latest_tuv
          inner join tm_text_unit_current_variant base_tucv on base_tucv.tm_text_unit_variant_id = latest_tuv.id
          inner join tm_text_unit base_tu on base_tu.id = base_tucv.tm_text_unit_id
          inner join push_run_asset_tm_text_unit prattu on prattu.tm_text_unit_id = base_tu.id
          inner join push_run_asset pra on pra.id = prattu.push_run_asset_id
          inner join push_run pr on pr.id = pra.push_run_id and pr.id in :pushRunIds
          inner join asset a on a.id = base_tu.asset_id
          inner join repository r on r.id = a.repository_id and r.id = :repositoryId
          inner join locale l on l.id = latest_tuv.locale_id and l.id in :localeIds
          left outer join (
             select distinct previous_tuv.id,
                             previous_tuv.content_md5,
                             a.id as asset_id,
                             l.id as locale_id,
                             tu.id as text_unit_id
             from tm_text_unit_variant previous_tuv
                 inner join pull_run_text_unit_variant prtuv on prtuv.tm_text_unit_variant_id = previous_tuv.id
                 inner join pull_run_asset pra on pra.id = prtuv.pull_run_asset_id
                 inner join pull_run pr on pr.id = pra.pull_run_id and pr.id in :pullRunIds
                 inner join tm_text_unit tu on tu.id = previous_tuv.tm_text_unit_id
                 inner join asset a on a.id = tu.asset_id
                 inner join repository r on r.id = a.repository_id and r.id = :repositoryId
                 inner join locale l on l.id = previous_tuv.locale_id and l.id in :localeIds
           ) as previous_tuv on previous_tuv.asset_id = a.id
                             and previous_tuv.locale_id = l.id
                             and previous_tuv.text_unit_id = base_tu.id
          where
             latest_tuv.included_in_localized_file = true
             and base_tucv.last_modified_date >= :translationsFromDate
             and (previous_tuv.id is null
                 or (latest_tuv.id != previous_tuv.id and latest_tuv.content_md5 != previous_tuv.content_md5))
          """)
  List<TextUnitVariantDelta> findDeltasForRuns(
      @Param("repositoryId") Long repositoryId,
      @Param("localeIds") List<Long> localeIds,
      @Param("pushRunIds") List<Long> pushRunIds,
      @Param("pullRunIds") List<Long> pullRunIds,
      @Param("translationsFromDate") ZonedDateTime translationsFromDate);

  @Query(
      """
          select l from TMTextUnitVariant ttuv
          join ttuv.locale l
          where ttuv.tmTextUnit.id = :tmTextUnitId
          """)
  Set<Locale> findLocalesWithVariantByTmTextUnit_Id(@Param("tmTextUnitId") Long tmTextUnitId);

  @Query(
      """
              SELECT new com.box.l10n.mojito.service.ai.translation.LocaleVariantDTO(ttuv.locale.id, ttuv.id)
              FROM TMTextUnitVariant ttuv
              WHERE ttuv.tmTextUnit.id = :tmTextUnitId
              """)
  List<LocaleVariantDTO> findLocaleVariantDTOsByTmTextUnitId(
      @Param("tmTextUnitId") Long tmTextUnitId);
}
