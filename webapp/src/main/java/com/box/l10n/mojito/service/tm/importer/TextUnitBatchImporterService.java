package com.box.l10n.mojito.service.tm.importer;

import static com.box.l10n.mojito.entity.TMTextUnitVariant.Status.APPROVED;
import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;
import static com.box.l10n.mojito.utils.Predicates.logIfFalse;

import com.box.l10n.mojito.JSR310Migration;
import com.box.l10n.mojito.aspect.StopWatch;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant.Status;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment.Severity;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.ImportTextUnitJob;
import com.box.l10n.mojito.service.asset.ImportTextUnitJobInput;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitCurrentVariantRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.TextUnitBatchMatcher;
import com.box.l10n.mojito.service.tm.TextUnitForBatchMatcher;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.textunitdtocache.TextUnitDTOsCacheService;
import com.box.l10n.mojito.service.tm.textunitdtocache.UpdateType;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.ZonedDateTime;
import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
@Component
public class TextUnitBatchImporterService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TextUnitBatchImporterService.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TMService tmService;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired AssetRepository assetRepository;

  @Autowired LocaleService localeService;

  @Autowired IntegrityCheckerFactory integrityCheckerFactory;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired TextUnitBatchMatcher textUnitBatchMatcher;

  @Autowired AuditorAwareImpl auditorAwareImpl;

  @Autowired TextUnitDTOsCacheService textUnitDTOsCacheService;

  @Autowired TMTextUnitVariantCommentService tmMTextUnitVariantCommentService;

  @Autowired MeterRegistry meterRegistry;

  @Value("${l10n.textUnitBatchImporterService.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  public enum IntegrityChecksType {
    /** Don't run integrity checks */
    SKIP,
    /** Always use the status from the integrity checker (legacy behavior 1) */
    ALWAYS,
    /**
     * Run integrity checks. If it fails and the target is the same, keep the current status;
     * otherwise, reject (legacy behavior 2).
     */
    KEEP_STATUS_IF_REJECTED_AND_SAME_TARGET,
    /**
     * Run integrity checks. If the target is the same, keep the current status.
     *
     * <p>This is an extension of the legacy behavior that allows marking a translation as invalid
     * when the integrity check did not catch the issue, eventually causing a build failure.
     */
    KEEP_STATUS_IF_SAME_TARGET;

    public static IntegrityChecksType fromLegacy(
        boolean integrityCheckSkipped, boolean integrityCheckKeepStatusIfFailedAndSameTarget) {
      IntegrityChecksType legacy = IntegrityChecksType.SKIP;

      if (!integrityCheckSkipped) {
        if (!integrityCheckKeepStatusIfFailedAndSameTarget) {
          legacy = ALWAYS;
        } else {
          legacy = KEEP_STATUS_IF_REJECTED_AND_SAME_TARGET;
        }
      }

      return legacy;
    }
  }

  /**
   * Imports a batch of text units.
   *
   * <p>Assumes the text units have the following mandatory attributes: repository name, target
   * locale, asset path, name (or tm textUnit id), target
   *
   * <p>Optional attribute: name (or tm text unit id), comment, status, includedInFile
   *
   * <p>If mandatory attributes are missing the text unit will be skipped
   *
   * <p>Integrity checks are applied and will determine the {@link Status}. Any string that passes
   * the integrity check will be imported as approved. If it doesn't pass the test it will be need
   * translation or the previous status when target is the same and the option is
   * integrityCheckKeepStatusIfFailedAndSameTarget is passed
   *
   * @param textUnitDTOs text units to import
   * @return
   */
  public PollableFuture<Void> asyncImportTextUnits(
      List<TextUnitDTO> textUnitDTOs, IntegrityChecksType integrityChecksType) {

    ImportTextUnitJobInput importTextUnitJobInput = new ImportTextUnitJobInput();
    importTextUnitJobInput.setTextUnitDTOs(textUnitDTOs);
    importTextUnitJobInput.setIntegrityChecksType(integrityChecksType);

    return quartzPollableTaskScheduler.scheduleJob(
        ImportTextUnitJob.class, importTextUnitJobInput, schedulerName);
  }

  @StopWatch
  public PollableFuture<Void> importTextUnits(
      List<TextUnitDTO> textUnitDTOs, IntegrityChecksType integrityChecksType) {

    return meterRegistry
        .timer("TextUnitBatchImporterService.importTextUnits")
        .record(
            () -> {
              logger.debug("Import {} text units", textUnitDTOs.size());
              List<TextUnitForBatchMatcherImport> textUnitForBatchImports =
                  skipInvalidAndConvertToTextUnitForBatchImport(textUnitDTOs);

              logger.debug("Batch by locale and asset to optimize the import");
              Map<Locale, Map<Asset, List<TextUnitForBatchMatcherImport>>> groupedByLocaleAndAsset =
                  textUnitForBatchImports.stream()
                      .collect(
                          Collectors.groupingBy(
                              TextUnitForBatchMatcherImport::getLocale,
                              Collectors.groupingBy(TextUnitForBatchMatcherImport::getAsset)));

              groupedByLocaleAndAsset.forEach(
                  (locale, assetMap) -> {
                    assetMap.forEach(
                        (asset, textUnitsForBatchImport) -> {
                          try (var timer =
                              Timer.resource(
                                      meterRegistry,
                                      "TextUnitBatchImporterService.importTextUnits.batch")
                                  .tag("repository", asset.getRepository().getName())
                                  .tag("asset", asset.getPath())) {

                            mapTextUnitsToImportWithExistingTextUnits(
                                locale, asset, textUnitsForBatchImport);
                            if (!IntegrityChecksType.SKIP.equals(integrityChecksType)) {
                              try (var timer2 =
                                  Timer.resource(
                                          meterRegistry,
                                          "TextUnitBatchImporterService.importTextUnits.integrityChecks")
                                      .tag("repository", asset.getRepository().getName())
                                      .tag("asset", asset.getPath())) {

                                applyIntegrityChecks(
                                    asset, textUnitsForBatchImport, integrityChecksType);
                              }
                            }
                            importTextUnitsOfLocaleAndAsset(locale, asset, textUnitsForBatchImport);
                          }
                        });
                  });

              return new PollableFutureTaskResult<>();
            });
  }

  /**
   * Maps text units to import with existing text units by first looking up by the tm text unit id
   * then the name of used text unit and finally the name of unused text unit (if there is only one
   * of them for a given name)
   *
   * @param locale
   * @param asset
   * @param textUnitsToImport text units to which the current text units must be added
   */
  @StopWatch
  void mapTextUnitsToImportWithExistingTextUnits(
      Locale locale, Asset asset, List<TextUnitForBatchMatcherImport> textUnitsToImport) {
    logger.debug(
        "Map the text units to import with current text unit for the given locale and asset");
    List<TextUnitDTO> textUnitTDOsForLocaleAndAsset =
        getTextUnitTDOsForLocaleAndAsset(locale, asset);
    Function<TextUnitForBatchMatcher, Optional<TextUnitDTO>> match =
        textUnitBatchMatcher.match(textUnitTDOsForLocaleAndAsset);
    textUnitsToImport.forEach(tu -> match.apply(tu).ifPresent(m -> tu.setCurrentTextUnit(m)));
  }

  @StopWatch
  @Transactional
  void importTextUnitsOfLocaleAndAsset(
      Locale locale, Asset asset, List<TextUnitForBatchMatcherImport> textUnitsToImport) {
    ZonedDateTime importTime = JSR310Migration.newDateTimeEmptyCtor();
    logger.info(
        "Start import text units for asset: {}, locale: {}, count: {}",
        asset.getPath(),
        locale.getBcp47Tag(),
        textUnitsToImport.size());

    textUnitsToImport.stream()
        .filter(
            logIfFalse(
                t -> t.getCurrentTextUnit() != null,
                logger,
                "No current text unit, skip: {}",
                TextUnitForBatchMatcherImport::getName))
        .filter(
            logIfFalse(
                t -> t.getContent() != null,
                logger,
                "Content can't be null, skip: {}",
                TextUnitForBatchMatcherImport::getName))
        .filter(
            logIfFalse(
                this::isUpdateNeeded,
                logger,
                "Update not needed, skip: {}",
                TextUnitForBatchMatcherImport::getName))
        .forEach(
            textUnitForBatchImport -> {
              logger.debug(
                  "Add translation: {} --> {}",
                  textUnitForBatchImport.getName(),
                  textUnitForBatchImport.getContent());

              TextUnitDTO currentTextUnit = textUnitForBatchImport.getCurrentTextUnit();

              TMTextUnitCurrentVariant tmTextUnitCurrentVariant = null;
              if (currentTextUnit.getTmTextUnitCurrentVariantId() != null) {
                // this is making many calls!
                tmTextUnitCurrentVariant =
                    tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(
                        currentTextUnit.getLocaleId(), currentTextUnit.getTmTextUnitId());
              }

              User importedBy = auditorAwareImpl.getCurrentAuditor().orElse(null);
              AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantResult =
                  tmService.addTMTextUnitCurrentVariantWithResult(
                      tmTextUnitCurrentVariant,
                      asset.getRepository().getTm().getId(),
                      asset.getId(),
                      currentTextUnit.getTmTextUnitId(),
                      locale.getId(),
                      textUnitForBatchImport.getContent(),
                      textUnitForBatchImport.getTargetComment(),
                      textUnitForBatchImport.getStatus(),
                      textUnitForBatchImport.isIncludedInLocalizedFile(),
                      importTime,
                      importedBy);

              if (addTMTextUnitCurrentVariantResult.isTmTextUnitCurrentVariantUpdated()) {

                Long tmTextUnitVariantId =
                    addTMTextUnitCurrentVariantResult
                        .getTmTextUnitCurrentVariant()
                        .getTmTextUnitVariant()
                        .getId();

                for (TMTextUnitVariantComment tmTextUnitVariantComment :
                    textUnitForBatchImport.getTmTextUnitVariantComments()) {
                  tmMTextUnitVariantCommentService.addComment(
                      tmTextUnitVariantId,
                      tmTextUnitVariantComment.getType(),
                      tmTextUnitVariantComment.getSeverity(),
                      tmTextUnitVariantComment.getContent());
                }
              }
            });
  }

  boolean isUpdateNeeded(TextUnitForBatchMatcherImport textUnitForBatchImport) {

    TextUnitDTO currentTextUnit = textUnitForBatchImport.getCurrentTextUnit();

    return currentTextUnit.getTarget() == null
        || tmService.isUpdateNeededForTmTextUnitVariant(
            currentTextUnit.getStatus(),
            DigestUtils.md5Hex(currentTextUnit.getTarget()),
            currentTextUnit.isIncludedInLocalizedFile(),
            currentTextUnit.getTargetComment(),
            textUnitForBatchImport.getStatus(),
            DigestUtils.md5Hex(textUnitForBatchImport.getContent()),
            textUnitForBatchImport.isIncludedInLocalizedFile(),
            textUnitForBatchImport.getTargetComment());
  }

  List<TextUnitDTO> getTextUnitTDOsForLocaleAndAsset(Locale locale, Asset asset) {
    return textUnitDTOsCacheService.getTextUnitDTOsForAssetAndLocale(
        asset.getId(), locale.getId(), false, UpdateType.ALWAYS);
  }

  void applyIntegrityChecks(
      Asset asset,
      List<TextUnitForBatchMatcherImport> textUnitsForBatchImport,
      IntegrityChecksType integrityChecksType) {

    Set<TextUnitIntegrityChecker> textUnitCheckers =
        integrityCheckerFactory.getTextUnitCheckers(asset);

    for (TextUnitForBatchMatcherImport textUnitForBatchImport : textUnitsForBatchImport) {

      TextUnitDTO currentTextUnit = textUnitForBatchImport.getCurrentTextUnit();

      if (currentTextUnit == null) {
        continue;
      }

      textUnitForBatchImport.setIncludedInLocalizedFile(true);
      textUnitForBatchImport.setStatus(APPROVED);

      for (TextUnitIntegrityChecker textUnitChecker : textUnitCheckers) {
        try {
          textUnitChecker.check(currentTextUnit.getSource(), textUnitForBatchImport.getContent());
        } catch (IntegrityCheckException ice) {

          boolean hasSameTarget =
              textUnitForBatchImport.getContent().equals(currentTextUnit.getTarget());

          if (hasSameTarget && !IntegrityChecksType.ALWAYS.equals(integrityChecksType)) {
            textUnitForBatchImport.setIncludedInLocalizedFile(
                currentTextUnit.isIncludedInLocalizedFile());
            textUnitForBatchImport.setStatus(currentTextUnit.getStatus());
          } else {
            logger.info(
                "Integrity check failed for string with source:\n{}\n\nand content:\n{}",
                currentTextUnit.getSource(),
                textUnitForBatchImport.getContent(),
                ice);
            textUnitForBatchImport.setIncludedInLocalizedFile(false);
            textUnitForBatchImport.setStatus(Status.TRANSLATION_NEEDED);

            TMTextUnitVariantComment tmTextUnitVariantComment = new TMTextUnitVariantComment();
            tmTextUnitVariantComment.setSeverity(Severity.ERROR);
            tmTextUnitVariantComment.setContent(ice.getMessage());
            textUnitForBatchImport.getTmTextUnitVariantComments().add(tmTextUnitVariantComment);
          }

          break;
        }
      }
    }
  }

  List<TextUnitForBatchMatcherImport> skipInvalidAndConvertToTextUnitForBatchImport(
      List<TextUnitDTO> textUnitDTOs) {

    logger.debug("Create caches to map convert to TextUnitForBatchMatcherImport list");
    LoadingCache<String, Repository> repositoriesCache =
        CacheBuilder.newBuilder()
            .build(CacheLoader.from((name) -> repositoryRepository.findByName(name)));

    LoadingCache<Map.Entry<String, Long>, Asset> assetsCache =
        CacheBuilder.newBuilder()
            .build(
                CacheLoader.from(
                    (entry) ->
                        assetRepository.findByPathAndRepositoryId(
                            entry.getKey(), entry.getValue())));

    logger.debug("Start converting to TextUnitForBatchMatcherImport");
    return textUnitDTOs.stream()
        .filter(
            logIfFalse(
                t -> t.getRepositoryName() != null,
                logger,
                "Missing mandatory repository name, skip: {}",
                TextUnitDTO::getName))
        .filter(
            logIfFalse(
                t -> t.getAssetPath() != null,
                logger,
                "Missing mandatory asset path, skip: {}",
                TextUnitDTO::getName))
        .filter(
            logIfFalse(
                t -> t.getTargetLocale() != null,
                logger,
                "Missing mandatory target locale, skip: {}",
                TextUnitDTO::getName))
        .filter(
            logIfFalse(
                t -> !(t.getName() == null && t.getTmTextUnitId() == null),
                logger,
                "Missing mandatory name or tmTextUnitId, skip: {}",
                TextUnitDTO::getName))
        .filter(
            logIfFalse(
                t -> t.getTarget() != null,
                logger,
                "Missing mandatory target, skip: {}",
                TextUnitDTO::getName))
        .map(
            t -> {
              TextUnitForBatchMatcherImport textUnitForBatchImport =
                  new TextUnitForBatchMatcherImport();
              textUnitForBatchImport.setTmTextUnitId(t.getTmTextUnitId());

              textUnitForBatchImport.setRepository(
                  repositoriesCache.getUnchecked(t.getRepositoryName()));
              if (textUnitForBatchImport.getRepository() != null) {
                textUnitForBatchImport.setAsset(
                    assetsCache.getUnchecked(
                        new SimpleEntry<>(
                            t.getAssetPath(), textUnitForBatchImport.getRepository().getId())));
              }
              textUnitForBatchImport.setName(t.getName());

              textUnitForBatchImport.setLocale(localeService.findByBcp47Tag(t.getTargetLocale()));
              textUnitForBatchImport.setContent(NormalizationUtils.normalize(t.getTarget()));
              textUnitForBatchImport.setComment(t.getComment());
              textUnitForBatchImport.setTargetComment(t.getTargetComment());
              textUnitForBatchImport.setIncludedInLocalizedFile(t.isIncludedInLocalizedFile());
              textUnitForBatchImport.setStatus(t.getStatus() == null ? APPROVED : t.getStatus());
              return textUnitForBatchImport;
            })
        .filter(
            logIfFalse(
                t -> t.getRepository() != null,
                logger,
                "No repository found, skip: {}",
                TextUnitForBatchMatcherImport::getName))
        .filter(
            logIfFalse(
                t -> t.getAsset() != null,
                logger,
                "No asset found, skip: {}",
                TextUnitForBatchMatcherImport::getName))
        .filter(
            logIfFalse(
                t -> t.getLocale() != null,
                logger,
                "No locale found, skip: {}",
                TextUnitForBatchMatcherImport::getName))
        .collect(Collectors.toList());
  }
}
