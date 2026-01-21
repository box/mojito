package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectTextUnitDecisionRepository
    extends JpaRepository<ReviewProjectTextUnitDecision, Long> {

  long countByReviewProjectTextUnit_ReviewProject_Id(Long reviewProjectId);

  long countByVariantIsNotNullAndReviewProjectTextUnit_ReviewProject_Id(Long reviewProjectId);

  List<ReviewProjectTextUnitDecision> findByReviewProjectTextUnit_ReviewProject_Id(
      Long reviewProjectId);

  Optional<ReviewProjectTextUnitDecision> findByReviewProjectTextUnitId(
      Long reviewProjectTextUnitId);
}
