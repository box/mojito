package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record ReviewProjectDetailView(
    Long id,
    ReviewProjectType type,
    ReviewProjectStatus status,
    ZonedDateTime createdDate,
    ZonedDateTime dueDate,
    String closeReason,
    Integer textUnitCount,
    Integer wordCount,
    String name,
    String notes,
    Long requestId,
    String requestName,
    ReviewProjectLocaleDetailView locale,
    List<ReviewProjectRepositorySummaryView> repositories,
    List<ReviewProjectLocaleDetailView> locales,
    List<String> screenshotImageIds) {}
