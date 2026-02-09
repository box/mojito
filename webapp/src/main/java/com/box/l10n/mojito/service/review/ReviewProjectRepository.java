package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectRepository extends JpaRepository<ReviewProject, Long> {

  @Query(
      """
      select new com.box.l10n.mojito.service.review.ReviewProjectDetail(
        rp.id,
        rp.type,
        rp.status,
        rp.createdDate,
        rp.dueDate,
        rp.closeReason,
        rp.textUnitCount,
        rp.wordCount,
        locale.id,
        locale.bcp47Tag,
        request.id,
        request.name
      )
      from ReviewProject rp
      left join rp.locale locale
      left join rp.reviewProjectRequest request
      where rp.id = :id
      """)
  Optional<ReviewProjectDetail> findDetailById(@Param("id") Long id);

  @Query(
      """
      select distinct request.id
      from ReviewProject rp
      join rp.reviewProjectRequest request
      where rp.id in :projectIds
      """)
  List<Long> findRequestIdsByProjectIds(@Param("projectIds") List<Long> projectIds);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      """
      delete from ReviewProject rp
      where rp.id in :projectIds
      """)
  int deleteByProjectIds(@Param("projectIds") List<Long> projectIds);
}
