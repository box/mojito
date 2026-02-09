package com.box.l10n.mojito.rest.repository;

import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import java.time.ZonedDateTime;
import java.util.List;

public record RepositorySummaryResponse(
    Long id,
    ZonedDateTime createdDate,
    String name,
    String description,
    LocaleSummary sourceLocale,
    List<RepositoryLocaleSummary> repositoryLocales,
    RepositoryStatisticSummary repositoryStatistic,
    List<AssetIntegrityCheckerSummary> assetIntegrityCheckers,
    Boolean checkSLA) {

  public record LocaleSummary(Long id, String bcp47Tag) {}

  public record RepositoryLocaleSummary(
      Long id,
      LocaleSummary locale,
      boolean toBeFullyTranslated,
      RepositoryLocaleSummary parentLocale) {}

  public record RepositoryStatisticSummary(
      Long id,
      ZonedDateTime createdDate,
      Long usedTextUnitCount,
      Long usedTextUnitWordCount,
      Long pluralTextUnitCount,
      Long pluralTextUnitWordCount,
      Long ooslaTextUnitCount,
      Long ooslaTextUnitWordCount,
      ZonedDateTime ooslaCreatedBefore,
      List<RepositoryLocaleStatisticSummary> repositoryLocaleStatistics) {}

  public record RepositoryLocaleStatisticSummary(
      Long id,
      LocaleSummary locale,
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

  public record AssetIntegrityCheckerSummary(
      Long id, String assetExtension, IntegrityCheckerType integrityCheckerType) {}
}
