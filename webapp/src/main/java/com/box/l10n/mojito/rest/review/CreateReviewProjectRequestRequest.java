package com.box.l10n.mojito.rest.review;

import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record CreateReviewProjectRequestRequest(
    List<String> localeTags,
    String notes,
    List<Long> tmTextUnitIds,
    ReviewProjectType type,
    ZonedDateTime dueDate,
    List<String> screenshotImageIds,
    String name) {}
