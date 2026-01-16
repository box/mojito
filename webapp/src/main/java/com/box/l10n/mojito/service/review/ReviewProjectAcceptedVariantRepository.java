package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectAcceptedVariant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectAcceptedVariantRepository
    extends JpaRepository<ReviewProjectAcceptedVariant, Long> {

  long countByReviewProjectId(Long reviewProjectId);

  java.util.List<ReviewProjectAcceptedVariant> findByReviewProjectId(Long reviewProjectId);

  Optional<ReviewProjectAcceptedVariant> findByReviewProjectIdAndTmTextUnitVariantId(
      Long reviewProjectId, Long tmTextUnitVariantId);

  Optional<ReviewProjectAcceptedVariant> findByReviewProjectTextUnitId(
      Long reviewProjectTextUnitId);
}
