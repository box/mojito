package com.box.l10n.mojito.service.review;

import java.time.ZonedDateTime;

public record ReviewProjectTextUnitView(
    Long reviewProjectTextUnitId,
    Long tmTextUnitId,
    Long tmTextUnitVariantId,
    String name,
    String source,
    String target,
    String currentTarget,
    String status,
    String baselineStatus,
    String reviewStatus,
    String notes,
    ZonedDateTime reviewedAt,
    String reviewedBy,
    Long repositoryId,
    String repositoryName,
    String assetPath,
    boolean includedInLocalizedFile) {}
