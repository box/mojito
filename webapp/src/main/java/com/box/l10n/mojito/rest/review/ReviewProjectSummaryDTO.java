package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record ReviewProjectSummaryDTO(
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
    List<ReviewProjectRepositorySummaryDTO> repositories,
    List<ReviewProjectLocaleSummaryDTO> locales,
    List<String> screenshotImageIds) {}
