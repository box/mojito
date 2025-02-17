package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.AiReviewProto;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * @author jaurambault
 */
@RepositoryRestResource(exported = false)
public interface AiReviewProtoRepository extends JpaRepository<AiReviewProto, Long> {

  @Query(
      "select a.tmTextUnitVariant.id "
          + "from AiReviewProto a "
          + "where a.tmTextUnitVariant.locale.id = :localeId "
          + "  and a.tmTextUnitVariant.tmTextUnit.asset.repository.id = :repositoryId")
  Set<Long> findTmTextUnitVariantIdsByLocaleIdAndRepositoryId(
      @Param("localeId") Long localeId, @Param("repositoryId") Long repositoryId);

  AiReviewProto findByTmTextUnitVariantId(Long tmTextUnitVariantId);
}
