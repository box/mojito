package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectRequestRepository extends JpaRepository<ReviewProjectRequest, Long> {

  @Query(
      """
      select rpr.id
      from ReviewProjectRequest rpr
      where rpr.id in :requestIds
        and not exists (
          select rp.id from ReviewProject rp where rp.reviewProjectRequest = rpr
        )
      """)
  List<Long> findOrphanRequestIds(@Param("requestIds") List<Long> requestIds);
}
