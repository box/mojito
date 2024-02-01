package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.Locale;
import com.box.l10n.mojito.entity.PluralForm;
import com.box.l10n.mojito.entity.Repository;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.okapi.ImportExportTextUnitUtils;
import com.box.l10n.mojito.okapi.TextUnitUtils;
import com.box.l10n.mojito.service.asset.AssetRepository;
import com.box.l10n.mojito.service.asset.AssetService;
import com.box.l10n.mojito.service.locale.LocaleService;
import com.box.l10n.mojito.service.pluralform.PluralFormService;
import com.box.l10n.mojito.service.translationkit.TranslationKitRepository;
import com.box.l10n.mojito.service.translationkit.TranslationKitService;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.LocaleId;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.pipeline.annotations.StepParameterMapping;
import net.sf.okapi.common.pipeline.annotations.StepParameterType;
import net.sf.okapi.common.resource.ITextUnit;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.filters.xliff.XLIFFFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.util.StopWatch;

/**
 * @author wyau
 */
@Configurable
public class ImportExportedXliffStep extends BasePipelineStep {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(ImportExportedXliffStep.class);

  @Autowired TMService tmService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired LocaleService localeService;

  @Autowired TranslationKitService translationKitService;

  @Autowired TranslationKitRepository translationKitRepository;

  @Autowired AssetRepository assetRepository;

  @Autowired AssetService assetService;

  @Autowired TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  @Autowired ImportExportTextUnitUtils importExportTextUnitUtils;

  @Autowired TextUnitUtils textUnitUtils;

  @Autowired PluralFormService pluralFormService;

  private LocaleId targetLocaleId;
  private LocaleId sourceLocaleId;

  private Repository repository;

  private Map<String, Long> tmTextUnitIdsByMd5ForAsset;

  XLIFFFilter xliffFilter;

  String xliffContent;

  Asset asset;

  /**
   * Indicates if the TM should be updated or if the translation can be imported assuming that there
   * is no translation yet.
   */
  private boolean updateTM;

  StopWatch overallStopWatch = new StopWatch();
  StopWatch textUnitStopWatch = new StopWatch();
  int numOfTextUnitProcess = 0;

  public ImportExportedXliffStep(Repository repository, String xliffContent, boolean updateTM) {
    this.repository = repository;
    this.xliffContent = xliffContent;
    this.updateTM = updateTM;
  }

  public ImportExportedXliffStep(Asset asset, String xliffContent, boolean updateTM) {
    this.asset = asset;
    this.repository = asset.getRepository();
    this.xliffContent = xliffContent;
    this.updateTM = updateTM;
  }

  @Override
  public String getName() {
    return "Import exported XLIFF";
  }

  @Override
  public String getDescription() {
    return "Import into the TM with the extracted XLIFF.";
  }

  @SuppressWarnings("deprecation")
  @StepParameterMapping(parameterType = StepParameterType.TARGET_LOCALE)
  public void setTargetLocale(LocaleId targetLocaleId) {
    this.targetLocaleId = targetLocaleId;
  }

  @SuppressWarnings("deprecation")
  @StepParameterMapping(parameterType = StepParameterType.SOURCE_LOCALE)
  public void setSourceLocale(LocaleId sourceLocaleId) {
    this.sourceLocaleId = sourceLocaleId;
  }

  public void setXliffFilter(XLIFFFilter xliffFilter) {
    this.xliffFilter = xliffFilter;
  }

  @Override
  protected Event handleStartDocument(Event event) {
    logger.debug("Initialize statistics for the import");

    return super.handleStartDocument(event);
  }

  @Override
  protected Event handleStartSubDocument(Event event) {

    logger.debug("Handle start sub document");
    overallStopWatch.start("Starts processing XLIFF");

    StartSubDocument startSubDocument = event.getStartSubDocument();

    initAsset(startSubDocument);

    if (!isSourceXliff()) {
      initTmTextUnitIdsByMd5ForAsset();
    }

    return super.handleStartSubDocument(event);
  }

  void initAsset(StartSubDocument startSubDocument) {

    if (asset == null) {
      logger.debug("Getting asset path from the file original property");
      String assetPath = startSubDocument.getName();

      if (isSourceXliff()) {
        initAssetForSourceXliff(assetPath);
      } else {
        initAssetForTargetXliff(assetPath);
      }
    } else {
      logger.debug("Asset has been provided, use it");
    }
  }

  void initTmTextUnitIdsByMd5ForAsset() throws RuntimeException {
    logger.debug("Initialize tmTextUnitIds By Md5 forAsset");

    tmTextUnitIdsByMd5ForAsset = getTMTextUnitIdsByMd5ForAsset(asset.getId());

    if (tmTextUnitIdsByMd5ForAsset.isEmpty()) {
      String msg = "No source asset has been populated yet. Please import that first";
      logger.debug(msg);
      // TODO(P1) all the runtime should become typed and propagated to client layer properly
      throw new RuntimeException(msg);
    }
  }

  /**
   * Use the filter to see if there's a target language
   *
   * @return
   */
  private boolean isSourceXliff() {
    return xliffFilter.getCurrentTargetLocale() == null
        || xliffFilter.getCurrentTargetLocale().equals(LocaleId.EMPTY)
        || xliffFilter.getCurrentTargetLocale().equals(sourceLocaleId);
  }

  /**
   * Populate the {@link Asset} object by path for target xliff
   *
   * @param assetPath
   */
  private void initAssetForTargetXliff(String assetPath) {

    logger.debug("Get asset for target xliff: " + assetPath);

    asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());

    if (asset == null) {
      // TODO(P1) all the runtime should become typed and propagated to client layer properly
      String msg =
          "There is no asset found with this path, make sure to first import the source asset first";
      logger.debug(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Populate the {@link Asset} object by path for the source xliff
   *
   * @param assetPath
   */
  private void initAssetForSourceXliff(String assetPath) {
    logger.debug("Get asset for source xliff: " + assetPath);
    asset = assetRepository.findByPathAndRepositoryId(assetPath, repository.getId());
    if (asset == null) {
      logger.debug("Creating asset at: {}", assetPath);
      asset = assetService.createAsset(repository.getId(), assetPath, false);
    } else {
      String msg =
          "Importing of this asset is only supported if an asset with this path did not already exist: "
              + assetPath;
      logger.debug(msg);
      // TODO(P1) all the runtime should become typed and propagated to client layer properly
      throw new RuntimeException(msg);
    }
  }

  @Override
  protected Event handleTextUnit(Event event) {
    ITextUnit textUnit = event.getTextUnit();

    if (isSourceXliff()) {
      handleSourceTextUnit(textUnit);
    } else {
      handleTargetTextUnit(textUnit);
    }

    stopWatchTextUnit();
    return event;
  }

  /**
   * Import target text units into the TM
   *
   * @param textUnit
   */
  protected void handleTargetTextUnit(ITextUnit textUnit) {
    if (!textUnit.isTranslatable()) {
      return;
    }

    Locale targetLocale = localeService.findByBcp47Tag(targetLocaleId.toBCP47());

    String name = textUnit.getName();
    String sourceContent = textUnitUtils.getSourceAsString(textUnit);
    String translation = textUnit.getTarget(targetLocaleId).toString();
    ImportExportNote importExportNote = importExportTextUnitUtils.getImportExportNote(textUnit);

    if (!Strings.isNullOrEmpty(translation)) {

      Long tmTextUnitId =
          tmTextUnitIdsByMd5ForAsset.get(
              textUnitUtils.computeTextUnitMD5(
                  name, sourceContent, importExportNote.getSourceComment()));

      if (tmTextUnitId == null) {
        String msg =
            "Trying to add a translation to an non existing text unit, name: "
                + name
                + ", comment: "
                + importExportNote.getSourceComment()
                + ", source: "
                + sourceContent;
        logger.warn(msg);
      } else {

        TMTextUnitVariant addTMTextUnitVariant = null;

        if (updateTM) {
          logger.debug("Import assuming there is already some translations in the TM");
          addTMTextUnitVariant =
              tmService.addCurrentTMTextUnitVariant(
                  tmTextUnitId,
                  targetLocale.getId(),
                  translation,
                  importExportNote.getStatus(),
                  importExportNote.isIncludedInLocalizedFile(),
                  importExportNote.getCreatedDate());

        } else {
          logger.debug(
              "Import assuming there is no translation in the TM yet (optimized, don't check for update)");
          addTMTextUnitVariant =
              tmService.addTMTextUnitVariant(
                  tmTextUnitId,
                  targetLocale.getId(),
                  translation,
                  importExportNote.getTargetComment(),
                  importExportNote.getStatus(),
                  importExportNote.isIncludedInLocalizedFile(),
                  importExportNote.getCreatedDate());

          tmService.makeTMTextUnitVariantCurrent(
              asset.getRepository().getTm().getId(),
              tmTextUnitId,
              targetLocale.getId(),
              addTMTextUnitVariant.getId(),
              asset.getId());
        }

        for (TMTextUnitVariantComment variantComment : importExportNote.getVariantComments()) {
          tmTextUnitVariantCommentService.addComment(
              addTMTextUnitVariant.getId(),
              variantComment.getType(),
              variantComment.getSeverity(),
              variantComment.getContent());
        }
      }
    } else {
      logger.debug("Empty translation for name: {}, skip it", name);
    }
  }

  /**
   * Import source text units into the TM
   *
   * @param textUnit
   */
  protected void handleSourceTextUnit(ITextUnit textUnit) {
    if (!textUnit.isTranslatable()) {
      return;
    }

    String name = textUnit.getName();
    String sourceContent = textUnitUtils.getSourceAsString(textUnit);
    ImportExportNote importExportNote = importExportTextUnitUtils.getImportExportNote(textUnit);

    PluralForm pluralForm = null;
    String pluralFormOther = null;

    if (importExportNote.getPluralForm() != null) {
      pluralForm = pluralFormService.findByPluralFormString(importExportNote.getPluralForm());
      pluralFormOther = importExportNote.getPluralFormOther();
    }

    tmService.addTMTextUnit(
        asset.getRepository().getTm(),
        asset,
        name,
        sourceContent,
        importExportNote.getSourceComment(),
        importExportNote.getCreatedDate(),
        pluralForm,
        pluralFormOther);
  }

  @Override
  protected Event handleEndDocument(Event event) {
    logger.debug("Done importing this XLIFF");
    overallStopWatch.stop();

    return super.handleEndDocument(event);
  }

  /**
   * Gets the map of MD5s to {@link TMTextUnit#id}s of the {@link TMTextUnit}s related to an {@link
   * Asset}.
   *
   * @param assetId {@link Asset#id}
   * @return the map of MD5s to
   */
  public Map<String, Long> getTMTextUnitIdsByMd5ForAsset(Long assetId) {
    logger.debug("Get TMTextUnit ids by Md5 for AssetId: {}", assetId);
    Map<String, Long> textUnitIdsByMd5 = new HashMap<>();

    for (TextUnitIdMd5DTO textUnit : tmTextUnitRepository.getTextUnitIdMd5DTOByAssetId(assetId)) {
      textUnitIdsByMd5.put(textUnit.getMd5(), textUnit.getId());
    }

    return textUnitIdsByMd5;
  }

  /** Start/stop stop watch in batches for text unit processing */
  protected void stopWatchTextUnit() {
    if (!textUnitStopWatch.isRunning()) {
      textUnitStopWatch.start("start creating text unit");
    }

    if (++numOfTextUnitProcess % 500 == 0) {
      logger.info("num of text unit added:  {}", numOfTextUnitProcess);

      textUnitStopWatch.stop();
      textUnitStopWatch.start("batch " + numOfTextUnitProcess);
    }
  }
}
