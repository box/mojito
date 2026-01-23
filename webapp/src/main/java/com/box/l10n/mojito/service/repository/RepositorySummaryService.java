package com.box.l10n.mojito.service.repository;

import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.rest.repository.RepositorySummaryResponse;
import com.box.l10n.mojito.rest.repository.RepositorySummaryResponse.AssetIntegrityCheckerSummary;
import com.box.l10n.mojito.rest.repository.RepositorySummaryResponse.LocaleSummary;
import com.box.l10n.mojito.rest.repository.RepositorySummaryResponse.RepositoryLocaleStatisticSummary;
import com.box.l10n.mojito.rest.repository.RepositorySummaryResponse.RepositoryLocaleSummary;
import com.box.l10n.mojito.rest.repository.RepositorySummaryResponse.RepositoryStatisticSummary;
import com.box.l10n.mojito.service.assetintegritychecker.AssetIntegrityCheckerRepository;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.repository.statistics.RepositoryLocaleStatisticRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RepositorySummaryService {

  /**
   * Builds {@link RepositorySummaryResponse}, which is the WS DTO, for simplicity in the service
   * layer. We can introduce an additional set of service DTOs later if needed.
   */
  @Autowired RepositoryRepository repositoryRepository;

  @Autowired RepositoryLocaleRepository repositoryLocaleRepository;

  @Autowired RepositoryLocaleStatisticRepository repositoryLocaleStatisticRepository;

  @Autowired AssetIntegrityCheckerRepository assetIntegrityCheckerRepository;

  @Autowired LocaleService localeService;

  /**
   * Builds {@link RepositorySummaryResponse}, which is the WS DTO, for simplicity in the service
   * layer. We can introduce an additional set of service DTOs later if needed.
   */
  public List<RepositorySummaryResponse> getRepositorySummaries() {
    List<RepositorySummaryRow> repositoryRows = repositoryRepository.findRepositorySummaryRows();
    if (repositoryRows.isEmpty()) {
      return List.of();
    }

    List<Long> repositoryIds =
        repositoryRows.stream().map(RepositorySummaryRow::repositoryId).toList();

    List<RepositoryLocaleRow> repositoryLocaleRows =
        repositoryLocaleRepository.findRowsByRepositoryIdIn(repositoryIds);
    Map<Long, List<RepositoryLocaleRow>> repositoryLocalesByRepositoryId =
        repositoryLocaleRows.stream()
            .collect(Collectors.groupingBy(RepositoryLocaleRow::repositoryId));
    Map<Long, RepositoryLocaleRow> repositoryLocaleById =
        repositoryLocaleRows.stream()
            .collect(Collectors.toMap(RepositoryLocaleRow::id, row -> row));

    List<Long> repositoryStatisticIds =
        repositoryRows.stream()
            .map(RepositorySummaryRow::repositoryStatisticId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    List<RepositoryLocaleStatisticRow> repositoryLocaleStatisticRows =
        repositoryStatisticIds.isEmpty()
            ? List.of()
            : repositoryLocaleStatisticRepository.findRowsByRepositoryStatisticIdIn(
                repositoryStatisticIds);
    Map<Long, List<RepositoryLocaleStatisticRow>> repositoryLocaleStatisticsByStatisticId =
        repositoryLocaleStatisticRows.stream()
            .collect(Collectors.groupingBy(RepositoryLocaleStatisticRow::repositoryStatisticId));

    List<AssetIntegrityCheckerRow> assetIntegrityCheckerRows =
        assetIntegrityCheckerRepository.findRowsByRepositoryIdIn(repositoryIds);
    Map<Long, List<AssetIntegrityCheckerRow>> assetIntegrityCheckerByRepositoryId =
        assetIntegrityCheckerRows.stream()
            .collect(Collectors.groupingBy(AssetIntegrityCheckerRow::repositoryId));

    List<RepositorySummaryResponse> response = new ArrayList<>(repositoryRows.size());

    for (RepositorySummaryRow repositoryRow : repositoryRows) {
      Long repositoryId = repositoryRow.repositoryId();
      List<RepositoryLocaleSummary> repositoryLocales =
          repositoryLocalesByRepositoryId.getOrDefault(repositoryId, List.of()).stream()
              .map(row -> toRepositoryLocaleSummary(row, repositoryLocaleById))
              .toList();

      List<RepositoryLocaleStatisticSummary> repositoryLocaleStatistics =
          repositoryLocaleStatisticsByStatisticId
              .getOrDefault(repositoryRow.repositoryStatisticId(), List.of())
              .stream()
              .map(this::toRepositoryLocaleStatisticSummary)
              .toList();

      RepositoryStatisticSummary repositoryStatistic =
          new RepositoryStatisticSummary(
              repositoryRow.repositoryStatisticId(),
              repositoryRow.repositoryStatisticCreatedDate(),
              repositoryRow.usedTextUnitCount(),
              repositoryRow.usedTextUnitWordCount(),
              repositoryRow.pluralTextUnitCount(),
              repositoryRow.pluralTextUnitWordCount(),
              repositoryRow.ooslaTextUnitCount(),
              repositoryRow.ooslaTextUnitWordCount(),
              repositoryRow.ooslaCreatedBefore(),
              repositoryLocaleStatistics);

      List<AssetIntegrityCheckerSummary> assetIntegrityCheckers =
          assetIntegrityCheckerByRepositoryId.getOrDefault(repositoryId, List.of()).stream()
              .map(
                  row ->
                      new AssetIntegrityCheckerSummary(
                          row.id(), row.assetExtension(), row.integrityCheckerType()))
              .toList();

      RepositorySummaryResponse repositoryResponse =
          new RepositorySummaryResponse(
              repositoryRow.repositoryId(),
              repositoryRow.repositoryCreatedDate(),
              repositoryRow.repositoryName(),
              repositoryRow.repositoryDescription(),
              toLocaleSummary(repositoryRow.sourceLocaleId()),
              repositoryLocales,
              repositoryStatistic,
              assetIntegrityCheckers,
              repositoryRow.checkSLA());

      response.add(repositoryResponse);
    }

    return response;
  }

  private LocaleSummary toLocaleSummary(Long localeId) {
    Locale locale = localeService.findById(localeId);
    return new LocaleSummary(locale.getId(), locale.getBcp47Tag());
  }

  private RepositoryLocaleSummary toRepositoryLocaleSummary(
      RepositoryLocaleRow row, Map<Long, RepositoryLocaleRow> repositoryLocaleById) {
    if (row == null) {
      return null;
    }

    RepositoryLocaleSummary parentLocale =
        toRepositoryLocaleSummary(
            repositoryLocaleById.get(row.parentLocaleId()), repositoryLocaleById);
    return new RepositoryLocaleSummary(
        row.id(), toLocaleSummary(row.localeId()), row.toBeFullyTranslated(), parentLocale);
  }

  private RepositoryLocaleStatisticSummary toRepositoryLocaleStatisticSummary(
      RepositoryLocaleStatisticRow row) {
    return new RepositoryLocaleStatisticSummary(
        row.id(),
        toLocaleSummary(row.localeId()),
        row.translatedCount(),
        row.translatedWordCount(),
        row.translationNeededCount(),
        row.translationNeededWordCount(),
        row.reviewNeededCount(),
        row.reviewNeededWordCount(),
        row.includeInFileCount(),
        row.includeInFileWordCount(),
        row.diffToSourcePluralCount(),
        row.forTranslationCount(),
        row.forTranslationWordCount());
  }

  // Row projections are top-level records in this package to keep JPQL constructor expressions
  // simple.
}
