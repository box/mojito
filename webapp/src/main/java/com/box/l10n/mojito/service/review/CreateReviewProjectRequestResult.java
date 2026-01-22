package com.box.l10n.mojito.service.review;

import java.time.ZonedDateTime;
import java.util.List;

public record CreateReviewProjectRequestResult(
    Long requestId,
    String requestName,
    List<String> localeTags,
    ZonedDateTime dueDate,
    List<Long> projectIds) {}
