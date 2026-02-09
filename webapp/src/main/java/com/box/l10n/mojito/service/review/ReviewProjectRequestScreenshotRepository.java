package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectRequestScreenshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectRequestScreenshotRepository
    extends JpaRepository<ReviewProjectRequestScreenshot, Long> {

  @Query(
      "select s.imageName from ReviewProjectRequestScreenshot s where s.reviewProjectRequest.id = ?1")
  List<String> findImageNamesByReviewProjectRequestId(Long reviewProjectRequestId);

  void deleteByReviewProjectRequestIdIn(List<Long> reviewProjectRequestIds);
}
