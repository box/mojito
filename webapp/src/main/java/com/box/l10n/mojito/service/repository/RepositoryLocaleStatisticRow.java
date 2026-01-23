package com.box.l10n.mojito.service.repository;

public record RepositoryLocaleStatisticRow(
    Long id,
    Long repositoryStatisticId,
    Long localeId,
    Long translatedCount,
    Long translatedWordCount,
    Long translationNeededCount,
    Long translationNeededWordCount,
    Long reviewNeededCount,
    Long reviewNeededWordCount,
    Long includeInFileCount,
    Long includeInFileWordCount,
    Long diffToSourcePluralCount,
    Long forTranslationCount,
    Long forTranslationWordCount) {}
