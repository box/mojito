package com.box.l10n.mojito.service.repository.statistics;

import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocaleStatistic;
import com.box.l10n.mojito.service.repository.RepositoryLocaleStatisticRow;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface RepositoryLocaleStatisticRepository
    extends JpaRepository<RepositoryLocaleStatistic, Long>, JpaSpecificationExecutor<Repository> {

  RepositoryLocaleStatistic findByRepositoryStatisticIdAndLocaleId(
      Long repositoryStatisticId, Long localeId);

  @EntityGraph(value = "RepositoryLocaleStatistic.legacy", type = EntityGraphType.FETCH)
  Set<RepositoryLocaleStatistic> findByRepositoryStatisticId(Long repositoryStatisticId);

  void deleteByRepositoryStatisticId(Long repositoryStatisticId);

  @Query(
      """
      select new com.box.l10n.mojito.service.repository.RepositoryLocaleStatisticRow(
        rls.id,
        rls.repositoryStatistic.id,
        rls.locale.id,
        rls.translatedCount,
        rls.translatedWordCount,
        rls.translationNeededCount,
        rls.translationNeededWordCount,
        rls.reviewNeededCount,
        rls.reviewNeededWordCount,
        rls.includeInFileCount,
        rls.includeInFileWordCount,
        rls.diffToSourcePluralCount,
        rls.forTranslationCount,
        rls.forTranslationWordCount
      )
      from RepositoryLocaleStatistic rls
      where rls.repositoryStatistic.id in :repositoryStatisticIds
      order by rls.id asc
      """)
  List<RepositoryLocaleStatisticRow> findRowsByRepositoryStatisticIdIn(
      @Param("repositoryStatisticIds") List<Long> repositoryStatisticIds);
}
