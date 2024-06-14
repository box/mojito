package com.box.l10n.mojito.okapi;

import com.box.l10n.mojito.entity.Asset;
import com.box.l10n.mojito.entity.RepositoryLocale;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.NormalizationUtils;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckException;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerFactory;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotation;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TMTextUnitVariantCommentAnnotations;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.TextUnitIntegrityChecker;
import com.box.l10n.mojito.service.tm.TranslatorWithInheritance;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.resource.TextContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * @author jaurambault
 */
@Configurable
public class ImportTranslationsFromLocalizedAssetStep extends AbstractImportTranslationsStep {

  /** Logger */
  static Logger logger = LoggerFactory.getLogger(ImportTranslationsFromLocalizedAssetStep.class);

  @Autowired TextUnitSearcher textUnitSearcher;

  @Autowired IntegrityCheckerFactory integrityCheckerFactory;

  Asset asset;
  RepositoryLocale repositoryLocale;
  StatusForEqualTarget statusForEqualTarget;

  Map<String, TMTextUnit> textUnitsByMd5 = new HashMap<>();
  ArrayListMultimap<String, TMTextUnit> textUnitsByNameUsed = ArrayListMultimap.create();
  ArrayListMultimap<String, TMTextUnit> textUnitsByNameUnused = ArrayListMultimap.create();

  TranslatorWithInheritance translatorWithInheritance;

  private Set<TextUnitIntegrityChecker> textUnitIntegrityCheckers = new HashSet<>();

  boolean hasTranslationWithoutInheritance;

  public enum StatusForEqualTarget {
    SKIPPED,
    REVIEW_NEEDED,
    TRANSLATION_NEEDED,
    APPROVED
  };

  public ImportTranslationsFromLocalizedAssetStep(
      Asset asset, RepositoryLocale repositoryLocale, StatusForEqualTarget statusForEqualTarget) {
    this.asset = asset;
    this.repositoryLocale = repositoryLocale;
    this.statusForEqualTarget = statusForEqualTarget;
  }

  @Override
  protected Event handleStartDocument(Event event) {
    event = super.handleStartDocument(event);

    initTmTextUnitsMapsForAsset();
    translatorWithInheritance =
        new TranslatorWithInheritance(asset, repositoryLocale, InheritanceMode.USE_PARENT);
    hasTranslationWithoutInheritance = translatorWithInheritance.hasTranslationWithoutInheritance();

    textUnitIntegrityCheckers = integrityCheckerFactory.getTextUnitCheckers(asset);
    if (textUnitIntegrityCheckers.isEmpty()) {
      logger.debug("There is no integrity checkers for asset id {}", asset.getId());
    } else {
      logger.debug(
          "Found {} integrity checker(s) for asset id {}",
          textUnitIntegrityCheckers.size(),
          asset.getId());
    }

    return event;
  }

  void initTmTextUnitsMapsForAsset() {
    logger.debug("Init TmTextUnit maps for asset");

    if (isMultilingual) {
      initTextUnitsMapByMd5();
    } else {
      initTextUnitsMapByName();
    }
  }

  void initTextUnitsMapByName() {
    logger.debug("initTextUnitsMapByName");
    Multimap<String, Long> tmTextUnitIdsByNameUsed = ArrayListMultimap.create();
    Multimap<String, Long> tmTextUnitIdsByNameUnused = ArrayListMultimap.create();

    logger.debug(
        "Map text unit names to text unit ids. Used text units have priority (if multiple unused, first one is used");
    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setAssetId(asset.getId());
    textUnitSearcherParameters.setForRootLocale(true);
    textUnitSearcherParameters.setPluralFormsFiltered(false);
    List<TextUnitDTO> textUnitDTOS = textUnitSearcher.search(textUnitSearcherParameters);

    for (TextUnitDTO textUnitDTO : textUnitDTOS) {
      if (textUnitDTO.isUsed()) {
        tmTextUnitIdsByNameUsed.put(textUnitDTO.getName(), textUnitDTO.getTmTextUnitId());
      } else {
        tmTextUnitIdsByNameUnused.put(textUnitDTO.getName(), textUnitDTO.getTmTextUnitId());
      }
    }

    logger.debug("Fetch the used text units and map them by name");
    List<TMTextUnit> textUnits = tmTextUnitRepository.findByIdIn(tmTextUnitIdsByNameUsed.values());

    for (TMTextUnit tmTextUnit : textUnits) {
      textUnitsByNameUsed.put(tmTextUnit.getName(), tmTextUnit);
    }

    logger.debug("Fetch the unused text units and map them by name");
    List<TMTextUnit> textUnitsUnused =
        tmTextUnitRepository.findByIdIn(tmTextUnitIdsByNameUnused.values());

    for (TMTextUnit tmTextUnit : textUnitsUnused) {
      textUnitsByNameUnused.put(tmTextUnit.getName(), tmTextUnit);
    }
  }

  @Override
  protected TMTextUnitVariant importTextUnit(
      TMTextUnit tmTextUnit,
      TextContainer target,
      TMTextUnitVariant.Status status,
      ZonedDateTime createdDate) {

    for (TextUnitIntegrityChecker textUnitIntegrityChecker : textUnitIntegrityCheckers) {
      try {
        textUnitIntegrityChecker.check(tmTextUnit.getContent(), target.toString());
      } catch (IntegrityCheckException integrityCheckException) {
        TMTextUnitVariantCommentAnnotation tmTextUnitVariantCommentAnnotation =
            new TMTextUnitVariantCommentAnnotation();
        tmTextUnitVariantCommentAnnotation.setCommentType(
            TMTextUnitVariantComment.Type.INTEGRITY_CHECK);

        tmTextUnitVariantCommentAnnotation.setMessage(integrityCheckException.getMessage());

        tmTextUnitVariantCommentAnnotation.setSeverity(
            TMTextUnitVariantComment.Severity.ERROR); // TODO(ja) dial it down for plural strings?

        new TMTextUnitVariantCommentAnnotations(target)
            .addAnnotation(tmTextUnitVariantCommentAnnotation);
      }
    }

    return super.importTextUnit(tmTextUnit, target, status, createdDate);
  }

  void initTextUnitsMapByMd5() {
    logger.debug("initTextUnitsMapByMd5");
    List<TMTextUnit> textUnits = tmTextUnitRepository.findByAsset(asset);
    for (TMTextUnit tmTextUnit : textUnits) {
      textUnitsByMd5.put(tmTextUnit.getMd5(), tmTextUnit);
    }
  }

  @Override
  public String getName() {
    return "Import translations from a localized asset (multilingual or not)";
  }

  @Override
  public String getDescription() {
    return "Updates the TM with the extracted new/changed variants."
        + " Expects: raw document. Sends back: original events.";
  }

  @Override
  TMTextUnit getTMTextUnit() {

    TMTextUnit tmTextUnit = null;

    if (isMultilingual) {
      tmTextUnit = textUnitsByMd5.remove(md5);
    } else {
      List<TMTextUnit> tmTextUnits = textUnitsByNameUsed.get(name);
      if (!tmTextUnits.isEmpty()) {
        tmTextUnit = tmTextUnits.remove(0);
      }
    }

    return tmTextUnit;
  }

  @Override
  protected TMTextUnitVariant.Status getStatusForImport(
      TMTextUnit tmTextUnit, TextContainer target) {

    TMTextUnitVariant.Status status;

    TextUnitDTO currentTranslation = translatorWithInheritance.getTextUnitDTO(tmTextUnit.getMd5());

    boolean hasSameTarget;
    String targetAsString = NormalizationUtils.normalize(target.toString());

    logger.debug(
        "Check if new target: [{}] is different to either the current, parent or source string: [{}]",
        targetAsString,
        currentTranslation);

    if (currentTranslation != null) {
      hasSameTarget = targetAsString.equals(currentTranslation.getTarget());
    } else {
      logger.debug("No current or parent, compare with the source");
      hasSameTarget = targetAsString.equals(tmTextUnit.getContent());
    }

    if (hasSameTarget) {
      logger.debug("Target is the same");
      if (repositoryLocale.isToBeFullyTranslated()) {
        boolean isForTargetLocale =
            currentTranslation != null
                && currentTranslation.getLocaleId().equals(repositoryLocale.getLocale().getId());
        status = getStatusForSameTargetAndFullyTranslated(isForTargetLocale, currentTranslation);
      } else {
        logger.debug("Locale is not fully translated, skip target as it is the same");
        status = null;
      }
    } else {
      logger.debug("Target is different, import as approved");
      status = TMTextUnitVariant.Status.APPROVED;
    }

    return status;
  }

  TMTextUnitVariant.Status getStatusForSameTargetAndFullyTranslated(
      boolean isForTargetLocale, TextUnitDTO currentTranslation) {
    logger.debug("Get status when target is same for a fully translated locale");

    TMTextUnitVariant.Status status;

    if (StatusForEqualTarget.TRANSLATION_NEEDED.equals(statusForEqualTarget)) {
      status = TMTextUnitVariant.Status.TRANSLATION_NEEDED;
    } else if (StatusForEqualTarget.REVIEW_NEEDED.equals(statusForEqualTarget)) {
      status = TMTextUnitVariant.Status.REVIEW_NEEDED;
    } else if (StatusForEqualTarget.SKIPPED.equals(statusForEqualTarget)) {
      status = null;
    } else {
      status = TMTextUnitVariant.Status.APPROVED;
    }

    if (isForTargetLocale && status != null && status.equals(currentTranslation.getStatus())) {
      logger.debug("Same target for target locale and same status, skip");
      status = null;
    }

    return status;
  }

  /**
   * Optimize by skipping the look up of the tmTextUnitCurrentVariant when then there is no
   * translation.
   *
   * <p>This is to optimize for the first import of big projects, looking up for
   * tmTextUnitCurrentVariants is expensive and not required as none is present yet.
   *
   * @param localeId
   * @param tmTextUnit
   * @return
   */
  @Override
  TMTextUnitCurrentVariant getTMTextUnitCurrentVariant(Long localeId, TMTextUnit tmTextUnit) {

    TMTextUnitCurrentVariant tmTextUnitCurrentVariant = null;

    if (hasTranslationWithoutInheritance) {
      return super.getTMTextUnitCurrentVariant(localeId, tmTextUnit);
    }

    return tmTextUnitCurrentVariant;
  }
}
