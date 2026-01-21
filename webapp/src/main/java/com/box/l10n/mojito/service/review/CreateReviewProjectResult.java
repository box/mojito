package com.box.l10n.mojito.service.review;

import java.time.ZonedDateTime;
import java.util.List;

public record CreateReviewProjectResult(
    Long requestId,
    String requestUuid,
    String requestName,
    List<String> localeTags,
    ZonedDateTime dueDate,
    List<Long> projectIds) {}
