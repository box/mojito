package com.box.l10n.mojito.apiclient;

import static java.util.Optional.ofNullable;

import com.box.l10n.mojito.apiclient.model.AssetIntegrityChecker;
import com.box.l10n.mojito.apiclient.model.AssetIntegrityCheckerRepository;
import com.box.l10n.mojito.apiclient.model.Locale;
import com.box.l10n.mojito.apiclient.model.LocaleRepository;
import com.box.l10n.mojito.apiclient.model.Repository;
import com.box.l10n.mojito.apiclient.model.RepositoryLocale;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleStatistic;
import com.box.l10n.mojito.apiclient.model.RepositoryLocaleStatisticRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryRepository;
import com.box.l10n.mojito.apiclient.model.RepositoryStatistic;
import com.box.l10n.mojito.apiclient.model.RepositoryStatisticRepository;

public class RepositoryMapper {
  public static Locale mapToLocale(LocaleRepository localeRepository) {
    Locale locale = new Locale();
    locale.setId(localeRepository.getId());
    locale.setBcp47Tag(localeRepository.getBcp47Tag());
    return locale;
  }

  public static RepositoryLocale mapToRepositoryLocale(
      RepositoryLocaleRepository repositoryLocaleRepository) {
    RepositoryLocale repositoryLocale = new RepositoryLocale();
    repositoryLocale.setId(repositoryLocaleRepository.getId());
    repositoryLocale.setLocale(
        ofNullable(repositoryLocaleRepository.getLocale())
            .map(RepositoryMapper::mapToLocale)
            .orElse(null));
    repositoryLocale.setToBeFullyTranslated(repositoryLocaleRepository.isToBeFullyTranslated());
    repositoryLocale.setParentLocale(
        ofNullable(repositoryLocaleRepository.getParentLocale())
            .map(RepositoryMapper::mapToRepositoryLocale)
            .orElse(null));
    return repositoryLocale;
  }

  public static AssetIntegrityChecker mapToAssetIntegrityChecker(
      AssetIntegrityCheckerRepository assetIntegrityCheckerRepository) {
    AssetIntegrityChecker assetIntegrityChecker = new AssetIntegrityChecker();
    assetIntegrityChecker.setId(assetIntegrityCheckerRepository.getId());
    assetIntegrityChecker.setAssetExtension(assetIntegrityCheckerRepository.getAssetExtension());
    assetIntegrityChecker.setIntegrityCheckerType(
        ofNullable(assetIntegrityCheckerRepository.getIntegrityCheckerType())
            .map(
                integrityCheckerType ->
                    AssetIntegrityChecker.IntegrityCheckerTypeEnum.fromValue(
                        integrityCheckerType.name()))
            .orElse(null));
    return assetIntegrityChecker;
  }

  public static RepositoryLocaleStatistic mapToRepositoryLocaleStatistic(
      RepositoryLocaleStatisticRepository repositoryLocaleStatisticRepository) {
    RepositoryLocaleStatistic repositoryLocaleStatistic = new RepositoryLocaleStatistic();
    repositoryLocaleStatistic.setLocale(
        ofNullable(repositoryLocaleStatisticRepository.getLocale())
            .map(RepositoryMapper::mapToLocale)
            .orElse(null));
    repositoryLocaleStatistic.setForTranslationCount(
        repositoryLocaleStatisticRepository.getForTranslationCount());
    return repositoryLocaleStatistic;
  }

  public static RepositoryStatistic mapToRepositoryStatistic(
      RepositoryStatisticRepository repositoryStatisticRepository) {
    RepositoryStatistic repositoryStatistic = new RepositoryStatistic();
    repositoryStatistic.setRepositoryLocaleStatistics(
        ofNullable(repositoryStatisticRepository.getRepositoryLocaleStatistics())
            .map(
                repositoryLocaleStatistics ->
                    repositoryLocaleStatistics.stream()
                        .map(RepositoryMapper::mapToRepositoryLocaleStatistic)
                        .toList())
            .orElse(null));
    return repositoryStatistic;
  }

  public static Repository mapToRepository(RepositoryRepository repositoryRepository) {
    Repository repository = new Repository();
    repository.setId(repositoryRepository.getId());
    repository.setName(repositoryRepository.getName());
    repository.setDescription(repositoryRepository.getDescription());
    repository.setDeleted(repositoryRepository.isDeleted());
    repository.setCheckSLA(repositoryRepository.isCheckSLA());
    repository.setSourceLocale(
        ofNullable(repositoryRepository.getSourceLocale())
            .map(RepositoryMapper::mapToLocale)
            .orElse(null));
    repository.setRepositoryLocales(
        ofNullable(repositoryRepository.getRepositoryLocales())
            .map(
                repositoryLocales ->
                    repositoryLocales.stream()
                        .map(RepositoryMapper::mapToRepositoryLocale)
                        .toList())
            .orElse(null));
    repository.setAssetIntegrityCheckers(
        ofNullable(repositoryRepository.getAssetIntegrityCheckers())
            .map(
                assetIntegrityCheckers ->
                    assetIntegrityCheckers.stream()
                        .map(RepositoryMapper::mapToAssetIntegrityChecker)
                        .toList())
            .orElse(null));
    repository.setRepositoryStatistic(
        ofNullable(repositoryRepository.getRepositoryStatistic())
            .map(RepositoryMapper::mapToRepositoryStatistic)
            .orElse(null));
    return repository;
  }
}
