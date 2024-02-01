package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.service.translationkit.TranslationKitExportedImportedAndCurrentTUV;
import com.box.l10n.mojito.service.translationkit.TranslationKitRepository;
import com.box.l10n.mojito.service.translationkit.TranslationKitService;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Objects;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.StartSubDocument;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jaurambault
 */
@Configurable
public class ImportTranslationsWithTranslationKitStep extends ImportTranslationsByIdStep {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(ImportTranslationsWithTranslationKitStep.class);

  @Autowired TranslationKitService translationKitService;

  @Autowired TranslationKitRepository translationKitRepository;

  TranslationKit translationKit;

  Map<Long, TranslationKitExportedImportedAndCurrentTUV> translationKitExportedAndCurrentTUVs;

  @Override
  public String getName() {
    return "Import translations";
  }

  @Override
  public String getDescription() {
    return "Updates the TM with the extracted new/changed variants."
        + " Expects: raw document. Sends back: original events.";
  }

  @Override
  protected Event handleStartSubDocument(Event event) {
    super.handleStartSubDocument(event);
    extractTranslationKit(event);
    return event;
  }

  /**
   * Extract the {@link TranslationKit} linked to the content being imported.
   *
   * <p>* @param event
   *
   * @throws ImportTranslationsStepException
   */
  void extractTranslationKit(Event event) throws ImportTranslationsStepException {
    logger.debug("Try to get a TranslationKit matching this import");
    StartSubDocument startSubDocument = event.getStartSubDocument();

    try {
      Long translationKitId = Long.valueOf(startSubDocument.getName());
      translationKit = translationKitRepository.findById(translationKitId).orElse(null);

      if (translationKit == null) {
        logger.debug("Cannot find TranslationKit entity for id: {}", translationKitId);
      }
    } catch (NumberFormatException nfe) {
      logger.debug(
          "Cannot convert sub document name: "
              + startSubDocument.getName()
              + " into Long (TranslationKit id)",
          nfe);
    }

    if (translationKit == null) {
      String msg =
          "No TranslationKit can be found for id: "
              + startSubDocument.getName()
              + " to perform this import";
      logger.debug(msg);
      throw new ImportTranslationsStepException(msg);
    }

    translationKitExportedAndCurrentTUVs =
        translationKitService.getTranslationKitExportedAndCurrentTUVs(translationKit.getId());
  }

  @Override
  TMTextUnit getTMTextUnit() {

    TMTextUnit tmTextUnit = null;

    try {
      Long tmTextUnitId = Long.valueOf(textUnit.getId());
      tmTextUnit = tmTextUnitRepository.findById(tmTextUnitId).orElse(null);
    } catch (NumberFormatException nfe) {
      logger.debug("Could not convert the textUnit id into a Long (TextUnit id)", nfe);
    }

    return tmTextUnit;
  }

  /**
   * Adds the translations as current translation if there wasn't any other translation added
   * between the time the translation kit was exported and the import time. Else just save the
   * translation as a variant, that can be later access looking at history.
   */
  @Override
  @Transactional
  TMTextUnitVariant importTextUnit(
      TMTextUnit tmTextUnit,
      TextContainer target,
      TMTextUnitVariant.Status status,
      ZonedDateTime createdDate) {
    TMTextUnitVariant importTextUnit =
        super.importTextUnit(tmTextUnit, target, status, createdDate);
    translationKitService.markTranslationKitTextUnitAsImported(translationKit, importTextUnit);
    return importTextUnit;
  }

  /**
   * The text unit should be imported if the current translation is the same as the previously
   * imported translation in this TK (a re-import is being performed) or if no translation has been
   * added since the TK was exported (this includes not having translation for this text unit).
   *
   * <p>Note that deleting or changing the status of translation will prevent a reimport.
   *
   * @param tmTextUnitId
   * @return
   */
  @Override
  boolean shouldImportAsCurrentTranslation(Long tmTextUnitId) {

    TranslationKitExportedImportedAndCurrentTUV translationKitExportedAndCurrentTUV =
        translationKitExportedAndCurrentTUVs.get(tmTextUnitId);

    boolean isTKReimport =
        Objects.equals(
            translationKitExportedAndCurrentTUV.getCurrentTmTextUnitVariant(),
            translationKitExportedAndCurrentTUV.getImportedTmTextUnitVariant());
    boolean noExternalTranslationAdded =
        Objects.equals(
            translationKitExportedAndCurrentTUV.getCurrentTmTextUnitVariant(),
            translationKitExportedAndCurrentTUV.getExportedTmTextUnitVariant());

    return isTKReimport || noExternalTranslationAdded;
  }

  @Override
  protected Event handleEndDocument(Event event) {
    super.handleEndDocument(event);
    translationKitService.updateStatistics(translationKit.getId(), notFoundTextUnitIds);
    return event;
  }
}
