package com.box.l10n.mojito.service.repository;

import java.time.ZonedDateTime;

public record RepositorySummaryRow(
    Long repositoryId,
    ZonedDateTime repositoryCreatedDate,
    String repositoryName,
    String repositoryDescription,
    Long sourceLocaleId,
    Boolean checkSLA,
    Long repositoryStatisticId,
    ZonedDateTime repositoryStatisticCreatedDate,
    Long usedTextUnitCount,
    Long usedTextUnitWordCount,
    Long pluralTextUnitCount,
    Long pluralTextUnitWordCount,
    Long ooslaTextUnitCount,
    Long ooslaTextUnitWordCount,
    ZonedDateTime ooslaCreatedBefore) {}
