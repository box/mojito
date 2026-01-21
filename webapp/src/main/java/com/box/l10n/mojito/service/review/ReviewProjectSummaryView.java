package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record ReviewProjectSummaryView(
    Long id,
    ZonedDateTime createdDate,
    ZonedDateTime dueDate,
    String closeReason,
    Integer textUnitCount,
    Integer wordCount,
    ReviewProjectType type,
    ReviewProjectStatus status,
    Long requestId,
    String requestUuid,
    String requestName,
    int totalSelected,
    long acceptedCount,
    String name,
    List<ReviewProjectRepositorySummaryView> repositories,
    List<ReviewProjectLocaleSummaryView> locales,
    List<String> screenshotImageIds) {}
