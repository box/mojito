package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;

public record ReviewProjectDetail(
    Long id,
    ReviewProjectType type,
    ReviewProjectStatus status,
    ZonedDateTime createdDate,
    ZonedDateTime dueDate,
    String closeReason,
    Integer textUnitCount,
    Integer wordCount,
    Long localeId,
    String localeTag,
    Long reviewProjectRequestId,
    String reviewProjectRequestName) {}
