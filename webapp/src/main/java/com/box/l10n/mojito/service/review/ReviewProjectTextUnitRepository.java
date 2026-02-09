package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectTextUnitRepository
    extends JpaRepository<ReviewProjectTextUnit, Long> {

  @Query(
      """
      select new com.box.l10n.mojito.service.review.ReviewProjectTextUnitDetail(
        rptu.id,
        ttu.id,
        ttu.name,
        ttu.content,
        ttu.comment,
        ttu.wordCount,
        asset.path,
        repo.id,
        repo.name,
        ttuv.id,
        ttuv.content,
        ttuv.status,
        ttuv.includedInLocalizedFile,
        ttuv.comment,
        currentTtuv.id,
        currentTtuv.content,
        currentTtuv.status,
        currentTtuv.includedInLocalizedFile,
        currentTtuv.comment,
        decisionTtuv.id,
        decisionTtuv.content,
        decisionTtuv.status,
        decisionTtuv.includedInLocalizedFile,
        decisionTtuv.comment,
        rptud.reviewedVariant.id,
        rptud.notes,
        rptud.decisionState
      )
      from ReviewProjectTextUnit rptu
      join rptu.reviewProject rp
      join rptu.tmTextUnit ttu
      join ttu.asset asset
      join asset.repository repo
      left join rptu.tmTextUnitVariant ttuv
      left join TMTextUnitCurrentVariant ttucv
        on ttucv.tmTextUnit = ttu
       and ttucv.locale = rp.locale
      left join ttucv.tmTextUnitVariant currentTtuv
      left join ReviewProjectTextUnitDecision rptud
        on rptud.reviewProjectTextUnit = rptu
      left join rptud.decisionVariant decisionTtuv
      where rp.id = :reviewProjectId
      order by rptu.id
      """)
  List<ReviewProjectTextUnitDetail> findDetailByReviewProjectId(
      @Param("reviewProjectId") Long reviewProjectId);

  @Query(
      """
      select new com.box.l10n.mojito.service.review.ReviewProjectTextUnitDetail(
        rptu.id,
        ttu.id,
        ttu.name,
        ttu.content,
        ttu.comment,
        ttu.wordCount,
        asset.path,
        repo.id,
        repo.name,
        ttuv.id,
        ttuv.content,
        ttuv.status,
        ttuv.includedInLocalizedFile,
        ttuv.comment,
        currentTtuv.id,
        currentTtuv.content,
        currentTtuv.status,
        currentTtuv.includedInLocalizedFile,
        currentTtuv.comment,
        decisionTtuv.id,
        decisionTtuv.content,
        decisionTtuv.status,
        decisionTtuv.includedInLocalizedFile,
        decisionTtuv.comment,
        rptud.reviewedVariant.id,
        rptud.notes,
        rptud.decisionState
      )
      from ReviewProjectTextUnit rptu
      join rptu.reviewProject rp
      join rptu.tmTextUnit ttu
      join ttu.asset asset
      join asset.repository repo
      left join rptu.tmTextUnitVariant ttuv
      left join TMTextUnitCurrentVariant ttucv
        on ttucv.tmTextUnit = ttu
       and ttucv.locale = rp.locale
      left join ttucv.tmTextUnitVariant currentTtuv
      left join ReviewProjectTextUnitDecision rptud
        on rptud.reviewProjectTextUnit = rptu
      left join rptud.decisionVariant decisionTtuv
      where rptu.id = :reviewProjectTextUnitId
      """)
  Optional<ReviewProjectTextUnitDetail> findDetailByReviewProjectTextUnitId(
      @Param("reviewProjectTextUnitId") Long reviewProjectTextUnitId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      delete from ReviewProjectTextUnit rptu
      where rptu.reviewProject.id in :projectIds
      """)
  int deleteByReviewProjectIds(@Param("projectIds") List<Long> projectIds);
}
