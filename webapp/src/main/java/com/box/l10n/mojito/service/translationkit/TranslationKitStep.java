package com.box.l10n.mojito.service.translationkit;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TranslationKit;
import com.box.l10n.mojito.entity.TranslationKitTextUnit;
import com.box.l10n.mojito.okapi.TextUnitDTOAnnotations;
import com.box.l10n.mojito.service.tm.TMTextUnitRepository;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantRepository;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import java.util.ArrayList;
import java.util.List;
import net.sf.okapi.common.Event;
import net.sf.okapi.common.pipeline.BasePipelineStep;
import net.sf.okapi.common.resource.ITextUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * This steps persist information related to {@link TranslationKit}.
 *
 * @author jaurambault
 */
@Configurable
public class TranslationKitStep extends BasePipelineStep {

  static Logger logger = LoggerFactory.getLogger(TranslationKitStep.class);

  @Autowired TranslationKitService translationKitService;

  @Autowired TMTextUnitRepository tmTextUnitRepository;

  @Autowired TMTextUnitVariantRepository tmTextUnitVariantRepository;

  @Autowired TranslationKitRepository translationKitRepository;

  @Autowired TextUnitDTOAnnotations textUnitDTOAnnotations;

  /** The {@link TranslationKit#id} */
  Long translationKitId;

  Long wordCount = 0L;

  /** Keeps track of {@link TranslationKitTextUnit}s to be included in the {@link TranslationKit} */
  List<TranslationKitTextUnit> translationKitTextUnits;

  public TranslationKitStep(Long translationKitId) {
    this.translationKitId = translationKitId;
  }

  @Override
  public String getName() {
    return "Translation Kit Step";
  }

  @Override
  public String getDescription() {
    return "Persist information related the creation of Translation Kits";
  }

  @Override
  protected Event handleStartDocument(Event event) {
    translationKitTextUnits = new ArrayList<>();
    return event;
  }

  @Override
  protected Event handleTextUnit(Event event) {
    ITextUnit textUnit = event.getTextUnit();

    TranslationKitTextUnit translationKitTextUnit = new TranslationKitTextUnit();

    translationKitTextUnit.setTranslationKit(translationKitRepository.getOne(translationKitId));

    Long textUnitId = Long.valueOf(textUnit.getId());
    TMTextUnit tmTextUnit = tmTextUnitRepository.getOne(textUnitId);
    translationKitTextUnit.setTmTextUnit(tmTextUnit);
    wordCount += tmTextUnit.getWordCount();

    translationKitTextUnit.setExportedTmTextUnitVariant(getTMTextUnitVariant(textUnit));

    translationKitTextUnits.add(translationKitTextUnit);

    textUnit.setPreserveWhitespaces(true);

    return event;
  }

  @Override
  protected Event handleEndDocument(Event event) {
    translationKitService.updateTranslationKitWithTmTextUnits(
        translationKitId, translationKitTextUnits, wordCount);
    return event;
  }

  /**
   * Gets the {@link TMTextUnitVariant} linked to a {@link ITextUnit}.
   *
   * @param textUnit the text unit to get the {@link TMTextUnitVariant} from
   * @return the {@link TMTextUnitVariant} or {@code null} if not available
   */
  TMTextUnitVariant getTMTextUnitVariant(ITextUnit textUnit) {

    TMTextUnitVariant tmTextUnitVariant = null;

    TextUnitDTO textUnitDTO = textUnitDTOAnnotations.getTextUnitDTO(textUnit);

    if (textUnitDTO != null && textUnitDTO.getTmTextUnitVariantId() != null) {
      tmTextUnitVariant = tmTextUnitVariantRepository.getOne(textUnitDTO.getTmTextUnitVariantId());
    }

    return tmTextUnitVariant;
  }
}
