package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;

public record SearchReviewProjectDetail(
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
    String localeTag,
    Long requestId,
    String requestName) {}
