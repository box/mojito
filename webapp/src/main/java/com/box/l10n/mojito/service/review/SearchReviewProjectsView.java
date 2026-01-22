package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record SearchReviewProjectsView(List<ReviewProject> reviewProject) {

  public record ReviewProject(
      Long id,
      ZonedDateTime createdDate,
      ZonedDateTime lastModifiedDate,
      ZonedDateTime dueDate,
      String closeReason,
      Integer textUnitCount,
      Integer wordCount,
      ReviewProjectType type,
      ReviewProjectStatus status,
      Long localeId,
      ReviewProjectRequest reviewProjectRequest) {}

  public record ReviewProjectRequest(Long id, String name) {}
}
