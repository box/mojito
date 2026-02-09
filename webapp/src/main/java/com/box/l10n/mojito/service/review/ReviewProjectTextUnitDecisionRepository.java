package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision;
import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision.DecisionState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectTextUnitDecisionRepository
    extends JpaRepository<ReviewProjectTextUnitDecision, Long> {

  List<ReviewProjectTextUnitDecision> findByReviewProjectTextUnit_ReviewProject_Id(
      Long reviewProjectId);

  Optional<ReviewProjectTextUnitDecision> findByReviewProjectTextUnitId(
      Long reviewProjectTextUnitId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      delete from ReviewProjectTextUnitDecision d
      where d.reviewProjectTextUnit.reviewProject.id in :projectIds
      """)
  int deleteByReviewProjectIds(@Param("projectIds") List<Long> projectIds);

  @Query(
      """
      select new com.box.l10n.mojito.service.review.ReviewProjectAcceptedCountRow(
        rptu.reviewProject.id,
        count(rptud.id)
      )
      from ReviewProjectTextUnitDecision rptud
      join rptud.reviewProjectTextUnit rptu
      where rptu.reviewProject.id in :projectIds and rptud.decisionState = :decisionState
      group by rptu.reviewProject.id
      """)
  List<ReviewProjectAcceptedCountRow> countAcceptedByProjectIdsAndDecisionState(
      @Param("projectIds") List<Long> projectIds,
      @Param("decisionState") DecisionState decisionState);
}
