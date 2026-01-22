package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectTextUnitRepository
    extends JpaRepository<ReviewProjectTextUnit, Long> {

  @EntityGraph(
      attributePaths = {
        "tmTextUnit",
        "tmTextUnit.asset",
        "tmTextUnitVariant",
        "reviewProject",
        "reviewProject.locale"
      })
  List<ReviewProjectTextUnit> findByReviewProjectIdOrderByIdAsc(Long reviewProjectId);

  Optional<ReviewProjectTextUnit> findByReviewProjectIdAndTmTextUnitVariantId(
      Long reviewProjectId, Long tmTextUnitVariantId);

  long countByReviewProjectId(Long reviewProjectId);
}
