package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectScreenshot;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(exported = false)
public interface ReviewProjectScreenshotRepository
    extends JpaRepository<ReviewProjectScreenshot, Long> {

  @Query("select s.imageKey from ReviewProjectScreenshot s where s.reviewProjectRequest.id = ?1")
  List<String> findImageKeysByReviewProjectRequestId(Long reviewProjectRequestId);

  @Query("select s.imageKey from ReviewProjectScreenshot s where s.reviewProject.id = ?1")
  List<String> findImageKeysByReviewProjectId(Long reviewProjectId);

  @Query(
      "select s.imageKey from ReviewProjectScreenshot s where s.reviewProject.id = ?1 and s.locale.id = ?2")
  List<String> findImageKeysByReviewProjectIdAndLocaleId(Long reviewProjectId, Long localeId);

  void deleteByReviewProjectRequestId(Long reviewProjectRequestId);
}
