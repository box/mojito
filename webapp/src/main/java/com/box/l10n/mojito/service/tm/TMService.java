package com.box.l10n.mojito.service.tm;

import static com.box.l10n.mojito.quartz.QuartzSchedulerManager.DEFAULT_SCHEDULER_NAME;

import com.box.l10n.mojito.common.StreamUtil;
import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.PollableTask;
import com.box.l10n.mojito.entity.PullRun;
import com.box.l10n.mojito.entity.PullRunAsset;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMXliff;
import com.box.l10n.mojito.entity.security.user.User;
import com.box.l10n.mojito.okapi.AbstractImportTranslationsStep;
import com.box.l10n.mojito.okapi.FilterConfigIdOverride;
import com.box.l10n.mojito.okapi.ImportTranslationsByIdStep;
import com.box.l10n.mojito.okapi.ImportTranslationsByMd5Step;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep;
import com.box.l10n.mojito.okapi.ImportTranslationsFromLocalizedAssetStep.StatusForEqualTarget;
import com.box.l10n.mojito.okapi.ImportTranslationsStepAnnotation;
import com.box.l10n.mojito.okapi.ImportTranslationsWithTranslationKitStep;
import com.box.l10n.mojito.okapi.InheritanceMode;
import com.box.l10n.mojito.okapi.PseudoLocalizeStep;
import com.box.l10n.mojito.okapi.RawDocument;
import com.box.l10n.mojito.okapi.Status;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.okapi.TranslateStep;
import com.box.l10n.mojito.okapi.XLIFFWriter;
import com.box.l10n.mojito.okapi.asset.AssetPathToFilterConfigMapper;
import com.box.l10n.mojito.okapi.asset.UnsupportedAssetFilterTypeException;
import com.box.l10n.mojito.okapi.extractor.AssetExtractor;
import com.box.l10n.mojito.okapi.filters.CopyFormsOnImport;
import com.box.l10n.mojito.okapi.filters.FilterOptions;
import com.box.l10n.mojito.okapi.qualitycheck.Parameters;
import com.box.l10n.mojito.okapi.qualitycheck.QualityCheckStep;
import com.box.l10n.mojito.okapi.steps.CheckForDoNotTranslateStep;
import com.box.l10n.mojito.okapi.steps.FilterEventsToInMemoryRawDocumentStep;
import com.box.l10n.mojito.quartz.QuartzJobInfo;
import com.box.l10n.mojito.quartz.QuartzPollableTaskScheduler;
import com.box.l10n.mojito.retry.DataIntegrityViolationExceptionRetryTemplate;
import com.box.l10n.mojito.security.AuditorAwareImpl;
import com.box.l10n.mojito.service.WordCountService;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckStep;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pollableTask.InjectCurrentTask;
import com.box.l10n.mojito.service.pollableTask.Pollable;
import com.box.l10n.mojito.service.pollableTask.PollableFuture;
import com.box.l10n.mojito.service.pollableTask.PollableFutureTaskResult;
import com.box.l10n.mojito.service.pullrun.PullRunAssetService;
import com.box.l10n.mojito.service.pullrun.PullRunService;
import com.box.l10n.mojito.service.repository.RepositoryLocaleRepository;
import com.box.l10n.mojito.service.repository.RepositoryRepository;
import com.box.l10n.mojito.xliff.XliffUtils;
import com.google.common.base.Preconditions;
import com.ibm.icu.text.MessageFormat;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.exceptions.OkapiBadFilterInputException;
import net.sf.okapi.common.filters.IFilterConfigurationMapper;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipelinedriver.IPipelineDriver;
import net.sf.okapi.common.pipelinedriver.PipelineDriver;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import net.sf.okapi.steps.common.FilterEventsWriterStep;
import net.sf.okapi.steps.common.RawDocumentToFilterEventsStep;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to manage {@link TM}s (translation memories).
 *
 * <p>Allows to add {@link TMTextUnit}s (entities to be translated) and {@link TMTextUnitVariant}
 * (actual translations). The current translations are marked using {@link
 * TMTextUnitCurrentVariant}.
 *
 * @author jaurambault
 */
@Service
public class TMService {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(TMService.class);

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired LocaleService localeService;

  @Autowired TMTextUnitCurrentVariantRepository tmTextUnitCurrentVariantRepository;

  @Autowired EntityManager entityManager;

  @Autowired AssetExtractor assetExtractor;

  @Autowired RepositoryRepository repositoryRepository;

  @Autowired TMRepository tmRepository;

  @Autowired XliffUtils xliffUtils;

  @Autowired WordCountService wordCountService;

  @Autowired AssetRepository assetRepository;

  @Autowired TMXliffRepository tmXliffRepository;

  @Autowired AuditorAwareImpl auditorAwareImpl;

  @Autowired QuartzPollableTaskScheduler quartzPollableTaskScheduler;

  @Autowired RepositoryLocaleRepository repositoryLocaleRepository;

  @Autowired TextUnitUtils textUnitUtils;

  @Autowired IFilterConfigurationMapper filterConfigurationMapper;

  @Autowired AssetPathToFilterConfigMapper assetPathToFilterConfigMapper;

  @Autowired MeterRegistry meterRegistry;

  @Autowired PullRunService pullRunService;

  @Autowired PullRunAssetService pullRunAssetService;

  @Autowired
  DataIntegrityViolationExceptionRetryTemplate dataIntegrityViolationExceptionRetryTemplate;

  @Value("${l10n.tmService.quartz.schedulerName:" + DEFAULT_SCHEDULER_NAME + "}")
  String schedulerName;

  @Value("${l10n.ai.translation.review.similarity.editDistanceMax:50}")
  int editDistanceMax;

  @Value("${l10n.ai.translation.review.similarity.highPercentage:90}")
  int aiTranslationSimilarityHighPercentage;

  @Value("${l10n.ai.translation.review.similarity.mediumPercentage:70}")
  int aiTranslationSimilarityMediumPercentage;

  /**
   * Adds a {@link TMTextUnit} in a {@link TM}.
   *
   * @param tmId the {@link TM} id (must be valid)
   * @param assetId the {@link Asset} id (must be valid)
   * @param name the text unit name
   * @param content the text unit content
   * @param comment the text unit comment, can be {@code null}
   * @return the create {@link TMTextUnit}
   * @throws DataIntegrityViolationException If trying to create a {@link TMTextUnit} with same
   *     logical key as an existing one or TM id invalid
   */
  public TMTextUnit addTMTextUnit(
      Long tmId, Long assetId, String name, String content, String comment) {
    TM tm = tmRepository.findById(tmId).orElse(null);
    Asset asset = assetRepository.findById(assetId).orElse(null);
    return addTMTextUnit(tm, asset, name, content, comment, null, null, null);
  }

  /**
   * Adds a {@link TMTextUnit} in a {@link TM}.
   *
   * @param tm the {@link TM}
   * @param asset the {@link Asset}
   * @param name the text unit name
   * @param content the text unit content
   * @param comment the text unit comment, can be {@code null}
   * @param createdDate to specify a creation date (can be used to re-import old TM), can be {@code
   *     null}
   * @return the create {@link TMTextUnit}
   * @throws DataIntegrityViolationException If trying to create a {@link TMTextUnit} with same
   *     logical key as an existing one or TM id invalid
   */
  public TMTextUnit addTMTextUnit(
      TM tm,
      Asset asset,
      String name,
      String content,
      String comment,
      ZonedDateTime createdDate,
      PluralForm pluralForm,
      String pluralFormOther) {

    return addTMTextUnit(
        tm, asset, name, content, comment, null, createdDate, pluralForm, pluralFormOther);
  }

  /**
   * Adds a {@link TMTextUnit} in a {@link TM}.
   *
   * @param tm the {@link TM}
   * @param asset the {@link Asset}
   * @param name the text unit name
   * @param content the text unit content
   * @param comment the text unit comment, can be {@code null}
   * @param createdDate to specify a creation date (can be used to re-import old TM), can be {@code
   *     null}
   * @return the create {@link TMTextUnit}
   * @throws DataIntegrityViolationException If trying to create a {@link TMTextUnit} with same
   *     logical key as an existing one or TM id invalid
   */
  public TMTextUnit addTMTextUnit(
      TM tm,
      Asset asset,
      String name,
      String content,
      String comment,
      User createdByUser,
      ZonedDateTime createdDate,
      PluralForm pluralForm,
      String pluralFormOther) {

    Locale sourceLocale = asset.getRepository().getSourceLocale();
    Preconditions.checkNotNull(sourceLocale, "There must be a source locale");

    TMTextUnit tmTextUnit =
        addTMTextUnit(
            tm.getId(),
            asset.getId(),
            name,
            content,
            comment,
            createdByUser,
            createdDate,
            pluralForm,
            pluralFormOther,
            sourceLocale.getId());
    tmTextUnit.setTm(tm);
    tmTextUnit.setAsset(asset);
    return tmTextUnit;
  }

  TMTextUnit addTMTextUnit(
      final Long tmId,
      final Long assetId,
      final String name,
      final String content,
      final String comment,
      final User createdByUser,
      final ZonedDateTime createdDate,
      final PluralForm pluralForm,
      final String pluralFormOther,
      final Long sourceLocaleId) {

    TMTextUnit tmTextUnit = new TMTextUnit();

    tmTextUnit.setTm(entityManager.getReference(TM.class, tmId));
    tmTextUnit.setAsset(entityManager.getReference(Asset.class, assetId));
    tmTextUnit.setName(name);
    tmTextUnit.setContent(content);
    tmTextUnit.setComment(comment);
    tmTextUnit.setMd5(textUnitUtils.computeTextUnitMD5(name, content, comment));
    tmTextUnit.setWordCount(wordCountService.getEnglishWordCount(content));
    tmTextUnit.setContentMd5(DigestUtils.md5Hex(content));
    tmTextUnit.setCreatedDate(createdDate);
    tmTextUnit.setPluralForm(pluralForm);
    tmTextUnit.setPluralFormOther(pluralFormOther);
    tmTextUnit.setCreatedByUser(
        createdByUser != null ? createdByUser : auditorAwareImpl.getCurrentAuditor().orElse(null));

    tmTextUnit = tmTextUnitRepository.save(tmTextUnit);

    logger.debug("Add a current TMTextUnitVariant for the source text ie. the default locale");
    TMTextUnitVariant addTMTextUnitVariant =
        addTMTextUnitVariant(
            tmTextUnit.getId(),
            sourceLocaleId,
            content,
            comment,
            TMTextUnitVariant.Status.APPROVED,
            true,
            createdDate);
    makeTMTextUnitVariantCurrent(
        tmId, tmTextUnit.getId(), sourceLocaleId, addTMTextUnitVariant.getId(), assetId);

    return tmTextUnit;
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @return the created {@link TMTextUnitVariant} or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitVariant addCurrentTMTextUnitVariant(
      Long tmTextUnitId, Long localeId, String content) {
    return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, null)
        .getTmTextUnitVariant();
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be included or not in the
   *     localized files
   * @return the created {@link TMTextUnitVariant} or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitVariant addCurrentTMTextUnitVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile) {
    return addCurrentTMTextUnitVariant(
        tmTextUnitId, localeId, content, status, includedInLocalizedFile, null);
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be included or not in the
   *     localized files
   * @param createdDate to specify a creation date (can be used to re-import old TM), can be {@code
   *     null}
   * @return the created {@link TMTextUnitVariant} or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitVariant addCurrentTMTextUnitVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate) {
    return addTMTextUnitCurrentVariant(
            tmTextUnitId, localeId, content, null, status, includedInLocalizedFile, createdDate)
        .getTmTextUnitVariant();
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param comment the translation comment, can be {@code null}
   * @return the {@link TMTextUnitCurrentVariant} that holds the created {@link TMTextUnitVariant}
   *     or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(
      Long tmTextUnitId, Long localeId, String content, String comment) {
    return addTMTextUnitCurrentVariant(
        tmTextUnitId, localeId, content, comment, TMTextUnitVariant.Status.APPROVED);
  }

  /**
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param comment the translation comment, can be {@code null}
   * @param status the translation status
   * @return the {@link TMTextUnitCurrentVariant} that holds the created {@link TMTextUnitVariant}
   *     or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status) {
    return addTMTextUnitCurrentVariant(tmTextUnitId, localeId, content, comment, status, true);
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param comment the translation comment, can be {@code null}
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be included or not in the
   *     localized files
   * @return the {@link TMTextUnitCurrentVariant} that holds the created {@link TMTextUnitVariant}
   *     or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile) {
    return addTMTextUnitCurrentVariant(
        tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, null);
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param comment the translation comment, can be {@code null}
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be included or not in the
   *     localized files
   * @param createdDate to specify a creation date (can be used to re-import old TM), can be {@code
   *     null}
   * @return the {@link TMTextUnitCurrentVariant} that holds the created {@link TMTextUnitVariant}
   *     or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public TMTextUnitCurrentVariant addTMTextUnitCurrentVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate) {

    return addTMTextUnitCurrentVariantWithResult(
            tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, createdDate)
        .getTmTextUnitCurrentVariant();
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Also checks for an existing {@link TMTextUnitCurrentVariant} and if it references a {@link
   * TMTextUnitVariant} that has same content, the {@link TMTextUnitVariant} is returned and no
   * entities are created.
   *
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param comment the translation comment, can be {@code null}
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be included or not in the
   *     localized files
   * @param createdDate to specify a creation date (can be used to re-import old TM), can be {@code
   *     null}
   * @return the result that contains the {@link TMTextUnitCurrentVariant} and indicates if it was
   *     updated or not. The {@link TMTextUnitCurrentVariant} holds the created {@link
   *     TMTextUnitVariant} or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantWithResult(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate) {

    logger.debug("Check if there is a current TMTextUnitVariant");
    TMTextUnitCurrentVariant currentTmTextUnitCurrentVariant =
        tmTextUnitCurrentVariantRepository.findByLocale_IdAndTmTextUnit_Id(localeId, tmTextUnitId);

    TMTextUnit tmTextUnit = tmTextUnitRepository.findById(tmTextUnitId).orElse(null);

    if (tmTextUnit == null) {
      String msg =
          MessageFormat.format(
              "Unable to find the TMTextUnit with ID: {0}. The TMTextUnitVariant and "
                  + "TMTextUnitCurrentVariant will not be created.",
              tmTextUnitId);
      throw new RuntimeException(msg);
    }

    User createdBy = auditorAwareImpl.getCurrentAuditor().orElse(null);
    return addTMTextUnitCurrentVariantWithResult(
        currentTmTextUnitCurrentVariant,
        tmTextUnit.getTm().getId(),
        tmTextUnit.getAsset().getId(),
        tmTextUnitId,
        localeId,
        content,
        comment,
        status,
        includedInLocalizedFile,
        createdDate,
        createdBy);
  }

  /**
   * Adds a current {@link TMTextUnitVariant} in a {@link TMTextUnit} for a locale other than the
   * default locale.
   *
   * <p>Requires the {@link TMTextUnitCurrentVariant} and TM id for optimization purpose.
   *
   * @param tmTextUnitCurrentVariant current variant or null is there is none
   * @param tmId the {@link TM} id in which the translation is added
   * @param assetId
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation (default locale not accepted)
   * @param content the translation content
   * @param comment the translation comment, can be {@code null}
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be included or not in the
   *     localized files
   * @param createdDate to specify a creation date (can be used to re-import old TM), can be {@code
   *     null}
   * @param createdBy to specify the user adding translation
   * @return the result that contains the {@link TMTextUnitCurrentVariant} and indicates if it was
   *     updated or not. The {@link TMTextUnitCurrentVariant} holds the created {@link
   *     TMTextUnitVariant} or an existing one with same content
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are invalid
   */
  public AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantWithResult(
      TMTextUnitCurrentVariant tmTextUnitCurrentVariant,
      Long tmId,
      Long assetId,
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate,
      User createdBy,
      boolean checkOverridden) {

    boolean noUpdate = false;

    TMTextUnitVariant tmTextUnitVariant;

    int i = 0;

    if (tmTextUnitCurrentVariant == null
        || tmTextUnitCurrentVariant.getTmTextUnitVariant() == null) {
      logger.debug("There is no currrent text unit variant, add entities");
      tmTextUnitVariant =
          addTMTextUnitVariant(
              tmTextUnitId,
              localeId,
              content,
              comment,
              status,
              includedInLocalizedFile,
              createdDate,
              createdBy);

      if (tmTextUnitCurrentVariant == null) {
        tmTextUnitCurrentVariant =
            makeTMTextUnitVariantCurrent(
                tmId, tmTextUnitId, localeId, tmTextUnitVariant.getId(), assetId);
      } else {
        tmTextUnitCurrentVariant.setTmTextUnitVariant(tmTextUnitVariant);
        tmTextUnitCurrentVariant =
            tmTextUnitCurrentVariantRepository.save(tmTextUnitCurrentVariant);
      }
      logger.trace("Put the actual tmTextUnitVariant instead of the proxy");
      tmTextUnitCurrentVariant.setTmTextUnitVariant(tmTextUnitVariant);
    } else {
      logger.debug("There is a current text unit variant, check if an update is needed");
      TMTextUnitVariant currentTmTextUnitVariant = tmTextUnitCurrentVariant.getTmTextUnitVariant();
      boolean overridden =
          checkOverridden
              && currentTmTextUnitVariant.getStatus() == TMTextUnitVariant.Status.OVERRIDDEN;
      if (currentTmTextUnitVariant.getStatus() == TMTextUnitVariant.Status.MT_REVIEW_NEEDED
          && status == TMTextUnitVariant.Status.APPROVED) {
        logAiReviewMetrics(content, currentTmTextUnitVariant, localeId);
      }
      boolean updateNeeded =
          !overridden
              && isUpdateNeededForTmTextUnitVariant(
                  currentTmTextUnitVariant.getStatus(),
                  currentTmTextUnitVariant.getContentMD5(),
                  currentTmTextUnitVariant.isIncludedInLocalizedFile(),
                  currentTmTextUnitVariant.getComment(),
                  status,
                  DigestUtils.md5Hex(content),
                  includedInLocalizedFile,
                  comment);

      if (updateNeeded) {
        if (currentTmTextUnitVariant.getStatus() == TMTextUnitVariant.Status.MT_REVIEW_NEEDED
            && !isContentOrCommentUpdated(
                currentTmTextUnitVariant.getContentMD5(),
                currentTmTextUnitVariant.getComment(),
                DigestUtils.md5Hex(content),
                comment)) {
          logger.debug(
              "Content and comment are the same as existing, update the MT review needed text unit variant: {}",
              currentTmTextUnitVariant.getId());
          currentTmTextUnitVariant.setStatus(status);
          currentTmTextUnitVariant.setIncludedInLocalizedFile(includedInLocalizedFile);
          tmTextUnitVariantRepository.save(currentTmTextUnitVariant);
        } else {
          logger.debug(
              "The current text unit variant has different content, comment or needs review. Add entities");
          tmTextUnitVariant =
              addTMTextUnitVariant(
                  tmTextUnitId,
                  localeId,
                  content,
                  comment,
                  status,
                  includedInLocalizedFile,
                  createdDate,
                  createdBy);
          logger.debug(
              "Updating the current TextUnitVariant with id: {} current for locale: {}",
              tmTextUnitVariant.getId(),
              localeId);

          tmTextUnitCurrentVariantRepository.flush();
          tmTextUnitCurrentVariant.setTmTextUnitVariant(tmTextUnitVariant);
          tmTextUnitCurrentVariantRepository.save(tmTextUnitCurrentVariant);
        }

      } else {
        logger.debug(
            overridden
                ? "The current text unit variant is kept because it has the OVERRIDDEN status"
                : "The current text unit variant has same content, comment and review status, don't add entities and return it instead");
        noUpdate = true;
      }
    }

    return new AddTMTextUnitCurrentVariantResult(!noUpdate, tmTextUnitCurrentVariant);
  }

  private void logAiReviewMetrics(
      String reviewedTranslation, TMTextUnitVariant currentTmTextUnitVariant, Long localeId) {
    if (currentTmTextUnitVariant.getContent().equals(reviewedTranslation)) {
      meterRegistry
          .counter(
              "AiTranslation.review.similarity.match",
              Tags.of("locale", localeService.findById(localeId).getBcp47Tag()))
          .increment();
    } else {
      // Translation has been updated in review, check similarity of original to new
      logSimilarityMetrics(reviewedTranslation, currentTmTextUnitVariant, localeId);
    }
  }

  private void logSimilarityMetrics(
      String reviewedTranslation, TMTextUnitVariant currentTmTextUnitVariant, Long localeId) {
    LevenshteinDistance levenshteinDistance = new LevenshteinDistance(editDistanceMax);
    int editDistance =
        levenshteinDistance.apply(currentTmTextUnitVariant.getContent(), reviewedTranslation);
    Locale locale = localeService.findById(localeId);
    if (locale == null) {
      logger.warn("Locale not found for id: {}", localeId);
    }
    if (editDistance < 0) {
      // Negative edit distance means the edit distance threshold was exceeded, log as low
      // similarity
      meterRegistry
          .counter(
              "AiTranslation.review.similarity.low",
              Tags.of("locale", locale != null ? locale.getBcp47Tag() : "null"))
          .increment();
      // Log the max edit distance in this scenario as we know it is at least this large
      meterRegistry
          .counter(
              "AiTranslation.review.editDistance",
              Tags.of("locale", locale != null ? locale.getBcp47Tag() : "null"))
          .increment(editDistanceMax);
    } else {
      meterRegistry
          .counter(
              "AiTranslation.review.editDistance",
              Tags.of("locale", locale != null ? locale.getBcp47Tag() : "null"))
          .increment(editDistance);
      double similarityPercentage =
          calculateSimilarityPercentage(
              currentTmTextUnitVariant.getContent(), reviewedTranslation, editDistance);
      if (similarityPercentage >= aiTranslationSimilarityHighPercentage) {
        meterRegistry
            .counter(
                "AiTranslation.review.similarity.high",
                Tags.of("locale", locale != null ? locale.getBcp47Tag() : "null"))
            .increment();
      } else if (similarityPercentage >= aiTranslationSimilarityMediumPercentage) {
        meterRegistry
            .counter(
                "AiTranslation.review.similarity.medium",
                Tags.of("locale", locale != null ? locale.getBcp47Tag() : "null"))
            .increment();
      } else {
        meterRegistry
            .counter(
                "AiTranslation.review.similarity.low",
                Tags.of("locale", locale != null ? locale.getBcp47Tag() : "null"))
            .increment();
      }
    }
  }

  private double calculateSimilarityPercentage(String original, String updated, int editDistance) {
    int maxLength = Math.max(original.length(), updated.length());
    return ((double) (maxLength - editDistance) / maxLength) * 100;
  }

  public AddTMTextUnitCurrentVariantResult addTMTextUnitCurrentVariantWithResult(
      TMTextUnitCurrentVariant tmTextUnitCurrentVariant,
      Long tmId,
      Long assetId,
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate,
      User createdBy) {
    return this.addTMTextUnitCurrentVariantWithResult(
        tmTextUnitCurrentVariant,
        tmId,
        assetId,
        tmTextUnitId,
        localeId,
        content,
        comment,
        status,
        includedInLocalizedFile,
        createdDate,
        createdBy,
        false);
  }

  /**
   * Indicates if a {@link TMTextUnitVariant} should be updated by looking at new/old content,
   * status, comments, etc
   *
   * @param currentStatus
   * @param currentContentMd5
   * @param currentIncludedInLocalizedFile
   * @param currentComment
   * @param newStatus
   * @param newContentMd5
   * @param newIncludedInLocalizedFile
   * @param newComment
   * @return
   */
  public boolean isUpdateNeededForTmTextUnitVariant(
      TMTextUnitVariant.Status currentStatus,
      String currentContentMd5,
      boolean currentIncludedInLocalizedFile,
      String currentComment,
      TMTextUnitVariant.Status newStatus,
      String newContentMd5,
      boolean newIncludedInLocalizedFile,
      String newComment) {

    return !(currentContentMd5.equals(newContentMd5)
        && currentStatus.equals(newStatus)
        && currentIncludedInLocalizedFile == newIncludedInLocalizedFile
        && Objects.equals(currentComment, newComment));
  }

  /**
   * Indicates if only the {@link TMTextUnitVariant.Status} and includedInLocalizedFile boolean has
   * changed to require an update for a {@link TMTextUnitVariant} by looking at new/old content,
   * status, comments, etc
   *
   * <p>Used to avoid causing Auditing table update failures as a variant status change does not
   * require the creation of a new variant.
   *
   * @param currentContentMd5
   * @param currentComment
   * @param newContentMd5
   * @param newComment
   * @return
   */
  public boolean isContentOrCommentUpdated(
      String currentContentMd5, String currentComment, String newContentMd5, String newComment) {

    return !(currentContentMd5.equals(newContentMd5)
        || !Objects.equals(currentComment, newComment));
  }

  /**
   * Adds a {@link TMTextUnitVariant} in a {@link TMTextUnit}.
   * <p/>
   * No checks are performed on the locale or for duplicated content. If this
   * is a requirement use {@link #addCurrentTMTextUnitVariant(java.lang.Long, java.lang.Long, java.lang.String)
   *
   * @param tmTextUnitId the text unit that will contain the translation
   * @param localeId locale id of the translation
   * @param content the translation content
   * @param comment comment for the translation, can be {@code null}
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be
   * included or not in the localized files
   * @return the created {@link TMTextUnitVariant}
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
   * invalid
   */
  protected TMTextUnitVariant addTMTextUnitVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile) {

    return addTMTextUnitVariant(
        tmTextUnitId, localeId, content, comment, status, includedInLocalizedFile, null);
  }

  public TMTextUnitVariant addTMTextUnitVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate) {
    User createdBy = auditorAwareImpl.getCurrentAuditor().orElse(null);
    return addTMTextUnitVariant(
        tmTextUnitId,
        localeId,
        content,
        comment,
        status,
        includedInLocalizedFile,
        createdDate,
        createdBy);
  }

  /**
   * Adds a {@link TMTextUnitVariant} in a {@link TMTextUnit}.
   * <p/>
   * No checks are performed on the locale or for duplicated content. If this
   * is a requirement use {@link #addCurrentTMTextUnitVariant(java.lang.Long, java.lang.Long, java.lang.String)
   *
   * @param tmTextUnitId the text unit that will contain the translation
   * @param localeId locale id of the translation
   * @param content the translation content
   * @param comment comment for the translation, can be {@code null}
   * @param status the translation status
   * @param includedInLocalizedFile indicate if the translation should be
   * included or not in the localized files
   * @param createdDate to specify a creation date (can be used to re-import
   * old TM), can be {@code null}
   * @param createdBy to specify a user in action
   * @return the created {@link TMTextUnitVariant}
   * @throws DataIntegrityViolationException If tmTextUnitId or localeId are
   * invalid
   */
  public TMTextUnitVariant addTMTextUnitVariant(
      Long tmTextUnitId,
      Long localeId,
      String content,
      String comment,
      TMTextUnitVariant.Status status,
      boolean includedInLocalizedFile,
      ZonedDateTime createdDate,
      User createdBy) {

    logger.debug(
        "Add TMTextUnitVariant for tmId: {} locale id: {}, content: {}",
        tmTextUnitId,
        localeId,
        content);

    Preconditions.checkNotNull(content, "content must not be null when adding a TMTextUnitVariant");

    TMTextUnit tmTextUnit = entityManager.getReference(TMTextUnit.class, tmTextUnitId);
    Locale locale = entityManager.getReference(Locale.class, localeId);

    TMTextUnitVariant tmTextUnitVariant = new TMTextUnitVariant();

    tmTextUnitVariant.setTmTextUnit(tmTextUnit);
    tmTextUnitVariant.setLocale(locale);
    tmTextUnitVariant.setContent(content);
    tmTextUnitVariant.setContentMD5(DigestUtils.md5Hex(content));
    tmTextUnitVariant.setComment(comment);
    tmTextUnitVariant.setStatus(status);
    tmTextUnitVariant.setIncludedInLocalizedFile(includedInLocalizedFile);
    tmTextUnitVariant.setCreatedDate(createdDate);
    tmTextUnitVariant.setCreatedByUser(createdBy);
    tmTextUnitVariant = tmTextUnitVariantRepository.save(tmTextUnitVariant);
    logger.trace("TMTextUnitVariant saved");

    return tmTextUnitVariant;
  }

  /**
   * Makes a {@link TMTextUnitVariant} current in a {@link TMTextUnit} for a given locale.
   *
   * @param tmId the TM id (must be the same as the tmTextUnit.getTm()) - used for denormalization
   * @param tmTextUnitId the text unit that will contains the translation
   * @param localeId locale id of the translation
   * @param tmTextUnitVariantId the text unit variant id to be made current
   * @param assetId
   * @return {@link TMTextUnitCurrentVariant} that contains the {@link TMTextUnitVariant}
   * @throws DataIntegrityViolationException If tmId, tmTextUnitId or localeId are invalid
   */
  protected TMTextUnitCurrentVariant makeTMTextUnitVariantCurrent(
      Long tmId, Long tmTextUnitId, Long localeId, Long tmTextUnitVariantId, Long assetId) {
    logger.debug(
        "Make the TMTextUnitVariant with id: {} current for locale: {}",
        tmTextUnitVariantId,
        localeId);
    TMTextUnitCurrentVariant tmTextUnitCurrentVariant = new TMTextUnitCurrentVariant();
    tmTextUnitCurrentVariant.setTm(entityManager.getReference(TM.class, tmId));
    tmTextUnitCurrentVariant.setAsset(entityManager.getReference(Asset.class, assetId));
    tmTextUnitCurrentVariant.setTmTextUnit(
        entityManager.getReference(TMTextUnit.class, tmTextUnitId));
    tmTextUnitCurrentVariant.setTmTextUnitVariant(
        entityManager.getReference(TMTextUnitVariant.class, tmTextUnitVariantId));
    tmTextUnitCurrentVariant.setLocale(entityManager.getReference(Locale.class, localeId));

    tmTextUnitCurrentVariantRepository.save(tmTextUnitCurrentVariant);
    logger.trace("TMTextUnitCurrentVariant persisted");

    return tmTextUnitCurrentVariant;
  }

  /**
   * Parses the XLIFF (from a translation kit) content and extract the new/changed variants.Then
   * updates the TM with these new variants.
   *
   * @param xliffContent The content of the localized XLIFF TODO(P1) Use BCP47 tag instead of Locale
   *     object?
   * @param importStatus specific status to use when importing translation
   * @param dropImporterUsernameOverride overrides the user importing translation
   * @return the imported XLIFF with information for each text unit about the import process
   * @throws OkapiBadFilterInputException when XLIFF document is invalid
   */
  public UpdateTMWithXLIFFResult updateTMWithTranslationKitXLIFF(
      String xliffContent,
      TMTextUnitVariant.Status importStatus,
      String dropImporterUsernameOverride)
      throws OkapiBadFilterInputException {

    ImportTranslationsWithTranslationKitStep importTranslationsWithTranslationKitStep =
        new ImportTranslationsWithTranslationKitStep();
    importTranslationsWithTranslationKitStep.setDropImporterUsernameOverride(
        dropImporterUsernameOverride);

    return updateTMWithXliff(xliffContent, importStatus, importTranslationsWithTranslationKitStep);
  }

  public UpdateTMWithXLIFFResult updateTMWithXLIFFById(
      String xliffContent, TMTextUnitVariant.Status importStatus)
      throws OkapiBadFilterInputException {

    return updateTMWithXliff(xliffContent, importStatus, new ImportTranslationsByIdStep());
  }

  /**
   * Parses the XLIFF content and extract the new/changed variants by doing MD5 lookup for a given
   * repository. Then updates the TM with these new variants. If the XLIFF is linked to an existing
   * translation kit, use {@link #updateTMWithTranslationKitXLIFF(java.lang.String,
   * com.box.l10n.mojito.entity.TMTextUnitVariant.Status) }
   *
   * @param xliffContent The content of the localized XLIFF TODO(P1) Use BCP47 tag instead of Locale
   *     object?
   * @param importStatus specific status to use when importing translation
   * @param repository the repository in which to perform the import
   * @return the imported XLIFF with information for each text unit about the import process
   * @throws OkapiBadFilterInputException when XLIFF document is invalid
   */
  public UpdateTMWithXLIFFResult updateTMWithXLIFFByMd5(
      String xliffContent, TMTextUnitVariant.Status importStatus, Repository repository)
      throws OkapiBadFilterInputException {

    return updateTMWithXliff(
        xliffContent, importStatus, new ImportTranslationsByMd5Step(repository));
  }

  /**
   * Update TM with XLIFF.
   *
   * @param xliffContent The content of the localized XLIFF TODO(P1) Use BCP47 tag instead of Locale
   *     object?
   * @param importStatus specific status to use when importing translation
   * @param abstractImportTranslationsStep defines which import logic to apply
   * @return the imported XLIFF with information for each text unit about the import process
   * @throws OkapiBadFilterInputException
   */
  private UpdateTMWithXLIFFResult updateTMWithXliff(
      String xliffContent,
      TMTextUnitVariant.Status importStatus,
      AbstractImportTranslationsStep abstractImportTranslationsStep)
      throws OkapiBadFilterInputException {

    logger.debug("Configuring pipeline for localized XLIFF processing");

    IPipelineDriver driver = new PipelineDriver();
    driver.addStep(new RawDocumentToFilterEventsStep(new XLIFFFilter()));

    driver.addStep(getConfiguredQualityStep());
    IntegrityCheckStep integrityCheckStep = new IntegrityCheckStep();
    driver.addStep(integrityCheckStep);

    abstractImportTranslationsStep.setImportWithStatus(importStatus);
    driver.addStep(abstractImportTranslationsStep);

    // TODO(P1) It sounds like it's not possible to the XLIFFFilter for the output
    // because the note is readonly mode and we need to override it to provide more information
    logger.debug(
        "Prepare FilterEventsWriterStep to use an XLIFFWriter with outputstream (allows only one doc to be processed)");
    FilterEventsWriterStep filterEventsWriterStep = new FilterEventsWriterStep(new XLIFFWriter());
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    filterEventsWriterStep.setOutputStream(byteArrayOutputStream);
    filterEventsWriterStep.setOutputEncoding(StandardCharsets.UTF_8.toString());

    driver.addStep(filterEventsWriterStep);

    // We need to read first the target language, because if we wait for okapi to read
    // it from the file it is too late to write the output with the XLIFFWriter
    // (missing target language)
    String targetLanguage = xliffUtils.getTargetLanguage(xliffContent);
    LocaleId targetLocaleId =
        targetLanguage != null ? LocaleId.fromBCP47(targetLanguage) : LocaleId.EMPTY;
    RawDocument rawDocument = new RawDocument(xliffContent, LocaleId.ENGLISH, targetLocaleId);

    driver.addBatchItem(rawDocument, RawDocument.getFakeOutputURIForStream(), null);

    logger.debug("Start processing batch");
    driver.processBatch();

    logger.debug("Get the Import report");
    ImportTranslationsStepAnnotation importTranslationsStepAnnotation =
        rawDocument.getAnnotation(ImportTranslationsStepAnnotation.class);

    UpdateTMWithXLIFFResult updateReport = new UpdateTMWithXLIFFResult();
    updateReport.setXliffContent(StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream));
    updateReport.setComment(importTranslationsStepAnnotation.getComment());

    return updateReport;
  }

  /**
   * @return A {@code QualityCheckStep} that will only perform the needed checks
   */
  private QualityCheckStep getConfiguredQualityStep() {

    Parameters parameters = new Parameters();
    parameters.disableAllChecks();

    // only enable the checks we want
    parameters.setEmptyTarget(true);
    parameters.setTargetSameAsSource(true);
    parameters.setTargetSameAsSourceForSameLanguage(true);
    parameters.setLeadingWS(true);
    parameters.setTrailingWS(true);
    parameters.setDoubledWord(true);
    parameters.setCheckXliffSchema(true);

    QualityCheckStep qualityCheckStep = new QualityCheckStep();
    qualityCheckStep.setParameters(parameters);

    return qualityCheckStep;
  }

  /**
   * Exports an {@link Asset} as XLIFF for a given locale.
   *
   * @param assetId {@link Asset#id} to be exported
   * @param bcp47Tag bcp47tag of the locale that needs to be exported
   * @return an XLIFF that contains {@link Asset}'s translation for that locale
   */
  @Transactional
  public String exportAssetAsXLIFF(Long assetId, String bcp47Tag) {

    logger.debug("Export data for asset id: {} and locale: {}", assetId, bcp47Tag);

    logger.trace("Create XLIFFWriter");
    XLIFFWriter xliffWriter = new XLIFFWriter();

    logger.trace(
        "Prepare FilterEventsWriterStep to use an XLIFFWriter with outputstream (allows only one doc to be processed)");
    FilterEventsWriterStep filterEventsWriterStep = new FilterEventsWriterStep(xliffWriter);
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    filterEventsWriterStep.setOutputStream(byteArrayOutputStream);
    filterEventsWriterStep.setOutputEncoding(StandardCharsets.UTF_8.toString());

    logger.trace("Prepare the Okapi pipeline");
    IPipelineDriver driver = new PipelineDriver();
    driver.addStep(new RawDocumentToFilterEventsStep(new TMExportFilter(assetId)));
    driver.addStep(filterEventsWriterStep);

    logger.trace("Add single document with fake output URI to be processed with an outputStream");
    Locale locale = localeService.findByBcp47Tag(bcp47Tag);
    RawDocument rawDocument =
        new RawDocument(
            RawDocument.EMPTY, LocaleId.ENGLISH, LocaleId.fromBCP47(locale.getBcp47Tag()));

    driver.addBatchItem(rawDocument, RawDocument.getFakeOutputURIForStream(), null);

    logger.debug("Start processing batch");
    driver.processBatch();

    logger.trace("Get the output result from the stream");
    return StreamUtil.getUTF8OutputStreamAsString(byteArrayOutputStream);
  }

  /**
   * Parses the given content and adds the translation for every text unit. Returns the content of
   * the localized content.
   *
   * <p>TODO(P1) This needs to support other file formats
   *
   * @param asset The {@link Asset} used to get translations
   * @param content The content to be localized
   * @param repositoryLocale the repository locale used to fetch the translation. Also used for the
   *     output tag if outputBcp47tag is null.
   * @param outputBcp47tag Optional, can be null. Allows to generate the file for a bcp47 tag that
   *     is different from the repository locale (which is still used to fetch the translations).
   *     This can be used to generate a file with tag "fr" even if the translations are stored with
   *     fr-FR repository locale.
   * @param filterOptions
   * @param status
   * @param inheritanceMode
   * @param pullRunName
   * @return the localized asset
   */
  public String generateLocalized(
      Asset asset,
      String content,
      RepositoryLocale repositoryLocale,
      String outputBcp47tag,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      Status status,
      InheritanceMode inheritanceMode,
      String pullRunName)
      throws UnsupportedAssetFilterTypeException {

    String bcp47Tag;

    if (outputBcp47tag == null) {
      bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();
    } else {
      logger.debug(
          "An output bcp47 tag: {} is specified (won't use the default tag (from the repository locale)",
          outputBcp47tag);
      bcp47Tag = outputBcp47tag;
    }

    logger.debug("Configuring pipeline for localized XLIFF generation");

    boolean replaceUsedTmTextUnitVariantIds = pullRunName != null;
    TranslateStep translateStep =
        new TranslateStep(
            asset, repositoryLocale, inheritanceMode, status, replaceUsedTmTextUnitVariantIds);
    String generateLocalizedBase =
        generateLocalizedBase(
            asset, content, filterConfigIdOverride, filterOptions, translateStep, bcp47Tag);

    if (replaceUsedTmTextUnitVariantIds) {
      dataIntegrityViolationExceptionRetryTemplate.execute(
          context -> {
            replaceUsedTmTextUnitVariantIds(
                asset,
                pullRunName,
                repositoryLocale.getLocale(),
                translateStep.getUsedTmTextUnitVariantIds(),
                outputBcp47tag);
            return null;
          });
    }

    return generateLocalizedBase;
  }

  void replaceUsedTmTextUnitVariantIds(
      Asset asset,
      String pullRunName,
      Locale locale,
      List<Long> usedTmTextUnitVariantIds,
      String outputBcp47tag) {
    logger.debug(
        "Replace used TmTextUnitVariantIds for pull run name: {} and locale: {}",
        pullRunName,
        locale.getBcp47Tag());
    PullRun pullRun = pullRunService.getOrCreate(pullRunName, asset.getRepository());
    PullRunAsset pullRunAsset = pullRunAssetService.getOrCreate(pullRun, asset);
    List<Long> uniqueUsedTmTextUnitVariantIds =
        usedTmTextUnitVariantIds.stream().distinct().collect(Collectors.toList());
    pullRunAssetService.replaceTextUnitVariants(
        pullRunAsset, locale.getId(), uniqueUsedTmTextUnitVariantIds, outputBcp47tag);
  }

  /**
   * Parses the given content and adds the pseudo localization for every text unit. Returns the
   * pseudolocalized content.
   *
   * @param asset The {@link Asset} used to get translations
   * @param content The content to be pseudolocalized
   * @return the pseudolocalized asset
   */
  public String generatePseudoLocalized(
      Asset asset, String content, FilterConfigIdOverride filterConfigIdOverride)
      throws UnsupportedAssetFilterTypeException {

    String bcp47tag = "en-x-psaccent";

    BasePipelineStep pseudoLocalizedStep = (BasePipelineStep) new PseudoLocalizeStep(asset);
    return generateLocalizedBase(
        asset, content, filterConfigIdOverride, null, pseudoLocalizedStep, bcp47tag);
  }

  /**
   * Parses the given content and adds the translation for every text unit. Returns the content of
   * the localized content.
   *
   * <p>TODO(P1) This needs to support other file formats
   *
   * @param asset The {@link Asset} used to get translations
   * @param content The content to be localized
   * @param filterConfigIdOverride
   * @param filterOptions
   * @param step
   * @param outputBcp47tag Optional, can be null. Allows to generate the file for a bcp47 tag that
   *     is different from the repository locale (which is still used to fetch the translations).
   *     This can be used to generate a file with tag "fr" even if the translations are stored with
   *     fr-FR repository locale.
   * @return the localized asset
   */
  private String generateLocalizedBase(
      Asset asset,
      String content,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions,
      BasePipelineStep step,
      String outputBcp47tag)
      throws UnsupportedAssetFilterTypeException {

    try (Timer.ResourceSample timer =
        Timer.resource(meterRegistry, "TMService.generateLocalizedBase")
            .tag("repositoryId", Objects.toString(asset.getRepository().getId()))) {

      IPipelineDriver driver = new PipelineDriver();

      driver.addStep(new RawDocumentToFilterEventsStep());
      driver.addStep(new CheckForDoNotTranslateStep());
      driver.addStep(step);

      // TODO(P1) see assetExtractor comments
      logger.debug("Adding all supported filters to the pipeline driver");
      driver.setFilterConfigurationMapper(filterConfigurationMapper);

      FilterEventsToInMemoryRawDocumentStep filterEventsToInMemoryRawDocumentStep =
          new FilterEventsToInMemoryRawDocumentStep();
      driver.addStep(filterEventsToInMemoryRawDocumentStep);

      LocaleId targetLocaleId = LocaleId.fromBCP47(outputBcp47tag);
      RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH, targetLocaleId);

      // TODO(P1) see assetExtractor comments
      String filterConfigId;

      if (filterConfigIdOverride != null) {
        filterConfigId = filterConfigIdOverride.getOkapiFilterId();
      } else {
        filterConfigId = assetPathToFilterConfigMapper.getFilterConfigIdFromPath(asset.getPath());
      }

      rawDocument.setFilterConfigId(filterConfigId);
      logger.debug("Set filter config {} for asset {}", filterConfigId, asset.getPath());

      logger.debug("Filter options: {}", filterOptions);
      rawDocument.setAnnotation(new FilterOptions(filterOptions));

      driver.addBatchItem(rawDocument);

      logger.debug("Start processing batch");
      driver.processBatch();

      String localizedContent = filterEventsToInMemoryRawDocumentStep.getOutput(rawDocument);

      return localizedContent;
    }
  }

  /**
   * Imports a localized version of an asset.
   *
   * <p>The target strings are checked against the source strings and if they are equals the status
   * of the imported translation is defined by statusForEqualTarget. When SKIPED is specified the
   * import is actually skipped.
   *
   * <p>For not fully translated locales, targets are imported only if they are different from
   * target of the parent locale.
   *
   * @param asset the asset for which the content will be imported
   * @param repositoryLocale the locale of the content to be imported
   * @param content the localized asset content
   * @param statusForEqualtarget the status of the text unit variant when the source equals the
   *     target
   * @param filterConfigIdOverride to override the filter used to process the asset
   * @param filterOptions
   * @return
   */
  public PollableFuture<Void> importLocalizedAssetAsync(
      Long assetId,
      String content,
      Long localeId,
      StatusForEqualTarget statusForEqualtarget,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions) {

    ImportLocalizedAssetJobInput importLocalizedAssetJobInput = new ImportLocalizedAssetJobInput();
    importLocalizedAssetJobInput.setAssetId(assetId);
    importLocalizedAssetJobInput.setLocaleId(localeId);
    importLocalizedAssetJobInput.setContent(content);
    importLocalizedAssetJobInput.setStatusForEqualtarget(statusForEqualtarget);
    importLocalizedAssetJobInput.setFilterConfigIdOverride(filterConfigIdOverride);
    importLocalizedAssetJobInput.setFilterOptions(filterOptions);

    QuartzJobInfo<ImportLocalizedAssetJobInput, Void> quartzJobInfo =
        QuartzJobInfo.newBuilder(ImportLocalizedAssetJob.class)
            .withInlineInput(false)
            .withInput(importLocalizedAssetJobInput)
            .withScheduler(schedulerName)
            .build();

    return quartzPollableTaskScheduler.scheduleJob(quartzJobInfo);
  }

  public void importLocalizedAsset(
      Long assetId,
      String content,
      Long localeId,
      StatusForEqualTarget statusForEqualtarget,
      FilterConfigIdOverride filterConfigIdOverride,
      List<String> filterOptions)
      throws UnsupportedAssetFilterTypeException {

    Asset asset = assetRepository.findById(assetId).orElse(null);
    RepositoryLocale repositoryLocale =
        repositoryLocaleRepository.findByRepositoryIdAndLocaleId(
            asset.getRepository().getId(), localeId);

    String bcp47Tag = repositoryLocale.getLocale().getBcp47Tag();

    logger.debug("Configuring pipeline to import localized file");

    IPipelineDriver driver = new PipelineDriver();

    driver.addStep(new RawDocumentToFilterEventsStep());
    driver.addStep(new CheckForDoNotTranslateStep());
    driver.addStep(
        new ImportTranslationsFromLocalizedAssetStep(
            asset, repositoryLocale, statusForEqualtarget));

    logger.debug("Adding all supported filters to the pipeline driver");
    driver.setFilterConfigurationMapper(filterConfigurationMapper);

    FilterEventsToInMemoryRawDocumentStep filterEventsToInMemoryRawDocumentStep =
        new FilterEventsToInMemoryRawDocumentStep();
    driver.addStep(filterEventsToInMemoryRawDocumentStep);

    LocaleId targetLocaleId = LocaleId.fromBCP47(bcp47Tag);
    RawDocument rawDocument = new RawDocument(content, LocaleId.ENGLISH, targetLocaleId);
    rawDocument.setAnnotation(new CopyFormsOnImport());
    rawDocument.setAnnotation(new FilterOptions(filterOptions));

    String filterConfigId;

    if (filterConfigIdOverride != null) {
      filterConfigId = filterConfigIdOverride.getOkapiFilterId();
    } else {
      filterConfigId = assetPathToFilterConfigMapper.getFilterConfigIdFromPath(asset.getPath());
    }

    rawDocument.setFilterConfigId(filterConfigId);
    logger.debug("Set filter config {} for asset {}", filterConfigId, asset.getPath());

    driver.addBatchItem(rawDocument);

    logger.debug("Start processing batch");
    processBatchInTransaction(driver);
  }

  @Transactional
  void processBatchInTransaction(IPipelineDriver driver) {
    driver.processBatch();
  }

  /**
   * Exports an {@link Asset} as XLIFF for a given locale asynchronously.
   *
   * @param tmXliffId {@link TMXliff#id} to persist generated XLIFF
   * @param assetId {@link Asset#id} to be exported
   * @param bcp47Tag bcp47tag of the locale that needs to be exported
   * @param currentTask
   * @return {@link PollableFutureTaskResult} that contains an XLIFF as result
   */
  @Pollable(async = true, message = "Export asset as xliff")
  public PollableFuture<String> exportAssetAsXLIFFAsync(
      Long tmXliffId, Long assetId, String bcp47Tag, @InjectCurrentTask PollableTask currentTask) {

    PollableFutureTaskResult<String> pollableFutureTaskResult = new PollableFutureTaskResult<>();

    String xliff = exportAssetAsXLIFF(assetId, bcp47Tag);

    TMXliff tmXliff = tmXliffRepository.findById(tmXliffId).orElse(null);
    tmXliff.setAsset(assetRepository.findById(assetId).orElse(null));
    tmXliff.setLocale(localeService.findByBcp47Tag(bcp47Tag));
    tmXliff.setContent(xliff);
    tmXliff.setPollableTask(currentTask);
    tmXliffRepository.save(tmXliff);

    pollableFutureTaskResult.setResult(xliff);
    return pollableFutureTaskResult;
  }

  public TMXliff createTMXliff(
      Long assetId, String bcp47Tag, String content, PollableTask pollableTask) {
    TMXliff tmXliff = new TMXliff();
    tmXliff.setAsset(assetRepository.findById(assetId).orElse(null));
    tmXliff.setLocale(localeService.findByBcp47Tag(bcp47Tag));
    tmXliff.setContent(content);
    tmXliff.setPollableTask(pollableTask);
    tmXliff = tmXliffRepository.save(tmXliff);
    return tmXliff;
  }
}
